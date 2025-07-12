import { defineStore } from 'pinia';
import { getComposableLogger } from '@/plugins/LoggerManager.js';

const logger = getComposableLogger('AnalyticsStore');

// 캐시 만료 시간 (1시간)
const CACHE_EXPIRY_TIME = 60 * 60 * 1000;

export const useAnalyticsStore = defineStore('analytics', {
  state: () => ({
    // 매출 통계 캐시
    salesCache: {
      data: null,
      filters: null,
      timestamp: null,
      loading: false,
      error: null,
    },

    // 이용률 통계 캐시
    usageCache: {
      data: null,
      filters: null,
      timestamp: null,
      loading: false,
      error: null,
    },

    // 기타 통계 캐시들...
    // 필요에 따라 추가 가능
  }),

  getters: {
    // 매출 데이터 캐시 유효성 확인
    isSalesCacheValid: state => {
      return filters => {
        if (!state.salesCache.data || !state.salesCache.timestamp) {
          return false;
        }

        // 캐시 만료 확인
        const isExpired = Date.now() - state.salesCache.timestamp > CACHE_EXPIRY_TIME;
        if (isExpired) {
          return false;
        }

        // 필터 조건 비교
        return JSON.stringify(state.salesCache.filters) === JSON.stringify(filters);
      };
    },

    // 이용률 데이터 캐시 유효성 확인
    isUsageCacheValid: state => {
      return filters => {
        if (!state.usageCache.data || !state.usageCache.timestamp) {
          return false;
        }

        // 캐시 만료 확인
        const isExpired = Date.now() - state.usageCache.timestamp > CACHE_EXPIRY_TIME;
        if (isExpired) {
          return false;
        }

        // 필터 조건 비교
        return JSON.stringify(state.usageCache.filters) === JSON.stringify(filters);
      };
    },

    // 캐시된 매출 데이터 반환
    getCachedSalesData: state => {
      return state.salesCache.data;
    },

    // 캐시된 이용률 데이터 반환
    getCachedUsageData: state => {
      return state.usageCache.data;
    },
  },

  actions: {
    // 매출 데이터 캐시 저장
    setSalesCache(data, filters) {
      this.salesCache = {
        data: JSON.parse(JSON.stringify(data)), // deep copy
        filters: JSON.parse(JSON.stringify(filters)),
        timestamp: Date.now(),
        loading: false,
        error: null,
      };

      logger.info('매출 데이터 캐시 저장', {
        filters,
        dataKeys: Object.keys(data),
        cacheSize: JSON.stringify(data).length,
      });
    },

    // 이용률 데이터 캐시 저장
    setUsageCache(data, filters) {
      this.usageCache = {
        data: JSON.parse(JSON.stringify(data)), // deep copy
        filters: JSON.parse(JSON.stringify(filters)),
        timestamp: Date.now(),
        loading: false,
        error: null,
      };

      logger.info('이용률 데이터 캐시 저장', {
        filters,
        dataKeys: Object.keys(data),
        cacheSize: JSON.stringify(data).length,
      });
    },

    // 매출 데이터 로딩 상태 설정
    setSalesLoading(loading) {
      this.salesCache.loading = loading;
    },

    // 이용률 데이터 로딩 상태 설정
    setUsageLoading(loading) {
      this.usageCache.loading = loading;
    },

    // 매출 데이터 에러 설정
    setSalesError(error) {
      this.salesCache.error = error;
      this.salesCache.loading = false;
    },

    // 이용률 데이터 에러 설정
    setUsageError(error) {
      this.usageCache.error = error;
      this.usageCache.loading = false;
    },

    // 매출 캐시 무효화
    invalidateSalesCache() {
      logger.info('매출 캐시 무효화');
      this.salesCache = {
        data: null,
        filters: null,
        timestamp: null,
        loading: false,
        error: null,
      };
    },

    // 이용률 캐시 무효화
    invalidateUsageCache() {
      logger.info('이용률 캐시 무효화');
      this.usageCache = {
        data: null,
        filters: null,
        timestamp: null,
        loading: false,
        error: null,
      };
    },

    // 모든 캐시 무효화
    invalidateAllCache() {
      logger.info('모든 분석 캐시 무효화');
      this.invalidateSalesCache();
      this.invalidateUsageCache();
    },

    // 만료된 캐시 정리
    cleanExpiredCache() {
      const now = Date.now();

      // 매출 캐시 확인
      if (this.salesCache.timestamp && now - this.salesCache.timestamp > CACHE_EXPIRY_TIME) {
        logger.info('만료된 매출 캐시 정리');
        this.invalidateSalesCache();
      }

      // 이용률 캐시 확인
      if (this.usageCache.timestamp && now - this.usageCache.timestamp > CACHE_EXPIRY_TIME) {
        logger.info('만료된 이용률 캐시 정리');
        this.invalidateUsageCache();
      }
    },

    // 캐시 상태 정보 반환
    getCacheInfo() {
      return {
        sales: {
          hasData: !!this.salesCache.data,
          age: this.salesCache.timestamp ? Date.now() - this.salesCache.timestamp : null,
          filters: this.salesCache.filters,
          loading: this.salesCache.loading,
          error: this.salesCache.error,
        },
        usage: {
          hasData: !!this.usageCache.data,
          age: this.usageCache.timestamp ? Date.now() - this.usageCache.timestamp : null,
          filters: this.usageCache.filters,
          loading: this.usageCache.loading,
          error: this.usageCache.error,
        },
      };
    },
  },
});
