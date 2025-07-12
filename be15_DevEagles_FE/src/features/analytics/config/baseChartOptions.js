/**
 * 베이스 차트 옵션 팩토리
 * 모든 차트 옵션 생성의 공통 기능 제공
 */
import { BrandColors, ChartThemeFactory, LightTheme, DarkTheme } from './chartThemes.js';

/**
 * 빈 차트 옵션 생성 (공통)
 * @param {string} message - 표시할 메시지
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} 빈 차트 옵션
 */
export function createEmptyChartOption(message, isDarkMode = false) {
  const theme = isDarkMode ? DarkTheme : LightTheme;

  return {
    title: {
      text: message,
      left: 'center',
      top: 'middle',
      textStyle: {
        color: theme.textTertiary,
        fontSize: 14,
      },
    },
    backgroundColor: 'transparent',
  };
}

/**
 * 공통 툴팁 스타일 생성
 * @param {boolean} isDarkMode - 다크모드 여부
 * @param {Object} customOptions - 커스텀 옵션
 * @returns {Object} 툴팁 설정
 */
export function createTooltipConfig(isDarkMode = false, customOptions = {}) {
  const theme = isDarkMode ? DarkTheme : LightTheme;

  return {
    trigger: customOptions.trigger || 'axis',
    backgroundColor: customOptions.backgroundColor || theme.background,
    borderWidth: customOptions.borderWidth || 1,
    borderColor: customOptions.borderColor || theme.border,
    borderRadius: customOptions.borderRadius || 12,
    textStyle: {
      color: customOptions.textColor || theme.textPrimary,
      fontSize: customOptions.fontSize || 12,
    },
    padding: customOptions.padding || [12, 16],
    extraCssText:
      customOptions.extraCssText ||
      'box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1); backdrop-filter: blur(10px);',
    axisPointer: {
      type: customOptions.axisPointerType || 'shadow',
      shadowStyle: {
        color: isDarkMode ? 'rgba(156, 163, 175, 0.3)' : 'rgba(156, 163, 175, 0.2)',
      },
    },
    ...customOptions.extra,
  };
}

/**
 * 공통 축 설정 생성
 * @param {boolean} isDarkMode - 다크모드 여부
 * @param {string} type - 'category' 또는 'value'
 * @param {Object} options - 추가 옵션
 * @returns {Object} 축 설정
 */
export function createAxisConfig(isDarkMode = false, type = 'category', options = {}) {
  const theme = isDarkMode ? DarkTheme : LightTheme;

  const baseConfig = {
    type,
    axisLine: {
      show: options.showAxisLine !== false,
      lineStyle: {
        color: theme.axisColor,
      },
    },
    axisTick: {
      show: options.showAxisTick !== false,
      lineStyle: {
        color: theme.axisColor,
      },
    },
    axisLabel: {
      color: theme.textSecondary,
      fontSize: 11,
      ...options.axisLabel,
    },
    splitLine: {
      lineStyle: {
        color: theme.splitLineColor,
        type: 'dashed',
        opacity: 0.5,
      },
      ...options.splitLine,
    },
  };

  // 추가 설정 병합
  if (options.name) baseConfig.name = options.name;
  if (options.data) baseConfig.data = options.data;
  if (options.max) baseConfig.max = options.max;
  if (options.nameTextStyle) baseConfig.nameTextStyle = options.nameTextStyle;

  return baseConfig;
}

/**
 * 공통 범례 설정 생성
 * @param {boolean} isDarkMode - 다크모드 여부
 * @param {Object} options - 추가 옵션
 * @returns {Object} 범례 설정
 */
export function createLegendConfig(isDarkMode = false, options = {}) {
  const theme = isDarkMode ? DarkTheme : LightTheme;

  return {
    top: options.top || 20,
    left: options.left || 'center',
    itemGap: options.itemGap || 20,
    textStyle: {
      fontSize: options.fontSize || 12,
      fontWeight: '500',
      color: theme.textPrimary,
    },
    itemWidth: options.itemWidth || 12,
    itemHeight: options.itemHeight || 12,
    ...options.extra,
  };
}

/**
 * 파이 차트용 공통 범례 설정 생성
 * @param {Array<Object>} seriesData - 차트 시리즈 데이터. e.g. [{name: 'A', value: 10}]
 * @param {boolean} isDarkMode - 다크모드 여부
 * @param {Object} options - 추가 옵션
 * @returns {Object} 범례 설정
 */
export function createPieLegendConfig(seriesData, isDarkMode = false, options = {}) {
  const theme = isDarkMode ? DarkTheme : LightTheme;
  const total = seriesData.reduce((sum, item) => sum + (item.value || 0), 0);

  return {
    orient: 'vertical',
    top: 'center',
    left: '70%',
    itemGap: options.itemGap || 15,
    itemWidth: options.itemWidth || 12,
    itemHeight: options.itemHeight || 12,
    formatter: name => {
      if (!seriesData || seriesData.length === 0) return name;
      const item = seriesData.find(d => d.name === name);
      if (!item || typeof item.value !== 'number') {
        return name;
      }
      const percentage = total > 0 ? ((item.value / total) * 100).toFixed(1) : '0.0';
      return `{name|${name}}{value|${percentage}%}`;
    },
    textStyle: {
      color: theme.textPrimary, // Default color
      rich: {
        name: {
          fontSize: 12,
          fontWeight: '500',
          width: 70,
          color: theme.textSecondary,
        },
        value: {
          fontSize: 12,
          fontWeight: 'bold',
          align: 'right',
          width: 40,
          color: theme.textPrimary,
        },
      },
    },
    ...options.extra,
  };
}

/**
 * 공통 그리드 설정 생성
 * @param {Object} options - 그리드 옵션
 * @returns {Object} 그리드 설정
 */
export function createGridConfig(options = {}) {
  return {
    top: options.top || 80,
    left: options.left || 60,
    right: options.right || 50,
    bottom: options.bottom || 60,
    containLabel: true,
    ...options.extra,
  };
}

/**
 * 공통 시리즈 아이템 스타일 생성
 * @param {string} color - 기본 색상
 * @param {Object} options - 추가 옵션
 * @returns {Object} 아이템 스타일
 */
export function createItemStyle(color, options = {}) {
  const baseStyle = {
    color: options.gradient
      ? ChartThemeFactory.createGradient(
          options.gradient.color1 || color,
          options.gradient.color2 || color,
          options.gradient.direction
        )
      : color,
  };

  if (options.borderRadius) {
    baseStyle.borderRadius = options.borderRadius;
  }

  if (options.borderColor) {
    baseStyle.borderColor = options.borderColor;
    baseStyle.borderWidth = options.borderWidth || 1;
  }

  return baseStyle;
}

/**
 * 공통 차트 제목 설정 생성
 * @param {string} title - 제목
 * @param {boolean} isDarkMode - 다크모드 여부
 * @param {Object} options - 추가 옵션
 * @returns {Object} 제목 설정
 */
export function createTitleConfig(title, isDarkMode = false, options = {}) {
  const theme = isDarkMode ? DarkTheme : LightTheme;

  return {
    text: title,
    textStyle: {
      fontSize: options.fontSize || 16,
      fontWeight: options.fontWeight || 'bold',
      color: theme.textPrimary,
    },
    left: options.left || 'center',
    top: options.top || 10,
    ...options.extra,
  };
}

/**
 * 값 포맷터 함수
 * @param {number} value - 값
 * @param {string} type - 'currency', 'number', 'percent'
 * @returns {string} 포맷된 값
 */
export function formatValue(value, type = 'number') {
  if (value === null || value === undefined) return '0';

  switch (type) {
    case 'currency':
      return new Intl.NumberFormat('ko-KR', {
        style: 'currency',
        currency: 'KRW',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(value);

    case 'percent':
      return `${Number(value).toFixed(1)}%`;

    case 'compact':
      if (value >= 1000000) return `${Math.floor(value / 1000000)}M`;
      if (value >= 1000) return `${Math.floor(value / 1000)}K`;
      return Math.floor(value).toLocaleString();

    default:
      return Math.floor(value).toLocaleString();
  }
}

/**
 * 성별 구분 색상 상수
 */
export const GENDER_COLORS = {
  male: '#4F46E5', // 남성 - 인디고
  female: '#EC4899', // 여성 - 핑크
};

/**
 * 로딩 스피너 설정 생성
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} 로딩 옵션
 */
export function createLoadingOptions(isDarkMode = false) {
  const theme = isDarkMode ? DarkTheme : LightTheme;

  return {
    text: '로딩 중...',
    color: BrandColors.primary,
    textColor: theme.textPrimary,
    maskColor: isDarkMode ? 'rgba(31, 41, 55, 0.8)' : 'rgba(255, 255, 255, 0.8)',
    zlevel: 0,
  };
}

/**
 * 데이터 정렬 함수
 * @param {Array} data - 정렬할 데이터
 * @param {string} groupBy - 그룹화 기준
 * @returns {Array} 정렬된 데이터
 */
export function sortChartData(data, groupBy) {
  if (!Array.isArray(data)) return [];

  return [...data].sort((a, b) => {
    if (groupBy === 'WEEK' || groupBy === 'MONTH') {
      // 시간 기반 데이터: 날짜로 정렬
      const aDate = a.date || '1';
      const bDate = b.date || '1';

      // YYYY-MM 또는 YYYY-WW 형태인 경우
      if (/^\d{4}-\d{2}$/.test(aDate) && /^\d{4}-\d{2}$/.test(bDate)) {
        return aDate.localeCompare(bDate);
      }

      // 숫자 형태인 경우 (주차/월 번호) - 하위 호환성
      if (/^\d{1,2}$/.test(aDate) && /^\d{1,2}$/.test(bDate)) {
        return parseInt(aDate) - parseInt(bDate);
      }

      // 날짜 형태인 경우 - 하위 호환성
      return new Date(aDate) - new Date(bDate);
    } else {
      // 기타 그룹화: 매출액 순으로 정렬
      return (b.totalSalesAmount || 0) - (a.totalSalesAmount || 0);
    }
  });
}

/**
 * 차트 제목 매핑
 */
export const CHART_TITLES = {
  sales: {
    WEEK: '주별 매출 분석',
    MONTH: '월별 매출 분석',
    GENDER: '성별 매출 분석',
    CATEGORY: '카테고리별 매출 분석',
    PRIMARY_ITEM: '1차 상품별 매출 분석',
    SECONDARY_ITEM: '2차 상품별 매출 분석',
    DEFAULT: '매출 분석',
  },
  discount: {
    WEEK: '주별 할인 효과 분석',
    MONTH: '월별 할인 효과 분석',
    DEFAULT: '할인 효과 분석',
  },
};

/**
 * 차트 제목 생성
 * @param {string} category - 카테고리 ('sales', 'discount')
 * @param {string} groupBy - 그룹화 기준
 * @returns {string} 차트 제목
 */
export function getChartTitle(category, groupBy) {
  const titles = CHART_TITLES[category];
  return titles ? titles[groupBy] || titles.DEFAULT : '';
}
