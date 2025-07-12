/**
 * Analytics Config 통합 Export
 */

// 테마 및 기본 설정
export * from './chartThemes.js';
export * from './baseChartOptions.js';

// 차트 옵션들
export * from './salesChartOptions.js';
export * from './itemChartOptions.js';
export * from './trendChartOptions.js';
export * from './usageChartOptions.js';

// 편의성을 위한 기본 exports
export { BrandColors, LightTheme, DarkTheme, ChartThemeFactory } from './chartThemes.js';
export {
  createEmptyChartOption,
  createTooltipConfig,
  createAxisConfig,
  createLegendConfig,
  createGridConfig,
  createTitleConfig,
  formatValue,
  GENDER_COLORS,
} from './baseChartOptions.js';
