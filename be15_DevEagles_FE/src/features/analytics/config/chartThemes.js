/**
 * 차트 테마
 */

// 브랜드 컬러 팔레트
export const BrandColors = {
  primary: '#3B82F6', // 블루 (주요 데이터)
  secondary: '#10B981', // 그린 (성장/긍정)
  accent: '#F59E0B', // 앰버 (주의/강조)
  danger: '#EF4444', // 레드 (위험/감소)
  warning: '#F97316', // 오렌지 (경고)
  info: '#06B6D4', // 사이안 (정보)
  success: '#22C55E', // 라임 (성공)
  purple: '#8B5CF6', // 보라 (프리미엄)
  pink: '#EC4899', // 핑크 (여성/뷰티)
  gray: '#6B7280', // 회색 (중립)
};

// 차트 카테고리별 컬러 스키마
export const CategoryColors = {
  sales: [BrandColors.primary, BrandColors.secondary, BrandColors.accent],
  customer: [BrandColors.pink, BrandColors.purple, BrandColors.info],
  staff: [BrandColors.success, BrandColors.warning, BrandColors.danger],
  operational: [BrandColors.info, BrandColors.gray, BrandColors.accent],
  financial: [BrandColors.primary, BrandColors.success, BrandColors.danger],
};

// 라이트 테마 정의
export const LightTheme = {
  name: 'light',

  // 기본 색상
  background: '#ffffff',
  surface: '#f8fafc',
  textPrimary: '#1f2937',
  textSecondary: '#6b7280',
  textTertiary: '#9ca3af',
  border: '#e5e7eb',

  // 그리드 및 축
  gridColor: '#f3f4f6',
  axisColor: '#d1d5db',
  splitLineColor: '#e5e7eb',

  // 차트 컬러
  colors: [
    BrandColors.primary,
    BrandColors.secondary,
    BrandColors.accent,
    BrandColors.purple,
    BrandColors.pink,
    BrandColors.info,
    BrandColors.success,
    BrandColors.warning,
  ],

  // 그라데이션
  gradients: {
    primary: ['#3B82F6', '#1D4ED8'],
    secondary: ['#10B981', '#059669'],
    accent: ['#F59E0B', '#D97706'],
  },

  // 투명도 레벨
  opacity: {
    light: 0.1,
    medium: 0.3,
    high: 0.7,
  },
};

// 다크 테마 정의
export const DarkTheme = {
  name: 'dark',

  // 기본 색상
  background: '#1f2937',
  surface: '#374151',
  textPrimary: '#f9fafb',
  textSecondary: '#d1d5db',
  textTertiary: '#9ca3af',
  border: '#4b5563',

  // 그리드 및 축
  gridColor: '#374151',
  axisColor: '#6b7280',
  splitLineColor: '#4b5563',

  // 차트 컬러 (다크 모드에 맞게 조정)
  colors: [
    '#60A5FA', // 밝은 블루
    '#34D399', // 밝은 그린
    '#FBBF24', // 밝은 앰버
    '#A78BFA', // 밝은 보라
    '#F472B6', // 밝은 핑크
    '#22D3EE', // 밝은 사이안
    '#4ADE80', // 밝은 라임
    '#FB923C', // 밝은 오렌지
  ],

  // 그라데이션
  gradients: {
    primary: ['#60A5FA', '#3B82F6'],
    secondary: ['#34D399', '#10B981'],
    accent: ['#FBBF24', '#F59E0B'],
  },

  // 투명도 레벨
  opacity: {
    light: 0.15,
    medium: 0.4,
    high: 0.8,
  },
};

/**
 * 차트 타입별 테마 커스터마이징
 */
export const ChartTypeThemes = {
  line: {
    lineWidth: 3,
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    areaStyle: {
      opacity: 0.1,
    },
  },

  bar: {
    barWidth: '60%',
    itemStyle: {
      borderRadius: [4, 4, 0, 0],
    },
  },

  pie: {
    radius: ['35%', '70%'],
    center: ['50%', '50%'],
    roseType: false,
    itemStyle: {
      borderRadius: 4,
      borderColor: '#fff',
      borderWidth: 2,
    },
  },

  gauge: {
    radius: '75%',
    center: ['50%', '60%'],
    startAngle: 200,
    endAngle: -40,
    splitNumber: 10,
  },
};

/**
 * 비즈니스 맥락에 맞는 색상 매핑
 */
export const BusinessColorMap = {
  // 매출 관련
  revenue: BrandColors.primary,
  profit: BrandColors.secondary,
  loss: BrandColors.danger,

  // 성과 관련
  target: BrandColors.gray,
  achievement: BrandColors.success,
  overAchievement: BrandColors.purple,
  underAchievement: BrandColors.warning,

  // 고객 관련
  newCustomer: BrandColors.info,
  returningCustomer: BrandColors.primary,
  vipCustomer: BrandColors.purple,

  // 직원 관련
  topPerformer: BrandColors.success,
  averagePerformer: BrandColors.accent,
  lowPerformer: BrandColors.warning,

  // 서비스 관련
  premium: BrandColors.purple,
  standard: BrandColors.primary,
  basic: BrandColors.gray,

  // 할인 관련
  discount: BrandColors.danger,
  coupon: BrandColors.accent,
  promotion: BrandColors.pink,
};

/**
 * 테마 팩토리 함수
 */
export class ChartThemeFactory {
  static createTheme(themeName, customizations = {}) {
    const baseTheme = themeName === 'dark' ? DarkTheme : LightTheme;

    return {
      ...baseTheme,
      ...customizations,
      colors: customizations.colors || baseTheme.colors,
    };
  }

  static getColorByCategory(category, index = 0) {
    const categoryColors = CategoryColors[category] || CategoryColors.sales;
    return categoryColors[index % categoryColors.length];
  }

  static getBusinessColor(context) {
    return BusinessColorMap[context] || BrandColors.primary;
  }

  static createGradient(color1, color2, direction = 'vertical') {
    return {
      type: 'linear',
      x: direction === 'horizontal' ? 0 : 0,
      y: direction === 'horizontal' ? 0 : 0,
      x2: direction === 'horizontal' ? 1 : 0,
      y2: direction === 'horizontal' ? 0 : 1,
      colorStops: [
        { offset: 0, color: color1 },
        { offset: 1, color: color2 },
      ],
    };
  }
}

/**
 * 반응형 차트 설정
 */
export const ResponsiveConfig = {
  breakpoints: {
    mobile: 480,
    tablet: 768,
    desktop: 1024,
    large: 1280,
  },

  chartSizes: {
    mobile: { width: '100%', height: 200 },
    tablet: { width: '100%', height: 300 },
    desktop: { width: '100%', height: 400 },
    large: { width: '100%', height: 500 },
  },

  fontSize: {
    mobile: 10,
    tablet: 12,
    desktop: 14,
    large: 16,
  },
};
