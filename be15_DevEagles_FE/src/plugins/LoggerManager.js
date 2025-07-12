/**
 * 싱글톤 패턴 기반 로거 매니저
 * 모듈별 로거 인스턴스를 캐싱하여 재사용
 */

import { Logger } from './logger.js';

class LoggerManager {
  constructor() {
    if (LoggerManager.instance) {
      return LoggerManager.instance;
    }

    this.loggerCache = new Map();
    this.defaultLogger = new Logger();

    // API 전용 로거들
    this.apiLoggers = new Map();

    LoggerManager.instance = this;
  }

  /**
   * 모듈별 로거 인스턴스 반환 (캐싱)
   * @param {string} moduleName - 모듈명
   * @returns {Logger} 로거 인스턴스
   */
  getLogger(moduleName) {
    if (!moduleName) {
      return this.defaultLogger;
    }

    if (!this.loggerCache.has(moduleName)) {
      this.loggerCache.set(moduleName, new Logger(` [${moduleName}]`));
    }

    return this.loggerCache.get(moduleName);
  }

  /**
   * API 전용 로거 반환 (특별한 포맷팅)
   * @param {string} apiName - API명 (예: 'CouponsAPI', 'AuthAPI')
   * @returns {Logger} API 로거 인스턴스
   */
  getApiLogger(apiName) {
    const loggerKey = `API_${apiName}`;

    if (!this.apiLoggers.has(loggerKey)) {
      const apiLogger = new Logger(` [🌐 ${apiName}]`);

      // API 전용 메서드 추가
      apiLogger.request = (method, url, data) => {
        apiLogger.info(`→ ${method.toUpperCase()} ${url}`, data ? { data } : '');
      };

      apiLogger.response = (method, url, status, data) => {
        const statusEmoji = status >= 400 ? '❌' : '✅';
        apiLogger.info(`← ${statusEmoji} ${method.toUpperCase()} ${url} [${status}]`, data);
      };

      apiLogger.error = (method, url, error) => {
        // 에러 객체를 직접 전달하지 않고 필요한 정보만 추출
        const errorInfo = {
          message: error?.message,
          status: error?.response?.status,
          data: error?.response?.data,
          stack: error?.stack,
        };
        apiLogger.info(`💥 ${method.toUpperCase()} ${url}`, errorInfo);
      };

      this.apiLoggers.set(loggerKey, apiLogger);
    }

    return this.apiLoggers.get(loggerKey);
  }

  /**
   * 전역 에러 로거
   */
  getErrorLogger() {
    if (!this.errorLogger) {
      this.errorLogger = new Logger(' [💥 ERROR]');

      // 에러 전용 메서드 추가
      this.errorLogger.apiError = (context, error, additional = {}) => {
        // 에러 객체를 직접 전달하지 않고 필요한 정보만 추출
        const errorInfo = {
          message: error?.message,
          status: error?.response?.status,
          data: error?.response?.data,
          stack: error?.stack,
          ...additional,
        };
        this.errorLogger.info(`API 에러 [${context}]`, errorInfo);
      };

      this.errorLogger.validationError = (field, message, value = null) => {
        this.errorLogger.warn(`유효성 검사 실패 [${field}]`, { message, value });
      };
    }

    return this.errorLogger;
  }

  /**
   * 성능 측정 로거
   */
  getPerformanceLogger() {
    if (!this.performanceLogger) {
      this.performanceLogger = new Logger(' [⚡ PERF]');

      // 성능 측정 전용 메서드
      this.performanceLogger.measure = (operation, duration) => {
        const emoji = duration > 1000 ? '🐌' : duration > 500 ? '🚶' : '🏃';
        this.performanceLogger.info(`${emoji} ${operation}: ${duration}ms`);
      };
    }

    return this.performanceLogger;
  }

  /**
   * 모든 캐시된 로거 정보 반환
   */
  getCacheInfo() {
    return {
      moduleLoggers: Array.from(this.loggerCache.keys()),
      apiLoggers: Array.from(this.apiLoggers.keys()),
      cacheSize: this.loggerCache.size + this.apiLoggers.size,
    };
  }

  /**
   * 특정 모듈의 로거 캐시 제거
   */
  clearModuleLogger(moduleName) {
    return this.loggerCache.delete(moduleName);
  }

  /**
   * 모든 로거 캐시 제거
   */
  clearAllCache() {
    this.loggerCache.clear();
    this.apiLoggers.clear();
    this.errorLogger = null;
    this.performanceLogger = null;
  }
}

// 싱글톤 인스턴스 생성
export const loggerManager = new LoggerManager();

// 편의 함수들
export function getLogger(moduleName) {
  return loggerManager.getLogger(moduleName);
}

export function getApiLogger(apiName) {
  return loggerManager.getApiLogger(apiName);
}

export function getErrorLogger() {
  return loggerManager.getErrorLogger();
}

export function getPerformanceLogger() {
  return loggerManager.getPerformanceLogger();
}

// 도메인별 특화 로거들
export function getViewLogger(componentName) {
  return loggerManager.getLogger(`VIEW_${componentName}`);
}

export function getComposableLogger(composableName) {
  return loggerManager.getLogger(`COMPOSABLE_${composableName}`);
}

export function getStoreLogger(storeName) {
  return loggerManager.getLogger(`STORE_${storeName}`);
}

export function getServiceLogger(serviceName) {
  return loggerManager.getLogger(`SERVICE_${serviceName}`);
}

export default LoggerManager;
