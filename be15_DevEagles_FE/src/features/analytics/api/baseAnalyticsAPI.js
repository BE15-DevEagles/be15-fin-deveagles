import { getApiLogger } from '@/plugins/LoggerManager.js';

/**
 * Analytics API 베이스 클래스
 * 공통 기능들을 제공하여 중복 제거
 */
export class BaseAnalyticsAPI {
  constructor(serviceName) {
    this.logger = getApiLogger(serviceName);
  }

  /**
   * 공통 쿼리 파라미터 빌딩
   * @param {Object} params - 파라미터 객체
   * @returns {Object} 쿼리 파라미터 객체
   */
  buildQueryParams(params) {
    const queryParams = {};

    if (params.startDate) queryParams.startDate = params.startDate;
    if (params.endDate) queryParams.endDate = params.endDate;

    return queryParams;
  }

  /**
   * 공통 API 에러 처리
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
          return new Error('데이터를 찾을 수 없습니다.');
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

  /**
   * 한국어 날짜 포맷팅 (공통)
   * @param {string} dateStr - 날짜 문자열
   * @returns {string} 포맷된 날짜 (MM/DD)
   */
  formatDateKorean(dateStr) {
    if (!dateStr) return '';

    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;

    const month = date.getMonth() + 1;
    const day = date.getDate();
    return `${month}/${day}`;
  }

  /**
   * 시간대 포맷팅 (공통)
   * @param {string} timeSlot - 시간대
   * @returns {string} 포맷된 시간대
   */
  formatTimeSlot(timeSlot) {
    if (!timeSlot) return '';

    // HH:MM 형태로 변환
    const time = timeSlot.includes(':') ? timeSlot : `${timeSlot}:00`;
    const [hour, minute] = time.split(':');
    const hourNum = parseInt(hour);

    if (minute === '00') {
      return `${hourNum}시`;
    } else {
      return `${hourNum}:${minute}`;
    }
  }

  /**
   * 주차 범위 포맷팅 (공통)
   * @param {string} dateStr - 주차 문자열
   * @returns {string} 포맷된 주 범위
   */
  formatWeekRange(dateStr) {
    if (!dateStr) return '';

    // YYYY-WW 형태 처리
    if (/^\d{4}-\d{2}$/.test(dateStr)) {
      const [year, week] = dateStr.split('-');
      const weekNum = parseInt(week);
      return `${week}주차`;
    }

    return dateStr;
  }

  /**
   * 월 표시 포맷팅 (공통)
   * @param {string} dateStr - 월 문자열
   * @returns {string} 포맷된 월
   */
  formatMonthDisplay(dateStr) {
    if (!dateStr) return '';

    // YYYY-MM 형태 처리
    if (/^\d{4}-\d{2}$/.test(dateStr)) {
      const [year, month] = dateStr.split('-');
      const shortYear = year.slice(-2);
      return `${shortYear}-${month}월`;
    }

    return dateStr;
  }
}
