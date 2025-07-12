import api from '@/plugins/axios.js';
import { BaseAnalyticsAPI } from './baseAnalyticsAPI.js';

const BASE_URL = '/statistics/reservations';

class ReservationAnalyticsAPI extends BaseAnalyticsAPI {
  constructor() {
    super('ReservationAnalyticsAPI');
  }

  /**
   * 기본 예약율 통계 조회 (일별 예약율)
   * GET /statistics/reservations
   * @param {Object} params - 조회 파라미터
   * @param {string} params.startDate - 시작일 (YYYY-MM-DD)
   * @param {string} params.endDate - 종료일 (YYYY-MM-DD)
   * @returns {Promise<Array>} ReservationStatisticsResponse 배열
   */
  async getReservationStatistics(params) {
    try {
      const queryParams = this.buildQueryParams(params);
      const url = BASE_URL;

      this.logger.request('GET', url, queryParams);
      const response = await api.get(url, { params: queryParams });
      this.logger.response('GET', url, response.status, response.data);

      return this.transformReservationStatisticsData(response.data.data);
    } catch (error) {
      this.logger.error('GET', BASE_URL, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 고급 예약율 통계 조회
   * POST /statistics/reservations/advanced
   * @param {Object} requestBody - ReservationRequest 객체
   * @returns {Promise<Array>} ReservationStatisticsResponse 배열
   */
  async getAdvancedReservationStatistics(requestBody) {
    try {
      const url = `${BASE_URL}/advanced`;

      this.logger.request('POST', url, requestBody);
      const response = await api.post(url, requestBody);
      this.logger.response('POST', url, response.status, response.data);

      return this.transformReservationStatisticsData(response.data.data, requestBody.groupBy);
    } catch (error) {
      this.logger.error('POST', `${BASE_URL}/advanced`, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 예약율 요약 통계 조회
   * GET /statistics/reservations/summary
   * @param {Object} params - 조회 파라미터
   * @param {string} params.startDate - 시작일 (YYYY-MM-DD)
   * @param {string} params.endDate - 종료일 (YYYY-MM-DD)
   * @returns {Promise<Object>} ReservationSummaryResponse
   */
  async getReservationSummary(params) {
    try {
      const queryParams = this.buildQueryParams(params);
      const url = `${BASE_URL}/summary`;

      this.logger.request('GET', url, queryParams);
      const response = await api.get(url, { params: queryParams });
      this.logger.response('GET', url, response.status, response.data);

      return this.transformReservationSummaryData(response.data.data, params);
    } catch (error) {
      this.logger.error('GET', `${BASE_URL}/summary`, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 특정 직원의 예약율 통계 조회
   * GET /statistics/reservations/staff/{staffId}
   * @param {number} staffId - 직원 ID
   * @param {Object} params - 조회 파라미터
   * @returns {Promise<Array>} 직원별 예약율 데이터
   */
  async getStaffReservationStatistics(staffId, params) {
    try {
      const queryParams = this.buildQueryParams(params);
      const url = `${BASE_URL}/staff/${staffId}`;

      this.logger.request('GET', url, queryParams);
      const response = await api.get(url, { params: queryParams });
      this.logger.response('GET', url, response.status, response.data);

      return this.transformReservationStatisticsData(response.data.data);
    } catch (error) {
      this.logger.error('GET', `${BASE_URL}/staff/${staffId}`, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 모든 직원의 예약율 통계 조회
   * GET /statistics/reservations/staff
   * @param {Object} params - 조회 파라미터
   * @returns {Promise<Array>} 모든 직원의 예약율 데이터
   */
  async getAllStaffReservationStatistics(params) {
    try {
      const queryParams = this.buildQueryParams(params);
      const url = `${BASE_URL}/staff`;

      this.logger.request('GET', url, queryParams);
      const response = await api.get(url, { params: queryParams });
      this.logger.response('GET', url, response.status, response.data);

      return this.transformReservationStatisticsData(response.data.data);
    } catch (error) {
      this.logger.error('GET', `${BASE_URL}/staff`, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 시간대별 방문객 통계 조회
   * GET /statistics/visitors/hourly
   * @param {Object} params - 조회 파라미터
   * @returns {Promise<Array>} 시간대별 방문객 데이터
   */
  async getHourlyVisitorStatistics(params) {
    try {
      const queryParams = this.buildQueryParams(params);
      const url = `/statistics/visitors/hourly`;

      this.logger.request('GET', url, queryParams);
      const response = await api.get(url, { params: queryParams });
      this.logger.response('GET', url, response.status, response.data);

      return this.transformHourlyVisitorData(response.data.data);
    } catch (error) {
      this.logger.error('GET', `/statistics/visitors/hourly`, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 일별 방문객 통계 조회
   * GET /statistics/visitors/daily
   * @param {Object} params - 조회 파라미터
   * @returns {Promise<Array>} 일별 방문객 데이터
   */
  async getDailyVisitorStatistics(params) {
    try {
      const queryParams = this.buildQueryParams(params);
      const url = `/statistics/visitors/daily`;

      this.logger.request('GET', url, queryParams);
      const response = await api.get(url, { params: queryParams });
      this.logger.response('GET', url, response.status, response.data);

      return this.transformDailyVisitorData(response.data.data);
    } catch (error) {
      this.logger.error('GET', `/statistics/visitors/daily`, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 일별 예약율 통계 조회
   * @param {Object} params - 조회 파라미터
   * @returns {Promise<Array>} 일별 예약율 데이터
   */
  async getDailyReservationData(params) {
    const requestBody = {
      startDate: params.startDate,
      endDate: params.endDate,
      groupBy: 'DAY',
      timeRange: params.timeRange || 'CUSTOM',
    };

    return await this.getAdvancedReservationStatistics(requestBody);
  }

  /**
   * 주별 예약율 통계 조회
   * @param {Object} params - 조회 파라미터
   * @returns {Promise<Array>} 주별 예약율 데이터
   */
  async getWeeklyReservationData(params) {
    const requestBody = {
      startDate: params.startDate,
      endDate: params.endDate,
      groupBy: 'WEEK',
      timeRange: params.timeRange || 'CUSTOM',
    };

    return await this.getAdvancedReservationStatistics(requestBody);
  }

  /**
   * 월별 예약율 통계 조회
   * @param {Object} params - 조회 파라미터
   * @returns {Promise<Array>} 월별 예약율 데이터
   */
  async getMonthlyReservationData(params) {
    const requestBody = {
      startDate: params.startDate,
      endDate: params.endDate,
      groupBy: 'MONTH',
      timeRange: params.timeRange || 'CUSTOM',
    };

    return await this.getAdvancedReservationStatistics(requestBody);
  }

  /**
   * 시간대별 예약율 통계 조회 (30분 단위)
   * @param {Object} params - 조회 파라미터
   * @returns {Promise<Array>} 시간대별 예약율 데이터
   */
  async getTimeSlotReservationData(params) {
    const requestBody = {
      startDate: params.startDate,
      endDate: params.endDate,
      groupBy: 'TIME_SLOT',
      timeRange: params.timeRange || 'CUSTOM',
    };

    return await this.getAdvancedReservationStatistics(requestBody);
  }

  /**
   * 요일별/시간대별 히트맵용 예약율 통계 조회
   * @param {Object} params - 조회 파라미터
   * @returns {Promise<Array>} 요일별/시간대별 예약율 데이터
   */
  async getDayTimeSlotReservationData(params) {
    const requestBody = {
      startDate: params.startDate,
      endDate: params.endDate,
      groupBy: 'DAY_TIME_SLOT',
      timeRange: params.timeRange || 'CUSTOM',
    };

    return await this.getAdvancedReservationStatistics(requestBody);
  }

  /**
   * 직원별 예약율 통계 조회
   * @param {Object} params - 조회 파라미터
   * @param {number|null} staffId - 특정 직원 ID (null이면 모든 직원)
   * @returns {Promise<Array>} 직원별 예약율 데이터
   */
  async getStaffBasedReservationData(params, staffId = null) {
    if (staffId) {
      return await this.getStaffReservationStatistics(staffId, params);
    } else {
      return await this.getAllStaffReservationStatistics(params);
    }
  }

  // buildQueryParams는 BaseAnalyticsAPI에서 상속받음

  /**
   * 예약율 통계 데이터 변환
   * @param {Array} data - ReservationStatisticsResponse 배열
   * @param {string} groupBy - 그룹화 기준
   * @returns {Array} 변환된 예약율 데이터
   */
  transformReservationStatisticsData(data, groupBy = 'DAY') {
    if (!Array.isArray(data)) return [];

    return data.map(item => ({
      // 원본 데이터
      date: item.date,
      timeSlot: item.timeSlot,
      totalSlots: item.totalSlots || 0,
      reservedSlots: item.reservedSlots || 0,
      availableSlots: item.availableSlots || 0,
      reservationRate: item.reservationRate || 0,
      groupBy: item.groupBy || groupBy,
      displayKey: item.displayKey,
      staffId: item.staffId,
      staffName: item.staffName,

      // 추가 계산된 값들
      utilizationRate: item.reservationRate || 0, // 예약율과 동일
      availabilityRate:
        item.totalSlots > 0
          ? Number((((item.availableSlots || 0) * 100) / item.totalSlots).toFixed(2))
          : 0,

      // 표시용 포맷팅
      formattedDate: this.formatDisplayDate(item.date, item.timeSlot, groupBy),
      formattedTimeSlot: this.formatTimeSlot(item.timeSlot),
      formattedRate: `${item.reservationRate || 0}%`,
    }));
  }

  /**
   * 예약율 요약 데이터 변환
   * @param {Object} data - ReservationSummaryResponse
   * @param {Object} params - 요청 파라미터
   * @returns {Object} 변환된 예약율 요약 데이터
   */
  transformReservationSummaryData(data, params) {
    if (!data)
      return {
        totalSlots: 0,
        totalReservedSlots: 0,
        totalAvailableSlots: 0,
        averageReservationRate: 0,
        totalDays: 0,
        dailyAverageReservations: 0,
        peakHourSlots: 0,
        offPeakHourSlots: 0,
        peakHourReservationRate: 0,
        offPeakHourReservationRate: 0,
      };

    return {
      totalSlots: data.totalSlots || 0,
      totalReservedSlots: data.totalReservedSlots || 0,
      totalAvailableSlots: data.totalAvailableSlots || 0,
      averageReservationRate: data.averageReservationRate || 0,
      totalDays: data.totalDays || 0,
      dailyAverageReservations: data.dailyAverageReservations || 0,
      peakHourSlots: data.peakHourSlots || 0,
      offPeakHourSlots: data.offPeakHourSlots || 0,
      peakHourReservationRate: data.peakHourReservationRate || 0,
      offPeakHourReservationRate: data.offPeakHourReservationRate || 0,

      // 추가 계산된 값들
      utilizationGrowth: 0, // 필요시 추가 계산
      averageUsageTime: 30, // 기본 30분 (실제로는 서비스 시간에 따라 계산)
      staffUtilization: data.averageReservationRate || 0, // 전체 예약율을 직원 가동률로 사용
      peakHourUtilization: data.peakHourReservationRate || 0,
    };
  }

  /**
   * 시간대별 방문객 데이터 변환
   * @param {Array} data - HourlyVisitorStatisticsResponse 배열
   * @returns {Array} 변환된 시간대별 방문객 데이터
   */
  transformHourlyVisitorData(data) {
    if (!Array.isArray(data)) return [];

    return data.map(item => ({
      hour: item.hour || 0,
      timeSlot: item.timeSlot || `${item.hour}시`,
      displayTime: item.displayTime || `${item.hour}시`,
      maleVisitors: item.maleVisitors || 0,
      femaleVisitors: item.femaleVisitors || 0,
      totalVisitors: item.totalVisitors || 0,

      // 차트 표시용 데이터
      male: item.maleVisitors || 0,
      female: item.femaleVisitors || 0,
      total: item.totalVisitors || 0,
    }));
  }

  /**
   * 일별 방문객 데이터 변환
   * @param {Array} data - DailyVisitorStatisticsResponse 배열
   * @returns {Array} 변환된 일별 방문객 데이터
   */
  transformDailyVisitorData(data) {
    if (!Array.isArray(data)) return [];

    return data.map(item => ({
      date: item.date,
      dayOfWeek: item.dayOfWeek || '',
      displayDate: item.displayDate || '',
      maleVisitors: item.maleVisitors || 0,
      femaleVisitors: item.femaleVisitors || 0,
      totalVisitors: item.totalVisitors || 0,

      // 차트 표시용 데이터
      male: item.maleVisitors || 0,
      female: item.femaleVisitors || 0,
      total: item.totalVisitors || 0,
      label: `${item.displayDate} (${item.dayOfWeek})`,
    }));
  }

  /**
   * 날짜 표시 포맷팅
   * @param {string} date - 날짜
   * @param {string} timeSlot - 시간대
   * @param {string} groupBy - 그룹화 기준
   * @returns {string} 포맷된 날짜
   */
  formatDisplayDate(date, timeSlot, groupBy) {
    if (!date) return '';

    switch (groupBy) {
      case 'DAY':
        return this.formatDateKorean(date);
      case 'WEEK':
        return this.formatWeekRange(date);
      case 'MONTH':
        return this.formatMonthDisplay(date);
      case 'TIME_SLOT':
      case 'HOUR':
        return this.formatTimeSlot(timeSlot);
      case 'STAFF':
        return this.formatDateKorean(date);
      default:
        return date;
    }
  }

  // formatDateKorean, formatWeekRange, formatMonthDisplay, formatTimeSlot
  // 은 BaseAnalyticsAPI에서 상속받음

  /**
   * API 에러 처리
   * @param {Error} error - 에러 객체
   * @returns {Error} 변환된 에러
   */
  handleApiError(error) {
    if (error.response) {
      const { status, data } = error.response;

      switch (status) {
        case 400:
          return new Error(data.message || '잘못된 요청입니다. 입력 데이터를 확인해주세요.');
        case 401:
          return new Error('인증이 필요합니다. 다시 로그인해주세요.');
        case 403:
          return new Error('접근 권한이 없습니다.');
        case 404:
          return new Error('예약율 통계 데이터를 찾을 수 없습니다.');
        case 422:
          return new Error(data.message || '유효하지 않은 데이터입니다.');
        case 429:
          return new Error('요청이 너무 많습니다. 잠시 후 다시 시도해주세요.');
        case 500:
          return new Error('서버 오류가 발생했습니다. 관리자에게 문의해주세요.');
        case 502:
        case 503:
        case 504:
          return new Error('서버가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.');
        default:
          return new Error(
            data.message || `알 수 없는 오류가 발생했습니다. (상태 코드: ${status})`
          );
      }
    } else if (error.request) {
      return new Error('네트워크 오류가 발생했습니다. 연결을 확인해주세요.');
    } else {
      return new Error(error.message || '오류가 발생했습니다.');
    }
  }
}

// 싱글톤 인스턴스 생성
const reservationAnalyticsAPI = new ReservationAnalyticsAPI();

export default reservationAnalyticsAPI;
