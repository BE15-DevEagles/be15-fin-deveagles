import api from '@/plugins/axios.js';
import { safeDivide } from '../utils/formatters.js';
import { BaseAnalyticsAPI } from './baseAnalyticsAPI.js';

const BASE_URL = '/statistics';

class SalesAnalyticsAPI extends BaseAnalyticsAPI {
  constructor() {
    super('SalesAnalyticsAPI');
  }
  /**
   * 기본 매출 통계 조회 (일별 매출)
   * GET /statistics/sales
   * @param {Object} params - 조회 파라미터
   * @param {string} params.startDate - 시작일 (YYYY-MM-DD)
   * @param {string} params.endDate - 종료일 (YYYY-MM-DD)
   * @returns {Promise<Array>} SalesStatisticsResponse 배열
   */
  async getSalesStatistics(params) {
    try {
      const queryParams = this.buildQueryParams(params);
      const url = `${BASE_URL}/sales`;

      this.logger.request('GET', url, queryParams);
      const response = await api.get(url, { params: queryParams });
      this.logger.response('GET', url, response.status, response.data);

      return this.transformSalesStatisticsData(response.data.data);
    } catch (error) {
      this.logger.error('GET', `${BASE_URL}/sales`, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 고급 매출 통계 조회
   * POST /statistics/sales/advanced
   * @param {Object} requestBody - StatisticsRequest 객체
   * @returns {Promise<Array>} AdvancedSalesStatisticsResponse 배열
   */
  async getAdvancedSalesStatistics(params, requestBody) {
    try {
      const url = `${BASE_URL}/sales/advanced`;

      this.logger.request('POST', url, requestBody);
      const response = await api.post(url, requestBody);
      this.logger.response('POST', url, response.status, response.data);

      return this.transformAdvancedSalesData(response.data.data, requestBody.groupBy);
    } catch (error) {
      this.logger.error('POST', `${BASE_URL}/sales/advanced`, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 매출 요약 통계 조회
   * GET /statistics/sales/summary
   * @param {Object} params - 조회 파라미터
   * @param {string} params.startDate - 시작일 (YYYY-MM-DD)
   * @param {string} params.endDate - 종료일 (YYYY-MM-DD)
   * @returns {Promise<Object>} SalesSummaryResponse
   */
  async getSalesSummary(params) {
    try {
      const queryParams = this.buildQueryParams(params);
      const url = `${BASE_URL}/sales/summary`;

      this.logger.request('GET', url, queryParams);
      const response = await api.get(url, { params: queryParams });
      this.logger.response('GET', url, response.status, response.data);

      return this.transformSalesSummaryData(response.data.data, params);
    } catch (error) {
      this.logger.error('GET', `${BASE_URL}/sales/summary`, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 카테고리별 매출 통계 조회
   * @param {Object} params - 조회 파라미터
   * @returns {Promise<Array>} 카테고리별 매출 데이터
   */
  async getCategorySalesData(params) {
    const requestBody = {
      startDate: params.startDate,
      endDate: params.endDate,
      groupBy: 'CATEGORY',
      timeRange: params.timeRange || 'CUSTOM',
    };

    return await this.getAdvancedSalesStatistics(params, requestBody);
  }

  /**
   * 성별 매출 통계 조회
   * @param {Object} params - 조회 파라미터
   * @returns {Promise<Array>} 성별 매출 데이터
   */
  async getGenderSalesData(params) {
    const requestBody = {
      startDate: params.startDate,
      endDate: params.endDate,
      groupBy: 'GENDER',
      timeRange: params.timeRange || 'CUSTOM',
    };

    return await this.getAdvancedSalesStatistics(params, requestBody);
  }

  /**
   * 상품별 매출 통계 조회
   * @param {Object} params - 조회 파라미터
   * @param {string} itemType - 'PRIMARY_ITEM' 또는 'SECONDARY_ITEM'
   * @returns {Promise<Array>} 상품별 매출 데이터
   */
  async getItemSalesData(params, itemType = 'SECONDARY_ITEM') {
    const requestBody = {
      startDate: params.startDate,
      endDate: params.endDate,
      groupBy: itemType,
      timeRange: params.timeRange || 'CUSTOM',
    };

    return await this.getAdvancedSalesStatistics(params, requestBody);
  }

  /**
   * 1차 상품별 일별 매출추이 조회
   * @param {Object} params - 조회 파라미터
   * @param {string} params.startDate - 시작일 (YYYY-MM-DD)
   * @param {string} params.endDate - 종료일 (YYYY-MM-DD)
   * @returns {Promise<Array>} 1차 상품별 일별 매출추이 데이터
   */
  async getPrimaryItemDailyTrendData(params) {
    try {
      const url = `${BASE_URL}/sales/primary-item-daily-trend`;
      const queryParams = this.buildQueryParams(params);

      this.logger.request('GET', url, queryParams);
      const response = await api.get(url, { params: queryParams });
      this.logger.response('GET', url, response.status, response.data);

      return this.transformPrimaryItemDailyTrendData(response.data.data);
    } catch (error) {
      this.logger.error('GET', `${BASE_URL}/sales/primary-item-daily-trend`, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 1차 상품별 일별 매출추이 데이터 변환
   * @param {Array} data - 원본 데이터 배열
   * @returns {Array} 변환된 1차 상품별 일별 매출추이 데이터
   */
  transformPrimaryItemDailyTrendData(data) {
    if (!Array.isArray(data)) return [];

    console.log('🔍 1차 상품별 일별 매출추이 원본 데이터:', data);

    const transformedData = data.map(item => {
      // 날짜 형식 정규화 (YYYY-MM-DD 형태로 변환)
      let normalizedDate = item.date;
      if (normalizedDate && normalizedDate.includes(' ')) {
        normalizedDate = normalizedDate.split(' ')[0]; // 시간 부분 제거
      }

      const transformed = {
        date: normalizedDate,
        primaryItemName: item.primaryItemName || item.displayKey || '미분류',
        totalSalesAmount: item.totalSalesAmount || item.salesAmount || 0,
        totalTransactions: item.totalTransactions || item.transactions || 0,
        totalDiscountAmount: item.totalDiscountAmount || 0,
        totalCouponDiscountAmount: item.totalCouponDiscountAmount || 0,
        displayKey: item.primaryItemName || item.displayKey || '미분류',
        groupBy: 'PRIMARY_ITEM_DAILY',
      };

      return transformed;
    });

    console.log('🔍 1차 상품별 일별 매출추이 변환된 데이터:', transformedData);
    return transformedData;
  }

  // buildQueryParams는 BaseAnalyticsAPI에서 상속받음

  /**
   * 기본 매출 통계 데이터 변환
   * @param {Array} data - SalesStatisticsResponse 배열
   * @returns {Array} 변환된 일별 매출 데이터
   */
  transformSalesStatisticsData(data) {
    if (!Array.isArray(data)) return [];

    return data.map(item => ({
      date: item.date,
      sales: item.totalSalesAmount || 0,
      transactions: item.totalTransactions || 0,
      averageOrderValue: safeDivide(item.totalSalesAmount, item.totalTransactions, 0),
    }));
  }

  /**
   * 고급 매출 통계 데이터 변환
   * @param {Array} data - AdvancedSalesStatisticsResponse 배열
   * @param {string} groupBy - 그룹화 기준
   * @returns {Array} 변환된 고급 매출 데이터
   */
  transformAdvancedSalesData(data, groupBy) {
    if (!Array.isArray(data)) return [];

    return data.map(item => {
      const totalSales = item.totalSalesAmount || 0;
      const totalDiscount = item.totalDiscountAmount || 0;
      const totalCouponDiscount = item.totalCouponDiscountAmount || 0;

      return {
        // 그룹화 키들
        date: item.date || null,
        gender: item.gender || null,
        category: item.category || null,
        primaryItemName: item.primaryItemName || null,
        secondaryItemName: item.secondaryItemName || null,

        // 집계 값들
        totalSalesAmount: totalSales,
        totalTransactions: item.totalTransactions || 0,
        totalDiscountAmount: totalDiscount,
        totalCouponDiscountAmount: totalCouponDiscount,

        // 계산된 값들 - 안전한 나눗셈 사용
        averageOrderValue: Math.floor(safeDivide(totalSales, item.totalTransactions, 0)),
        discountRate:
          totalSales > 0 ? Number(safeDivide(totalDiscount * 100, totalSales, 0).toFixed(1)) : 0,
        couponUsageRate:
          totalDiscount > 0
            ? Number(safeDivide(totalCouponDiscount * 100, totalDiscount, 0).toFixed(1))
            : 0,

        // 표시용 키 (그룹화 기준에 따라 결정)
        displayKey: this.getDisplayKey(item, groupBy),
        groupBy: groupBy,
      };
    });
  }

  /**
   * 매출 요약 데이터 변환
   * @param {Object} data - SalesSummaryResponse
   * @param {Object} params - 요청 파라미터 (일평균 계산용)
   * @returns {Object} 변환된 매출 요약 데이터
   */
  transformSalesSummaryData(data, params) {
    if (!data)
      return {
        totalSales: 0,
        dailyAverage: 0,
        totalTransactions: 0,
        averageOrderValue: 0,
        startDate: null,
        endDate: null,
      };

    // 일평균 매출 재계산
    let calculatedDailyAverage = data.dailyAverage || 0;

    if (params && params.startDate && params.endDate && data.totalSales) {
      const startDate = new Date(params.startDate);
      const endDate = new Date(params.endDate);
      const daysDiff = Math.ceil((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1;

      if (daysDiff > 0) {
        calculatedDailyAverage = Math.floor(data.totalSales / daysDiff);
      }
    }

    return {
      totalSales: data.totalSales || 0,
      dailyAverage: calculatedDailyAverage,
      totalTransactions: data.totalTransactions || 0,
      averageOrderValue: Math.floor(data.averageOrderValue || 0),
      startDate: data.startDate,
      endDate: data.endDate,
    };
  }

  /**
   * 표시용 키 추출
   * @param {Object} item - 데이터 항목
   * @param {string} groupBy - 그룹화 기준
   * @returns {string} 표시용 키
   */
  getDisplayKey(item, groupBy) {
    switch (groupBy) {
      case 'WEEK':
        return item.date ? this.formatWeekRange(item.date) : '주차 없음';
      case 'MONTH':
        return item.date ? this.formatMonthDisplay(item.date) : '월 없음';
      case 'GENDER':
        return this.formatGender(item.gender);
      case 'CATEGORY':
        return item.category || '카테고리 없음';
      case 'PRIMARY_ITEM':
        return item.primaryItemName || '1차 상품 없음';
      case 'SECONDARY_ITEM':
        return item.secondaryItemName || '2차 상품 없음';
      default:
        return '기타';
    }
  }

  /**
   * 월 표시용 포맷팅 (백엔드에서 YYYY-MM 형태로 제공)
   * @param {string} dateStr - 백엔드에서 받은 날짜 문자열 (YYYY-MM 또는 기존 형태)
   * @returns {string} 포맷된 월 (YY-MM월)
   */
  formatMonthDisplay(dateStr) {
    if (!dateStr) return '';

    // 백엔드에서 YYYY-MM 형태로 올 경우
    if (/^\d{4}-\d{2}$/.test(dateStr)) {
      const [year, month] = dateStr.split('-');
      const shortYear = year.slice(-2);
      return `${shortYear}-${month}월`;
    }

    // 기존 로직들 (하위 호환성 유지)
    if (/^\d{1,2}$/.test(dateStr)) {
      const monthNum = parseInt(dateStr);
      const currentYear = new Date().getFullYear().toString().slice(-2);
      return monthNum >= 1 && monthNum <= 12
        ? `${currentYear}-${String(monthNum).padStart(2, '0')}월`
        : '';
    }

    // 날짜 형태인 경우
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return '';
    const shortYear = date.getFullYear().toString().slice(-2);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    return `${shortYear}-${month}월`;
  }

  /**
   * 월 포맷팅 (기존 유지 - 다른 곳에서 사용할 수 있음)
   * @param {string} dateStr - 날짜 문자열
   * @returns {string} 포맷된 월
   */
  formatMonth(dateStr) {
    if (!dateStr) return '';

    // 백엔드에서 YYYY-MM 형태로 올 경우
    if (/^\d{4}-\d{2}$/.test(dateStr)) {
      return dateStr;
    }

    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return '';
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  /**
   * 주차 계산
   * @param {string} dateStr - 날짜 문자열
   * @returns {number} 주차 번호
   */
  getWeekNumber(dateStr) {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return 0;
    const startOfYear = new Date(date.getFullYear(), 0, 1);
    const days = Math.floor((date - startOfYear) / (24 * 60 * 60 * 1000));
    return Math.ceil((days + startOfYear.getDay() + 1) / 7);
  }

  /**
   * 주별 날짜 범위 포맷팅 (백엔드에서 YYYY-WW 형태로 제공)
   * @param {string} dateStr - 백엔드에서 받은 주차 문자열 (YYYY-WW 또는 기존 형태)
   * @returns {string} 포맷된 주 범위 (월/일~월/일)
   */
  formatWeekRange(dateStr) {
    if (!dateStr) return '';

    // 백엔드에서 YYYY-WW 형태로 올 경우
    if (/^\d{4}-\d{2}$/.test(dateStr)) {
      const [year, week] = dateStr.split('-');
      const weekNum = parseInt(week);

      // 해당 연도의 첫 번째 날짜
      const startOfYear = new Date(parseInt(year), 0, 1);

      // 첫 번째 주의 시작일 계산 (월요일 기준)
      const firstMonday = new Date(startOfYear);
      const dayOfWeek = startOfYear.getDay(); // 0: 일요일, 1: 월요일, ...
      const daysToMonday = dayOfWeek === 0 ? 1 : 8 - dayOfWeek;
      firstMonday.setDate(startOfYear.getDate() + daysToMonday);

      // 해당 주차의 시작일 계산
      const weekStartDate = new Date(firstMonday);
      weekStartDate.setDate(firstMonday.getDate() + (weekNum - 1) * 7);

      // 주차의 끝일 계산
      const weekEndDate = new Date(weekStartDate);
      weekEndDate.setDate(weekStartDate.getDate() + 6);

      return `${weekStartDate.getMonth() + 1}/${weekStartDate.getDate()}~${weekEndDate.getMonth() + 1}/${weekEndDate.getDate()}`;
    }

    // 기존 로직들 (하위 호환성 유지)
    if (/^\d{1,2}$/.test(dateStr)) {
      const weekNum = parseInt(dateStr);
      const currentYear = new Date().getFullYear();
      const startOfYear = new Date(currentYear, 0, 1);
      const startOfWeek = new Date(startOfYear.getTime() + (weekNum - 1) * 7 * 24 * 60 * 60 * 1000);
      const endOfWeek = new Date(startOfWeek.getTime() + 6 * 24 * 60 * 60 * 1000);

      return `${startOfWeek.getMonth() + 1}/${startOfWeek.getDate()}~${endOfWeek.getMonth() + 1}/${endOfWeek.getDate()}`;
    }

    // 날짜 형태인 경우
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;

    // 해당 주의 시작일 (월요일) 계산
    const dayOfWeek = date.getDay();
    const startOfWeek = new Date(date);
    startOfWeek.setDate(date.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));

    // 주의 끝일 (일요일) 계산
    const endOfWeek = new Date(startOfWeek);
    endOfWeek.setDate(startOfWeek.getDate() + 6);

    return `${startOfWeek.getMonth() + 1}/${startOfWeek.getDate()}~${endOfWeek.getMonth() + 1}/${endOfWeek.getDate()}`;
  }

  /**
   * 연도-월 포맷팅 (24-01, 24-02 형식) - 백엔드에서 YYYY-MM으로 오므로 변환
   * @param {string} dateStr - 백엔드에서 받은 날짜 문자열 (YYYY-MM 또는 기존 형태)
   * @returns {string} 포맷된 연도-월 (YY-MM)
   */
  formatMonthWithYear(dateStr) {
    if (!dateStr) return '';

    // 백엔드에서 YYYY-MM 형태로 올 경우
    if (/^\d{4}-\d{2}$/.test(dateStr)) {
      const [year, month] = dateStr.split('-');
      const shortYear = year.slice(-2);
      return `${shortYear}-${month}`;
    }

    // 기존 로직들 (하위 호환성 유지)
    if (/^\d{1,2}$/.test(dateStr)) {
      const monthNum = parseInt(dateStr);
      if (monthNum >= 1 && monthNum <= 12) {
        const currentYear = new Date().getFullYear();
        const shortYear = currentYear.toString().slice(-2);
        return `${shortYear}-${String(monthNum).padStart(2, '0')}`;
      }
      return '';
    }

    // 날짜 형태인 경우
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;

    const shortYear = date.getFullYear().toString().slice(-2);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    return `${shortYear}-${month}`;
  }

  /**
   * 성별 포맷팅
   * @param {string} gender - 성별 코드
   * @returns {string} 포맷된 성별
   */
  formatGender(gender) {
    switch (gender) {
      case 'M':
      case 'MALE':
        return '남성';
      case 'F':
      case 'FEMALE':
        return '여성';
      default:
        return '기타';
    }
  }

  // handleApiError는 BaseAnalyticsAPI에서 상속받음
}

// 싱글톤 인스턴스 생성
const salesAnalyticsAPI = new SalesAnalyticsAPI();

export default salesAnalyticsAPI;
