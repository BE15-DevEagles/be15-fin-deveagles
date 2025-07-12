/**
 * 데이터 포맷팅 유틸리티 함수들
 * 통일된 포맷팅 로직 제공
 */

/**
 * 통화 포맷팅 (원화) - 소수점 완전 제거
 * @param {number} value - 포맷팅할 숫자
 * @param {Object} options - 포맷팅 옵션
 * @returns {string} 포맷된 통화 문자열
 */
export function formatCurrency(value, options = {}) {
  const { showUnit = true, shortFormat = false } = options;

  // 잘못된 값 처리
  if (!value || isNaN(value) || value === 0) return '0원';

  // 정수로 변환 (소수점 완전 제거)
  const intValue = Math.floor(Number(value));

  if (shortFormat) {
    if (intValue >= 100000000) {
      return `${Math.floor(intValue / 100000000)}억${showUnit ? '원' : ''}`;
    }
    if (intValue >= 10000) {
      return `${Math.floor(intValue / 10000)}만${showUnit ? '원' : ''}`;
    }
  }

  return `${intValue.toLocaleString('ko-KR')}${showUnit ? '원' : ''}`;
}

/**
 * 퍼센트 포맷팅 - NaN 처리 강화
 * @param {number} value - 포맷팅할 숫자
 * @param {Object} options - 포맷팅 옵션
 * @returns {string} 포맷된 퍼센트 문자열
 */
export function formatPercentage(value, options = {}) {
  const { showSign = true, decimalPlaces = 1 } = options;

  // 잘못된 값 처리
  if (value == null || isNaN(value) || !isFinite(value)) return '0%';

  const numValue = Number(value);
  const sign = showSign && numValue >= 0 ? '+' : '';
  return `${sign}${numValue.toFixed(decimalPlaces)}%`;
}

/**
 * 안전한 나눗셈 - 0으로 나누기 방지
 * @param {number} numerator - 분자
 * @param {number} denominator - 분모
 * @param {number} defaultValue - 기본값
 * @returns {number} 나눗셈 결과
 */
export function safeDivide(numerator, denominator, defaultValue = 0) {
  if (!numerator || !denominator || isNaN(numerator) || isNaN(denominator) || denominator === 0) {
    return defaultValue;
  }

  const result = Number(numerator) / Number(denominator);
  return isFinite(result) ? result : defaultValue;
}

/**
 * 숫자를 축약된 형태로 포맷팅 (차트 축 라벨용)
 * @param {number} value - 포맷팅할 숫자
 * @returns {string} 축약된 숫자 문자열
 */
export function formatChartValue(value) {
  if (!value || isNaN(value)) return '0';

  const intValue = Math.floor(Number(value));

  if (intValue >= 100000000) return `${Math.floor(intValue / 100000000)}억`;
  if (intValue >= 10000) return `${Math.floor(intValue / 10000)}만`;
  if (intValue >= 1000) return `${Math.floor(intValue / 1000)}천`;
  return intValue.toLocaleString('ko-KR');
}

/**
 * 날짜를 차트용으로 포맷팅
 * @param {string|Date} date - 포맷팅할 날짜
 * @param {string} format - 포맷 타입 ('short', 'long')
 * @returns {string} 포맷된 날짜 문자열
 */
export function formatChartDate(date, format = 'short') {
  if (!date) return '';

  const dateObj = typeof date === 'string' ? new Date(date) : date;

  // 잘못된 날짜 처리
  if (isNaN(dateObj.getTime())) return '';

  if (format === 'short') {
    return `${dateObj.getMonth() + 1}/${dateObj.getDate()}`;
  }

  if (format === 'month') {
    return `${dateObj.getMonth() + 1}월`;
  }

  return dateObj.toLocaleDateString('ko-KR');
}

/**
 * 증감률 계산 및 포맷팅 - 안전한 계산
 * @param {number} current - 현재 값
 * @param {number} previous - 이전 값
 * @returns {Object} 증감률 정보
 */
export function calculateGrowthRate(current, previous) {
  // 잘못된 값 처리
  if (!current || isNaN(current)) current = 0;
  if (!previous || isNaN(previous)) previous = 0;

  if (previous === 0) {
    return {
      rate: 0,
      formatted: '0%',
      type: 'neutral',
      isPositive: false,
      isNegative: false,
    };
  }

  const rate = ((current - previous) / previous) * 100;

  // 무한대나 NaN 처리
  if (!isFinite(rate)) {
    return {
      rate: 0,
      formatted: '0%',
      type: 'neutral',
      isPositive: false,
      isNegative: false,
    };
  }

  const isPositive = rate > 0;
  const isNegative = rate < 0;

  return {
    rate,
    formatted: formatPercentage(rate),
    type: isPositive ? 'positive' : isNegative ? 'negative' : 'neutral',
    isPositive,
    isNegative,
  };
}

/**
 * 컬렉션에서 평균값 계산
 * @param {Array} data - 데이터 배열
 * @param {string} key - 평균을 구할 키 (optional)
 * @returns {number} 평균값
 */
export function calculateAverage(data, key = null) {
  if (!data || !Array.isArray(data) || data.length === 0) return 0;

  const values = key
    ? data.map(item => Number(item[key]) || 0)
    : data.map(item => Number(item) || 0);
  const validValues = values.filter(val => !isNaN(val) && isFinite(val));

  if (validValues.length === 0) return 0;

  const sum = validValues.reduce((acc, val) => acc + val, 0);
  return sum / validValues.length;
}

/**
 * 컬렉션에서 총합 계산 - 안전한 계산
 * @param {Array} data - 데이터 배열
 * @param {string} key - 합계를 구할 키 (optional)
 * @returns {number} 총합
 */
export function calculateTotal(data, key = null) {
  if (!data || !Array.isArray(data) || data.length === 0) return 0;

  const values = key
    ? data.map(item => Number(item[key]) || 0)
    : data.map(item => Number(item) || 0);
  const validValues = values.filter(val => !isNaN(val) && isFinite(val));

  return validValues.reduce((acc, val) => acc + val, 0);
}
