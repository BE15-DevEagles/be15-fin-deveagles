import { ref, reactive, computed } from 'vue';
import {
  createDailySalesChartOption,
  createAdvancedSalesChartOption,
  createDiscountAnalysisChartOption,
  createSummaryCardsData,
} from '../config/salesChartOptions.js';
import {
  createPrimaryItemShareChartOption,
  createSecondaryItemShareChartOption,
  createPrimaryItemTransactionShareChartOption,
  createSecondaryItemTransactionShareChartOption,
} from '../config/itemChartOptions.js';
import { createPrimaryItemDailyTrendChartOption } from '../config/trendChartOptions.js';
import { formatCurrency, formatPercentage } from '../utils/formatters.js';
import salesAnalyticsAPI from '../api/salesAnalytics.js';
import { useAnalyticsCore } from './useAnalyticsCore.js';
import { getComposableLogger } from '@/plugins/LoggerManager.js';
import { useAnalyticsStore } from '@/store/analytics.js';

export function useSalesAnalytics() {
  const logger = getComposableLogger('SalesAnalytics');
  const analyticsStore = useAnalyticsStore();

  // 공통 기능 사용
  const {
    loading,
    error,
    safeApiCall,
    safeParallelApiCalls,
    getDateString,
    calculateDaysDifference,
    updateFilters: updateFiltersCore,
    clearError,
  } = useAnalyticsCore();

  // 개별 데이터 로딩 상태
  const itemDataLoading = ref(false);

  // 비즈니스 대시보드 데이터 구조
  const dashboardData = reactive({
    // 매출 요약 (SalesSummaryResponse)
    summary: {
      totalSales: 0,
      dailyAverage: 0,
      totalTransactions: 0,
      averageOrderValue: 0,
      startDate: null,
      endDate: null,
    },

    // 기본 매출 통계 (SalesStatisticsResponse)
    dailySales: [],

    // 고급 매출 통계 (AdvancedSalesStatisticsResponse)
    categoryData: [],
    genderData: [],
    trendData: [],
    discountData: [],
    primaryItemData: [],
    secondaryItemData: [],
    primaryItemDailyTrendData: [],

    // 지표 카드 데이터
    summaryCards: [],
  });

  // 필터 상태 - 백엔드 StatisticsRequest 스펙에 맞춤
  const filters = reactive({
    // 기본 필터
    startDate: getDateString(-30), // 30일 전
    endDate: getDateString(0), // 오늘
    timeRange: 'CUSTOM',

    // 고급 필터
    groupBy: 'WEEK',
    gender: null,
    categoryId: null,
    primaryItemId: null,
    secondaryItemId: null,

    // UI 필터
    analysisType: 'overview', // overview, category, gender, item, discount
    chartType: 'bar', // bar, line, pie
  });

  // 데이터 로드 메인 함수
  const loadSalesData = async (forceRefresh = false) => {
    if (loading.value && !forceRefresh) return;

    // 캐시 확인
    const currentFilters = {
      startDate: filters.startDate,
      endDate: filters.endDate,
      timeRange: filters.timeRange,
      groupBy: filters.groupBy,
      gender: filters.gender,
      categoryId: filters.categoryId,
      primaryItemId: filters.primaryItemId,
      secondaryItemId: filters.secondaryItemId,
    };

    if (!forceRefresh && analyticsStore.isSalesCacheValid(currentFilters)) {
      const cachedData = analyticsStore.getCachedSalesData;
      if (cachedData) {
        logger.info('캐시된 매출 데이터 사용', {
          cacheAge: Date.now() - analyticsStore.salesCache.timestamp,
          filters: currentFilters,
        });

        // 캐시된 데이터로 dashboardData 업데이트
        Object.assign(dashboardData, cachedData);
        loading.value = false;
        error.value = null;
        return;
      }
    }

    loading.value = true;
    error.value = null;
    analyticsStore.setSalesLoading(true);

    try {
      logger.info('매출 데이터 로드 시작', { filters: currentFilters });

      // 병렬로 기본 데이터 로드
      await Promise.all([
        loadSummaryData(),
        loadDailySalesData(),
        loadCategoryData(),
        loadGenderData(),
        loadTrendData(),
        loadDiscountData(),
        loadPrimaryItemData(),
        loadSecondaryItemData(),
        loadPrimaryItemDailyTrendData(),
      ]);

      // 지표 카드 데이터 생성
      dashboardData.summaryCards = createSummaryCardsData(dashboardData.summary);

      // 캐시에 저장
      const dataToCache = JSON.parse(JSON.stringify(dashboardData));
      analyticsStore.setSalesCache(dataToCache, currentFilters);

      logger.info('매출 데이터 로드 완료', {
        summary: dashboardData.summary,
        dailySalesCount: dashboardData.dailySales.length,
        categoryCount: dashboardData.categoryData.length,
        genderCount: dashboardData.genderData.length,
        trendCount: dashboardData.trendData.length,
        discountCount: dashboardData.discountData.length,
        primaryItemCount: dashboardData.primaryItemData.length,
        secondaryItemCount: dashboardData.secondaryItemData.length,
      });
    } catch (err) {
      const errorMessage = err.message || '매출 데이터를 불러오는 중 오류가 발생했습니다.';
      error.value = errorMessage;
      analyticsStore.setSalesError(errorMessage);
      logger.error('매출 데이터 로드 실패', err);

      // 에러 발생 시 빈 데이터로 초기화
      resetDashboardData();
    } finally {
      loading.value = false;
      analyticsStore.setSalesLoading(false);
    }
  };

  // 매출 요약 데이터 로드
  const loadSummaryData = async () => {
    try {
      const params = {
        startDate: filters.startDate,
        endDate: filters.endDate,
      };

      const summaryData = await salesAnalyticsAPI.getSalesSummary(params);
      Object.assign(dashboardData.summary, summaryData);
    } catch (err) {
      logger.warn('매출 요약 데이터 로드 실패', err);
    }
  };

  // 일별 매출 데이터 로드
  const loadDailySalesData = async () => {
    try {
      const params = {
        startDate: filters.startDate,
        endDate: filters.endDate,
      };

      const dailyData = await salesAnalyticsAPI.getSalesStatistics(params);
      dashboardData.dailySales = dailyData;
    } catch (err) {
      logger.warn('일별 매출 데이터 로드 실패', err);
      dashboardData.dailySales = [];
    }
  };

  // 카테고리별 매출 데이터 로드
  const loadCategoryData = async () => {
    try {
      const params = {
        startDate: filters.startDate,
        endDate: filters.endDate,
        timeRange: filters.timeRange,
      };

      const categoryData = await salesAnalyticsAPI.getCategorySalesData(params);
      dashboardData.categoryData = categoryData;
    } catch (err) {
      logger.warn('카테고리별 매출 데이터 로드 실패', err);
      dashboardData.categoryData = [];
    }
  };

  // 성별 매출 데이터 로드
  const loadGenderData = async () => {
    try {
      const params = {
        startDate: filters.startDate,
        endDate: filters.endDate,
        timeRange: filters.timeRange,
      };

      const genderData = await salesAnalyticsAPI.getGenderSalesData(params);
      dashboardData.genderData = genderData;
    } catch (err) {
      logger.warn('성별 매출 데이터 로드 실패', err);
      dashboardData.genderData = [];
    }
  };

  // 매출 추이 데이터 로드 (선택한 기간에 따라 동적 그룹화)
  const loadTrendData = async () => {
    try {
      const daysDiff = calculateDaysDifference(filters.startDate, filters.endDate);
      const groupBy = getOptimalGroupBy(daysDiff);

      const requestBody = {
        startDate: filters.startDate,
        endDate: filters.endDate,
        groupBy,
        timeRange: 'CUSTOM',
      };

      const trendData = await salesAnalyticsAPI.getAdvancedSalesStatistics({}, requestBody);
      dashboardData.trendData = trendData;
    } catch (err) {
      logger.warn('매출 추이 데이터 로드 실패', err);
      dashboardData.trendData = [];
    }
  };

  // 할인 분석 데이터 로드 (선택한 기간에 따라 동적 그룹화)
  const loadDiscountData = async () => {
    try {
      const daysDiff = calculateDaysDifference(filters.startDate, filters.endDate);
      const groupBy = getOptimalGroupByForDiscount(daysDiff);

      const requestBody = {
        startDate: filters.startDate,
        endDate: filters.endDate,
        groupBy,
        timeRange: filters.timeRange,
      };

      const discountData = await salesAnalyticsAPI.getAdvancedSalesStatistics({}, requestBody);
      dashboardData.discountData = discountData;
    } catch (err) {
      logger.warn('할인 분석 데이터 로드 실패', err);
      dashboardData.discountData = [];
    }
  };

  // 1차 아이템 데이터 로드
  const loadPrimaryItemData = async () => {
    try {
      itemDataLoading.value = true;
      const params = {
        startDate: filters.startDate,
        endDate: filters.endDate,
        timeRange: filters.timeRange,
      };

      const primaryItemData = await salesAnalyticsAPI.getItemSalesData(params, 'PRIMARY_ITEM');
      logger.info('1차 아이템 데이터 로드 완료', { count: primaryItemData?.length || 0 });

      dashboardData.primaryItemData = primaryItemData;
    } catch (err) {
      logger.error('1차 아이템 데이터 로드 실패', err);
      dashboardData.primaryItemData = [];
    } finally {
      itemDataLoading.value = false;
    }
  };

  // 2차 아이템 데이터 로드
  const loadSecondaryItemData = async () => {
    try {
      const params = {
        startDate: filters.startDate,
        endDate: filters.endDate,
        timeRange: filters.timeRange,
      };

      const secondaryItemData = await salesAnalyticsAPI.getItemSalesData(params, 'SECONDARY_ITEM');
      logger.info('2차 아이템 데이터 로드 완료', { count: secondaryItemData?.length || 0 });

      dashboardData.secondaryItemData = secondaryItemData;
    } catch (err) {
      logger.error('2차 아이템 데이터 로드 실패', err);
      dashboardData.secondaryItemData = [];
    }
  };

  // 1차 상품별 일별 매출추이 데이터 로드
  const loadPrimaryItemDailyTrendData = async () => {
    try {
      const params = {
        startDate: filters.startDate,
        endDate: filters.endDate,
        timeRange: filters.timeRange,
      };

      logger.info('1차 상품별 일별 매출추이 데이터 로드 시작', params);

      const primaryItemDailyTrendData =
        await salesAnalyticsAPI.getPrimaryItemDailyTrendData(params);
      logger.info('1차 상품별 일별 매출추이 데이터 로드 완료', {
        count: primaryItemDailyTrendData?.length || 0,
      });

      dashboardData.primaryItemDailyTrendData = primaryItemDailyTrendData || [];
    } catch (err) {
      logger.error('1차 상품별 일별 매출추이 데이터 로드 실패', err);
      dashboardData.primaryItemDailyTrendData = [];
    }
  };

  // 차트 옵션 생성 (computed로 반응형 연결)
  const getDailySalesChartOption = computed(() => {
    return (isDarkMode = false) =>
      createDailySalesChartOption(dashboardData.dailySales, isDarkMode);
  });

  const getCategoryChartOption = computed(() => {
    return (isDarkMode = false) =>
      createAdvancedSalesChartOption(dashboardData.categoryData, filters.chartType, isDarkMode);
  });

  const getGenderChartOption = computed(() => {
    return (isDarkMode = false) =>
      createAdvancedSalesChartOption(dashboardData.genderData, 'pie', isDarkMode);
  });

  const getTrendChartOption = computed(() => {
    return (isDarkMode = false) =>
      createAdvancedSalesChartOption(dashboardData.trendData, 'line', isDarkMode);
  });

  const getDiscountChartOption = computed(() => {
    return (isDarkMode = false) =>
      createDiscountAnalysisChartOption(dashboardData.discountData, isDarkMode);
  });

  const getPrimaryItemShareChartOption = computed(() => {
    return (isDarkMode = false) =>
      createPrimaryItemShareChartOption(dashboardData.primaryItemData, isDarkMode);
  });

  const getSecondaryItemShareChartOption = computed(() => {
    return (isDarkMode = false) =>
      createSecondaryItemShareChartOption(dashboardData.secondaryItemData, isDarkMode);
  });

  const getPrimaryItemTransactionShareChartOption = computed(() => {
    return (isDarkMode = false) =>
      createPrimaryItemTransactionShareChartOption(dashboardData.primaryItemData, isDarkMode);
  });

  const getSecondaryItemTransactionShareChartOption = computed(() => {
    return (isDarkMode = false) =>
      createSecondaryItemTransactionShareChartOption(dashboardData.secondaryItemData, isDarkMode);
  });

  const getPrimaryItemDailyTrendChartOption = computed(() => {
    return (isDarkMode = false) =>
      createPrimaryItemDailyTrendChartOption(dashboardData.primaryItemDailyTrendData, isDarkMode);
  });

  // 필터 업데이트
  const updateFilters = async newFilters => {
    Object.assign(filters, newFilters);

    // 날짜 관련 필터 변경 시 전체 데이터 다시 로드
    if (newFilters.startDate || newFilters.endDate || newFilters.timeRange) {
      await loadSalesData();
    }
  };

  // 필터 초기화
  const resetFilters = () => {
    Object.assign(filters, {
      startDate: getDateString(-30),
      endDate: getDateString(0),
      timeRange: 'CUSTOM',
      groupBy: 'WEEK',
      gender: null,
      categoryId: null,
      primaryItemId: null,
      secondaryItemId: null,
      analysisType: 'overview',
      chartType: 'bar',
    });
    loadSalesData();
  };

  // 대시보드 데이터 초기화
  const resetDashboardData = () => {
    Object.assign(dashboardData, {
      summary: {
        totalSales: 0,
        dailyAverage: 0,
        totalTransactions: 0,
        averageOrderValue: 0,
        startDate: null,
        endDate: null,
      },
      dailySales: [],
      categoryData: [],
      genderData: [],
      trendData: [],
      discountData: [],
      primaryItemData: [],
      secondaryItemData: [],
      primaryItemDailyTrendData: [],
      summaryCards: [],
    });
  };

  return {
    // 상태
    loading,
    error,
    dashboardData,
    filters,
    itemDataLoading,

    // 데이터 로드 메서드
    loadSalesData,
    loadSummaryData,
    loadDailySalesData,
    loadCategoryData,
    loadGenderData,
    loadTrendData,
    loadDiscountData,

    // 차트 옵션 (computed)
    getDailySalesChartOption,
    getCategoryChartOption,
    getGenderChartOption,
    getTrendChartOption,
    getDiscountChartOption,
    getPrimaryItemShareChartOption,
    getSecondaryItemShareChartOption,
    getPrimaryItemTransactionShareChartOption,
    getSecondaryItemTransactionShareChartOption,
    getPrimaryItemDailyTrendChartOption,

    // 액션 메서드
    updateFilters,
    resetFilters,

    // 유틸리티
    formatCurrency,
    formatPercentage,
  };
}

// ========== Sales 특화 유틸리티 함수들 ==========

/**
 * 기간에 따른 최적 그룹화 결정 (매출 추이용) - 최소 단위: 주간
 * @param {number} daysDiff - 일수 차이
 * @returns {string} 그룹화 타입
 */
function getOptimalGroupBy(daysDiff) {
  if (daysDiff <= 60) {
    return 'WEEK'; // 60일 이하: 주별
  } else {
    return 'MONTH'; // 60일 초과: 월별
  }
}

/**
 * 기간에 따른 최적 그룹화 결정 (할인 분석용) - 최소 단위: 주간
 * @param {number} daysDiff - 일수 차이
 * @returns {string} 그룹화 타입
 */
function getOptimalGroupByForDiscount(daysDiff) {
  if (daysDiff <= 45) {
    return 'WEEK'; // 45일 이하: 주별
  } else {
    return 'MONTH'; // 45일 초과: 월별
  }
}
