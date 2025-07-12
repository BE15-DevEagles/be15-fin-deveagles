/**
 * 추세 분석 차트 옵션
 * 1차 상품별 일별 매출 추세 차트 옵션들을 관리
 */
import {
  createEmptyChartOption,
  createTooltipConfig,
  createLegendConfig,
  createGridConfig,
  createTitleConfig,
  createAxisConfig,
  formatValue,
} from './baseChartOptions.js';
import { BrandColors, LightTheme, DarkTheme } from './chartThemes.js';

/**
 * 1차 상품별 일별 매출추이 Stacked Area 차트 옵션 생성
 * @param {Array} data - 1차 상품별 일별 매출 데이터
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} ECharts Stacked Area 차트 옵션
 */
export function createPrimaryItemDailyTrendChartOption(data, isDarkMode = false) {
  if (!Array.isArray(data) || data.length === 0) {
    return createEmptyChartOption('1차 상품별 일별 매출 데이터가 없습니다.', isDarkMode);
  }

  // 날짜별로 그룹화하고 상위 상품들만 선택
  const dateMap = new Map();
  const itemSalesMap = new Map();

  // 데이터 전처리 - 날짜별, 상품별 매출 집계
  data.forEach(item => {
    const date = item.date;
    const itemName = item.primaryItemName || '미분류';
    const sales = item.totalSalesAmount || 0;

    if (!dateMap.has(date)) {
      dateMap.set(date, new Map());
    }
    dateMap.get(date).set(itemName, sales);

    // 상품별 총 매출 계산
    itemSalesMap.set(itemName, (itemSalesMap.get(itemName) || 0) + sales);
  });

  // 상위 10개 상품 선택 (총 매출 기준)
  const topItems = Array.from(itemSalesMap.entries())
    .sort(([, a], [, b]) => b - a)
    .slice(0, 10)
    .map(([itemName]) => itemName);

  // 날짜 정렬 및 파싱 개선
  const sortedDates = Array.from(dateMap.keys()).sort((a, b) => {
    // 날짜 문자열을 Date 객체로 변환하여 정렬
    const dateA = new Date(a);
    const dateB = new Date(b);
    return dateA - dateB;
  });

  // 다크모드에 따른 색상 팔레트 정의
  const theme = isDarkMode ? DarkTheme : LightTheme;
  const colors = theme.colors.slice(0, 10);

  // 시리즈 데이터 생성
  const series = topItems.map((itemName, index) => {
    const seriesData = sortedDates.map(date => {
      const dateData = dateMap.get(date);
      const value = dateData?.get(itemName) || 0;
      return value;
    });

    return {
      name: itemName,
      type: 'line',
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: colors[index % colors.length] + '80' }, // 50% 투명도
            { offset: 1, color: colors[index % colors.length] + '20' }, // 12% 투명도
          ],
        },
      },
      lineStyle: {
        width: 2,
        color: colors[index % colors.length],
      },
      itemStyle: {
        color: colors[index % colors.length],
      },
      emphasis: {
        focus: 'series',
      },
      data: seriesData,
    };
  });

  const finalChartOption = {
    ...createTitleConfig('1차 상품별 일별 매출추이', isDarkMode, { fontSize: 18 }),
    tooltip: {
      ...createTooltipConfig(isDarkMode, {
        axisPointerType: 'cross',
        formatter: function (params) {
          // 간단한 통화 포맷팅 함수
          const formatCurrency = value => {
            return new Intl.NumberFormat('ko-KR', {
              style: 'currency',
              currency: 'KRW',
              minimumFractionDigits: 0,
              maximumFractionDigits: 0,
            }).format(value);
          };

          let result = `<div style="font-weight: 600; margin-bottom: 8px; color: ${theme.textPrimary};">${params[0].axisValue}</div>`;
          let total = 0;

          params.forEach(param => {
            const value = param.value || 0;
            total += value;
            result += `<div style="display: flex; align-items: center; margin-bottom: 4px;">
                        <span style="display: inline-block; width: 12px; height: 12px; background-color: ${param.color}; border-radius: 2px; margin-right: 8px;"></span>
                        <span style="color: ${theme.textPrimary};">${param.seriesName}:</span>
                        <span style="margin-left: 8px; font-weight: 600; color: ${theme.textPrimary};">${formatCurrency(value)}</span>
                      </div>`;
          });

          result += `<div style="border-top: 1px solid ${theme.border}; margin-top: 8px; padding-top: 8px;">
                      <span style="color: ${theme.textPrimary}; font-weight: 600;">총 매출: ${formatCurrency(total)}</span>
                    </div>`;

          return result;
        },
      }),
    },
    legend: {
      ...createLegendConfig(isDarkMode, {
        data: topItems,
        top: 50,
        left: 'center',
        fontSize: 11,
        pageButtonItemGap: 5,
        pageButtonGap: 8,
        pageIconSize: 10,
      }),
    },
    grid: createGridConfig({
      top: 120,
      left: 60,
      right: 60,
      bottom: 60,
    }),
    xAxis: {
      ...createAxisConfig(isDarkMode, 'category', {
        data: sortedDates.map(date => {
          // 날짜 파싱 개선
          let dateObj;
          if (date && typeof date === 'string') {
            // YYYY-MM-DD 형식 또는 다른 ISO 날짜 형식 처리
            dateObj = new Date(date);
            // 잘못된 날짜인 경우 기본값 사용
            if (isNaN(dateObj.getTime())) {
              dateObj = new Date();
            }
          } else {
            dateObj = new Date();
          }
          return `${dateObj.getMonth() + 1}/${dateObj.getDate()}`;
        }),
        showAxisLine: true,
        showAxisTick: false,
        axisLabel: {
          rotate: sortedDates.length > 20 ? 45 : 0, // 데이터가 많으면 라벨 회전
        },
      }),
    },
    yAxis: {
      ...createAxisConfig(isDarkMode, 'value', {
        name: '매출 (원)',
        nameTextStyle: {
          color: theme.textSecondary,
          fontSize: 12,
        },
        axisLabel: {
          formatter: value => formatValue(value, 'compact'),
        },
        showAxisLine: false,
        showAxisTick: false,
      }),
    },
    series: series,
  };

  return finalChartOption;
}

/**
 * 시계열 매출 추세 차트 옵션 생성
 * @param {Array} data - 시계열 매출 데이터
 * @param {Object} options - 차트 옵션
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} ECharts 라인 차트 옵션
 */
export function createTimeSeriesTrendChartOption(data, options = {}, isDarkMode = false) {
  if (!Array.isArray(data) || data.length === 0) {
    return createEmptyChartOption('시계열 매출 데이터가 없습니다.', isDarkMode);
  }

  const theme = isDarkMode ? DarkTheme : LightTheme;
  const title = options.title || '매출 추세';
  const xAxisKey = options.xAxisKey || 'date';
  const yAxisKey = options.yAxisKey || 'value';

  return {
    ...createTitleConfig(title, isDarkMode),
    tooltip: {
      ...createTooltipConfig(isDarkMode, {
        trigger: 'axis',
        axisPointerType: 'cross',
        formatter: function (params) {
          const param = params[0];
          const value = param.value || 0;
          const date = param.axisValue;

          return `<div style="font-weight: 600; margin-bottom: 8px; color: ${theme.textPrimary};">${date}</div>
                  <div style="color: ${theme.textPrimary};">매출: <strong>${formatValue(value, 'currency')}</strong></div>`;
        },
      }),
    },
    grid: createGridConfig({
      top: 80,
      left: 80,
      right: 60,
      bottom: 60,
    }),
    xAxis: {
      ...createAxisConfig(isDarkMode, 'category', {
        data: data.map(item => {
          const date = item[xAxisKey];
          if (date) {
            const dateObj = new Date(date);
            return `${dateObj.getMonth() + 1}/${dateObj.getDate()}`;
          }
          return '';
        }),
        showAxisLine: true,
        showAxisTick: false,
      }),
    },
    yAxis: {
      ...createAxisConfig(isDarkMode, 'value', {
        name: '매출 (원)',
        nameTextStyle: {
          color: theme.textSecondary,
          fontSize: 12,
        },
        axisLabel: {
          formatter: value => formatValue(value, 'compact'),
        },
        showAxisLine: false,
        showAxisTick: false,
      }),
    },
    series: [
      {
        name: '매출',
        type: 'line',
        data: data.map(item => item[yAxisKey] || 0),
        smooth: true,
        lineStyle: {
          color: BrandColors.primary,
          width: 3,
        },
        itemStyle: {
          color: BrandColors.primary,
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: BrandColors.primary + '50' },
              { offset: 1, color: BrandColors.primary + '10' },
            ],
          },
        },
        emphasis: {
          focus: 'series',
        },
      },
    ],
  };
}

/**
 * 다중 시계열 비교 차트 옵션 생성
 * @param {Array} datasets - 여러 시계열 데이터 배열
 * @param {Object} options - 차트 옵션
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} ECharts 다중 라인 차트 옵션
 */
export function createMultiTimeSeriesChartOption(datasets, options = {}, isDarkMode = false) {
  if (!Array.isArray(datasets) || datasets.length === 0) {
    return createEmptyChartOption('시계열 비교 데이터가 없습니다.', isDarkMode);
  }

  const theme = isDarkMode ? DarkTheme : LightTheme;
  const title = options.title || '매출 추세 비교';
  const xAxisKey = options.xAxisKey || 'date';
  const yAxisKey = options.yAxisKey || 'value';

  // 모든 날짜 수집 및 정렬
  const allDates = new Set();
  datasets.forEach(dataset => {
    dataset.data.forEach(item => allDates.add(item[xAxisKey]));
  });
  const sortedDates = Array.from(allDates).sort((a, b) => new Date(a) - new Date(b));

  // 시리즈 데이터 생성
  const series = datasets.map((dataset, index) => {
    const dataMap = new Map();
    dataset.data.forEach(item => {
      dataMap.set(item[xAxisKey], item[yAxisKey] || 0);
    });

    const seriesData = sortedDates.map(date => dataMap.get(date) || 0);

    return {
      name: dataset.name || `시리즈 ${index + 1}`,
      type: 'line',
      data: seriesData,
      smooth: true,
      lineStyle: {
        color: theme.colors[index % theme.colors.length],
        width: 2,
      },
      itemStyle: {
        color: theme.colors[index % theme.colors.length],
      },
      emphasis: {
        focus: 'series',
      },
    };
  });

  return {
    ...createTitleConfig(title, isDarkMode),
    tooltip: {
      ...createTooltipConfig(isDarkMode, {
        trigger: 'axis',
        axisPointerType: 'cross',
        formatter: function (params) {
          let result = `<div style="font-weight: 600; margin-bottom: 8px; color: ${theme.textPrimary};">${params[0].axisValue}</div>`;

          params.forEach(param => {
            const value = param.value || 0;
            result += `<div style="display: flex; align-items: center; margin-bottom: 4px;">
                        <span style="display: inline-block; width: 12px; height: 12px; background-color: ${param.color}; border-radius: 2px; margin-right: 8px;"></span>
                        <span style="color: ${theme.textPrimary};">${param.seriesName}:</span>
                        <span style="margin-left: 8px; font-weight: 600; color: ${theme.textPrimary};">${formatValue(value, 'currency')}</span>
                      </div>`;
          });

          return result;
        },
      }),
    },
    legend: {
      ...createLegendConfig(isDarkMode, {
        data: datasets.map((dataset, index) => dataset.name || `시리즈 ${index + 1}`),
        top: 40,
        left: 'center',
      }),
    },
    grid: createGridConfig({
      top: 80,
      left: 80,
      right: 60,
      bottom: 60,
    }),
    xAxis: {
      ...createAxisConfig(isDarkMode, 'category', {
        data: sortedDates.map(date => {
          const dateObj = new Date(date);
          return `${dateObj.getMonth() + 1}/${dateObj.getDate()}`;
        }),
        showAxisLine: true,
        showAxisTick: false,
      }),
    },
    yAxis: {
      ...createAxisConfig(isDarkMode, 'value', {
        name: '매출 (원)',
        nameTextStyle: {
          color: theme.textSecondary,
          fontSize: 12,
        },
        axisLabel: {
          formatter: value => formatValue(value, 'compact'),
        },
        showAxisLine: false,
        showAxisTick: false,
      }),
    },
    series: series,
  };
}
