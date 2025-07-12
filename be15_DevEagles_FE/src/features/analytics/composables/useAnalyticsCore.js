import { ref, reactive, computed } from 'vue';

/**
 * Analytics 공통 기능 Composable
 * 로딩, 에러 처리, 날짜 필터링 등 공통 로직 제공
 */
export function useAnalyticsCore() {
  // 공통 상태
  const loading = ref(false);
  const error = ref(null);

  /**
   * 날짜 문자열 생성 (오늘 기준 상대적)
   * @param {number} daysOffset - 일수 오프셋 (음수: 과거, 양수: 미래)
   * @returns {string} YYYY-MM-DD 형태의 날짜 문자열
   */
  const getDateString = (daysOffset = 0) => {
    const date = new Date();
    date.setDate(date.getDate() + daysOffset);
    return date.toISOString().split('T')[0];
  };

  /**
   * 기본 날짜 범위 필터 생성
   * @param {number} defaultDaysBack - 기본 과거 일수 (기본값: 30일)
   * @returns {Object} 날짜 범위 객체
   */
  const createDateRangeFilter = (defaultDaysBack = 30) => {
    return reactive({
      start: getDateString(-defaultDaysBack),
      end: getDateString(0),
    });
  };

  /**
   * 두 날짜 사이의 일수 차이 계산
   * @param {string} startDate - 시작일 (YYYY-MM-DD)
   * @param {string} endDate - 종료일 (YYYY-MM-DD)
   * @returns {number} 일수 차이
   */
  const calculateDaysDifference = (startDate, endDate) => {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diffTime = Math.abs(end - start);
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  };

  /**
   * 안전한 API 호출 래퍼
   * @param {Function} apiCall - API 호출 함수
   * @param {Object} options - 옵션
   * @returns {Promise} API 호출 결과
   */
  const safeApiCall = async (apiCall, options = {}) => {
    const {
      loadingRef = loading,
      errorRef = error,
      onSuccess = null,
      onError = null,
      resetErrorOnStart = true,
    } = options;

    if (loadingRef.value) return null;

    loadingRef.value = true;
    if (resetErrorOnStart) errorRef.value = null;

    try {
      const result = await apiCall();

      if (onSuccess) {
        onSuccess(result);
      }

      return result;
    } catch (err) {
      console.error('API 호출 실패:', err);
      const errorMessage = err.message || '데이터를 불러오는 중 오류가 발생했습니다.';
      errorRef.value = errorMessage;

      if (onError) {
        onError(err);
      }

      throw err;
    } finally {
      loadingRef.value = false;
    }
  };

  /**
   * 병렬 API 호출 래퍼
   * @param {Array} apiCalls - API 호출 함수 배열
   * @param {Object} options - 옵션
   * @returns {Promise<Array>} API 호출 결과 배열
   */
  const safeParallelApiCalls = async (apiCalls, options = {}) => {
    const {
      loadingRef = loading,
      errorRef = error,
      continueOnError = true,
      resetErrorOnStart = true,
    } = options;

    if (loadingRef.value) return [];

    loadingRef.value = true;
    if (resetErrorOnStart) errorRef.value = null;

    try {
      const promises = apiCalls.map(apiCall => {
        return continueOnError
          ? apiCall().catch(err => {
              console.warn('개별 API 호출 실패:', err);
              return null; // 실패 시 null 반환하여 다른 호출 계속 진행
            })
          : apiCall();
      });

      const results = await Promise.all(promises);
      return results;
    } catch (err) {
      console.error('병렬 API 호출 실패:', err);
      const errorMessage = err.message || '데이터를 불러오는 중 오류가 발생했습니다.';
      errorRef.value = errorMessage;
      throw err;
    } finally {
      loadingRef.value = false;
    }
  };

  /**
   * 필터 업데이트 헬퍼
   * @param {Object} filters - 필터 reactive 객체
   * @param {Object} newFilters - 새로운 필터값들
   * @param {Function} reloadCallback - 데이터 재로드 콜백
   */
  const updateFilters = (filters, newFilters, reloadCallback = null) => {
    Object.assign(filters, newFilters);

    if (reloadCallback && typeof reloadCallback === 'function') {
      reloadCallback();
    }
  };

  /**
   * 에러 초기화
   */
  const clearError = () => {
    error.value = null;
  };

  /**
   * 로딩 상태 체크 (computed)
   */
  const isLoading = computed(() => loading.value);

  /**
   * 에러 상태 체크 (computed)
   */
  const hasError = computed(() => !!error.value);

  /**
   * 데이터 준비 상태 체크 (computed)
   */
  const isReady = computed(() => !loading.value && !error.value);

  return {
    // 상태
    loading,
    error,
    isLoading,
    hasError,
    isReady,

    // 날짜 유틸리티
    getDateString,
    createDateRangeFilter,
    calculateDaysDifference,

    // API 호출 유틸리티
    safeApiCall,
    safeParallelApiCalls,

    // 필터 관리
    updateFilters,

    // 에러 관리
    clearError,
  };
}
