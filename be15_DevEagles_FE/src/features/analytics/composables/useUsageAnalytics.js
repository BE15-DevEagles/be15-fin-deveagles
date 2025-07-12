import { ref, reactive, computed } from 'vue';
import reservationAnalyticsAPI from '../api/reservationAnalytics.js';
import {
  createHourlyUsageChartOption,
  createDailyVisitorChartOption,
  createStaffReservationChartOption,
  createHeatmapChartOption,
} from '../config/usageChartOptions.js';
import { formatPercentage } from '../utils/formatters.js';
import { useMetadataStore } from '@/store/metadata.js';
import { getComposableLogger } from '@/plugins/LoggerManager.js';
import { useAnalyticsStore } from '@/store/analytics.js';

export function useUsageAnalytics() {
  const loading = ref(false);
  const error = ref(null);
  const metadataStore = useMetadataStore();
  const logger = getComposableLogger('UsageAnalytics');
  const analyticsStore = useAnalyticsStore();

  // 예약율 데이터
  const usageData = reactive({
    overallUtilization: 0,
    utilizationGrowth: 0,
    averageUsageTime: 0,
    staffUtilization: 0,
    peakHourUtilization: 0,
    hourlyUsage: [],
    dailyVisitorData: [],
    staffUsage: [],
    heatmapData: [],
    peakHours: [],
    heatmapHours: [],
    heatmapMinHour: 9,
    heatmapMaxHour: 20,
  });

  // 필터 옵션
  const filters = reactive({
    dateRange: {
      start: (() => {
        const date = new Date();
        date.setDate(date.getDate() - 30);
        return date.toISOString().split('T')[0];
      })(),
      end: new Date().toISOString().split('T')[0],
    },
  });

  // 날짜 범위 계산
  const getDateRange = () => {
    const now = new Date();
    let startDate, endDate;

    return {
      startDate:
        filters.dateRange.start ||
        new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      endDate: filters.dateRange.end || new Date().toISOString().split('T')[0],
    };
  };

  // 데이터 로드
  const loadUsageData = async (forceRefresh = false) => {
    if (loading.value && !forceRefresh) return;

    // 캐시 확인
    const currentFilters = {
      startDate: filters.dateRange.start,
      endDate: filters.dateRange.end,
    };

    if (!forceRefresh && analyticsStore.isUsageCacheValid(currentFilters)) {
      const cachedData = analyticsStore.getCachedUsageData;
      if (cachedData) {
        logger.info('캐시된 이용률 데이터 사용', {
          cacheAge: Date.now() - analyticsStore.usageCache.timestamp,
          filters: currentFilters,
        });

        // 캐시된 데이터로 usageData 업데이트
        Object.assign(usageData, cachedData);
        loading.value = false;
        error.value = null;
        return;
      }
    }

    loading.value = true;
    error.value = null;
    analyticsStore.setUsageLoading(true);

    try {
      // 메타데이터 먼저 로드 (직원 이름 매핑용)
      await metadataStore.loadMetadata();

      const dateRange = getDateRange();
      const params = {
        startDate: dateRange.startDate,
        endDate: dateRange.endDate,
        timeRange: 'CUSTOM',
      };

      logger.info('예약율 데이터 로딩 시작', params);

      // 병렬로 여러 API 호출
      const [summaryData, hourlyVisitorData, dailyVisitorData, staffData, heatmapData] =
        await Promise.all([
          reservationAnalyticsAPI.getReservationSummary(params),
          reservationAnalyticsAPI.getHourlyVisitorStatistics(params),
          reservationAnalyticsAPI.getDailyVisitorStatistics(params),
          reservationAnalyticsAPI.getAllStaffReservationStatistics(params),
          reservationAnalyticsAPI.getDayTimeSlotReservationData(params),
        ]);

      usageData.overallUtilization = Number(summaryData?.averageReservationRate || 0);
      usageData.utilizationGrowth = 0;
      usageData.averageUsageTime = Number(summaryData?.averageUsageTime || 30);
      usageData.staffUtilization = Number(summaryData?.averageReservationRate || 0);
      usageData.peakHourUtilization = Number(summaryData?.peakHourReservationRate || 0);

      // 데이터 변환 (빈 배열 처리)
      usageData.hourlyUsage = Array.isArray(hourlyVisitorData)
        ? transformHourlyVisitorData(hourlyVisitorData)
        : [];
      usageData.dailyVisitorData = Array.isArray(dailyVisitorData)
        ? transformDailyVisitorData(dailyVisitorData)
        : [];
      usageData.staffUsage = Array.isArray(staffData) ? transformStaffData(staffData) : [];

      // 히트맵 데이터 변환
      let actualHeatmapData;
      if (Array.isArray(heatmapData)) {
        actualHeatmapData = heatmapData;
      } else if (heatmapData && Array.isArray(heatmapData.data)) {
        actualHeatmapData = heatmapData.data;
      } else {
        logger.warn('히트맵 데이터가 배열이 아님', { type: typeof heatmapData });
        actualHeatmapData = [];
      }

      if (Array.isArray(actualHeatmapData) && actualHeatmapData.length > 0) {
        const heatmapResult = transformHeatmapData(actualHeatmapData);
        usageData.heatmapData = heatmapResult.data;
        usageData.heatmapHours = heatmapResult.hours;
        usageData.heatmapMinHour = heatmapResult.minHour;
        usageData.heatmapMaxHour = heatmapResult.maxHour;
        logger.info('히트맵 데이터 변환 완료', {
          dataCount: usageData.heatmapData.length,
          timeRange: `${heatmapResult.minHour}시~${heatmapResult.maxHour}시`,
        });
      } else {
        logger.warn('유효한 히트맵 데이터가 없음');
        usageData.heatmapData = [];
        usageData.heatmapHours = [];
        usageData.heatmapMinHour = 9;
        usageData.heatmapMaxHour = 20;
      }

      // 기타 데이터
      usageData.peakHours = Array.isArray(hourlyVisitorData)
        ? extractPeakHours(hourlyVisitorData)
        : [];

      // 캐시에 저장
      const dataToCache = JSON.parse(JSON.stringify(usageData));
      analyticsStore.setUsageCache(dataToCache, currentFilters);

      logger.info('예약율 데이터 로드 완료', {
        overallUtilization: usageData.overallUtilization,
        peakHourUtilization: usageData.peakHourUtilization,
        heatmapDataCount: usageData.heatmapData.length,
      });
    } catch (err) {
      const errorMessage = `데이터 로딩 실패: ${err.message || '알 수 없는 오류'}`;
      error.value = errorMessage;
      analyticsStore.setUsageError(errorMessage);
      logger.error('예약율 데이터 로드 실패', err);

      // 실패 시 기본값 설정
      Object.assign(usageData, {
        overallUtilization: 0,
        utilizationGrowth: 0,
        averageUsageTime: 30,
        staffUtilization: 0,
        peakHourUtilization: 0,
        hourlyUsage: [],
        dailyVisitorData: [],
        staffUsage: [],
        heatmapData: [],
        heatmapHours: [],
        heatmapMinHour: 9,
        heatmapMaxHour: 20,
        peakHours: [],
      });
    } finally {
      loading.value = false;
      analyticsStore.setUsageLoading(false);
    }
  };

  // 시간대별 방문객 데이터 변환
  const transformHourlyVisitorData = data => {
    return data.map(item => ({
      time: item.displayTime || item.timeSlot || `${item.hour}시`,
      hour: item.hour || 0,
      male: Number(item.maleVisitors || 0),
      female: Number(item.femaleVisitors || 0),
      total: Number(item.totalVisitors || 0),
      maleVisitors: Number(item.maleVisitors || 0),
      femaleVisitors: Number(item.femaleVisitors || 0),
      totalVisitors: Number(item.totalVisitors || 0),
    }));
  };

  // 일별 방문객 데이터 변환
  const transformDailyVisitorData = data => {
    return data.map(item => ({
      date: item.date,
      dayOfWeek: item.dayOfWeek || '',
      displayDate: item.displayDate || '',
      label: item.label || `${item.displayDate} (${item.dayOfWeek})`,
      male: Number(item.maleVisitors || 0),
      female: Number(item.femaleVisitors || 0),
      total: Number(item.totalVisitors || 0),
      maleVisitors: Number(item.maleVisitors || 0),
      femaleVisitors: Number(item.femaleVisitors || 0),
      totalVisitors: Number(item.totalVisitors || 0),
    }));
  };

  // 직원별 데이터 변환
  const transformStaffData = data => {
    return data.map(item => {
      // metadata store에서 직원 이름 찾기
      const staffInfo = metadataStore.staff.find(s => s.id === item.staffId);
      const staffName = staffInfo?.name || item.staffName || `직원${item.staffId}`;

      return {
        staff: staffName,
        reservationCount: Number(item.reservedSlots || 0),
        totalSales: Number(item.totalSales || 0),
        staffId: item.staffId,
      };
    });
  };

  // 히트맵 데이터 변환 - 요일별/시간대별 실제 데이터 사용
  const transformHeatmapData = data => {
    if (!Array.isArray(data) || data.length === 0) {
      logger.warn('히트맵 데이터가 없음', { type: typeof data, length: data?.length });
      return { data: [], hours: [], minHour: 9, maxHour: 20 };
    }

    // 요일별 순서 매핑 (MON=0, TUE=1, ..., SUN=6)
    const dayMap = {
      MON: 0,
      MONDAY: 0,
      월: 0,
      월요일: 0,
      TUE: 1,
      TUESDAY: 1,
      화: 1,
      화요일: 1,
      WED: 2,
      WEDNESDAY: 2,
      수: 2,
      수요일: 2,
      THU: 3,
      THURSDAY: 3,
      목: 3,
      목요일: 3,
      FRI: 4,
      FRIDAY: 4,
      금: 4,
      금요일: 4,
      SAT: 5,
      SATURDAY: 5,
      토: 5,
      토요일: 5,
      SUN: 6,
      SUNDAY: 6,
      일: 6,
      일요일: 6,
    };

    // 시간대 추출 및 범위 계산
    const hours = new Set();
    const validData = [];

    data.forEach(item => {
      const timeSlot = item.timeSlot;
      let dayOfWeek = item.dayOfWeek;

      // dayOfWeek가 없으면 displayKey에서 추출
      if (!dayOfWeek && item.displayKey) {
        dayOfWeek = item.displayKey.split('_')[0];
      }

      const reservationRate = Number(item.reservationRate || 0);

      if (timeSlot && dayOfWeek && dayOfWeek in dayMap) {
        // 시간 파싱 - HH:MM:SS 형식에서 시간 추출
        let hour;
        if (timeSlot.includes(':')) {
          hour = parseInt(timeSlot.split(':')[0]);
        } else {
          hour = parseInt(timeSlot);
        }

        // 유효한 시간인지 확인
        if (!isNaN(hour) && hour >= 0 && hour <= 23) {
          hours.add(hour);
          validData.push({
            hour,
            dayOfWeek,
            reservationRate,
            dayIndex: dayMap[dayOfWeek],
          });
        }
      }
    });

    // 시간 범위 계산
    const hourArray = Array.from(hours).sort((a, b) => a - b);
    const minHour = hourArray.length > 0 ? hourArray[0] : 9;
    const maxHour = hourArray.length > 0 ? hourArray[hourArray.length - 1] : 20;

    // 시간 라벨 생성
    const hourLabels = [];
    for (let hour = minHour; hour <= maxHour; hour++) {
      hourLabels.push(`${hour}시`);
    }

    // 데이터 매핑용 맵 생성
    const dataMap = new Map();

    // 요일별, 시간대별 데이터 매핑
    validData.forEach(item => {
      const hourIndex = item.hour - minHour; // 0부터 시작하는 인덱스
      const dayIndex = item.dayIndex;
      const rate = Math.round(item.reservationRate * 10) / 10;

      const key = `${hourIndex}-${dayIndex}`;
      dataMap.set(key, rate);
    });

    // 히트맵 데이터 생성 (모든 시간대/요일 조합)
    const heatmapData = [];
    for (let dayIndex = 0; dayIndex < 7; dayIndex++) {
      for (let hour = minHour; hour <= maxHour; hour++) {
        const hourIndex = hour - minHour;
        const key = `${hourIndex}-${dayIndex}`;
        const rate = dataMap.get(key) || 0;

        heatmapData.push([hourIndex, dayIndex, rate]);
      }
    }

    // 시간순, 요일순으로 정렬
    heatmapData.sort((a, b) => {
      if (a[0] !== b[0]) return a[0] - b[0]; // 시간순
      return a[1] - b[1]; // 요일순
    });

    return {
      data: heatmapData,
      hours: hourLabels,
      minHour,
      maxHour,
    };
  };

  // 피크 시간 추출 - 실제 방문객 수 기반
  const extractPeakHours = hourlyVisitorData => {
    if (!Array.isArray(hourlyVisitorData) || hourlyVisitorData.length === 0) return [];

    return hourlyVisitorData
      .sort((a, b) => (b.totalVisitors || 0) - (a.totalVisitors || 0))
      .slice(0, 3)
      .map(item => ({
        time: item.displayTime || item.timeSlot || `${item.hour}시`,
        visitors: Number(item.totalVisitors || 0),
        male: Number(item.maleVisitors || 0),
        female: Number(item.femaleVisitors || 0),
      }));
  };

  // 차트 옵션 생성 (computed로 반응형 데이터와 연결)
  const getHourlyUsageChartOption = computed(() => {
    return (isDarkMode = false) => createHourlyUsageChartOption(usageData.hourlyUsage, isDarkMode);
  });

  const getDailyVisitorChartOption = computed(() => {
    return (isDarkMode = false) =>
      createDailyVisitorChartOption(usageData.dailyVisitorData, isDarkMode);
  });

  const getStaffReservationChartOption = computed(() => {
    return (isDarkMode = false) =>
      createStaffReservationChartOption(usageData.staffUsage, isDarkMode);
  });

  const getHeatmapChartOption = computed(() => {
    return (isDarkMode = false) =>
      createHeatmapChartOption(usageData.heatmapData, usageData.heatmapHours, isDarkMode);
  });

  // 필터 업데이트
  const updateFilters = newFilters => {
    Object.assign(filters, newFilters);
    loadUsageData();
  };

  return {
    // 상태
    loading,
    error,
    usageData,
    filters,

    // 메서드
    loadUsageData,
    updateFilters,

    // 차트 옵션
    getHourlyUsageChartOption,
    getDailyVisitorChartOption,
    getStaffReservationChartOption,
    getHeatmapChartOption,

    // 유틸리티
    formatPercentage,
  };
}
