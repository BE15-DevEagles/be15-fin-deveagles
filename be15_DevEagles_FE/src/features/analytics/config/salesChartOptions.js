/**
 * 매출 분석 차트 옵션
 * 매출 관련 차트 옵션들을 관리
 */
import { formatCurrency } from '../utils/formatters.js';
import {
  createEmptyChartOption,
  createTooltipConfig,
  createAxisConfig,
  createLegendConfig,
  createGridConfig,
  createTitleConfig,
  formatValue,
  sortChartData,
  getChartTitle,
  createPieLegendConfig,
} from './baseChartOptions.js';
import { BrandColors } from './chartThemes.js';

/**
 * 일별 매출 추이 차트 옵션
 * @param {Array} salesData - transformSalesStatisticsData 결과
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} ECharts 옵션
 */
export function createDailySalesChartOption(salesData, isDarkMode = false) {
  if (!Array.isArray(salesData) || salesData.length === 0) {
    return createEmptyChartOption('일별 매출 데이터가 없습니다', isDarkMode);
  }

  return {
    ...createTitleConfig('일별 매출 추이', isDarkMode, { fontSize: 18 }),
    tooltip: {
      ...createTooltipConfig(isDarkMode, {
        axisPointerType: 'cross',
        formatter: function (params) {
          const data = params[0];
          const salesValue = data.value || 0;
          const dataItem = data.data || {};
          const transactions = dataItem.transactions || 0;
          const avgOrder = dataItem.averageOrderValue || 0;

          return `<div style="font-weight: 600; margin-bottom: 8px;">${data.axisValue}</div>
                  <div style="display: flex; align-items: center; margin-bottom: 4px;">
                    <span style="color: ${BrandColors.primary}; font-weight: bold;">매출:</span>
                    <span style="margin-left: 8px; font-weight: 600;">${formatCurrency(salesValue)}</span>
                  </div>
                  <div style="display: flex; align-items: center; margin-bottom: 4px;">
                    <span style="color: ${BrandColors.secondary};">거래건수:</span>
                    <span style="margin-left: 8px;">${transactions}건</span>
                  </div>
                  <div style="display: flex; align-items: center;">
                    <span style="color: ${BrandColors.accent};">평균주문액:</span>
                    <span style="margin-left: 8px;">${formatCurrency(avgOrder)}</span>
                  </div>`;
        },
      }),
    },
    legend: {
      ...createLegendConfig(isDarkMode, {
        data: ['매출', '거래건수'],
        top: 40,
      }),
    },
    grid: createGridConfig(),
    xAxis: {
      ...createAxisConfig(isDarkMode, 'category', {
        data: salesData.map(item => {
          const date = new Date(item.date);
          return `${date.getMonth() + 1}/${date.getDate()}`;
        }),
        showAxisLine: true,
        showAxisTick: false,
      }),
    },
    yAxis: [
      {
        ...createAxisConfig(isDarkMode, 'value', {
          name: '매출 (원)',
          nameTextStyle: {
            color: isDarkMode ? '#D1D5DB' : '#6B7280',
            fontSize: 12,
          },
          axisLabel: {
            formatter: value => formatValue(value, 'compact'),
          },
          showAxisLine: false,
          showAxisTick: false,
        }),
      },
      {
        ...createAxisConfig(isDarkMode, 'value', {
          name: '거래건수',
          nameTextStyle: {
            color: isDarkMode ? '#D1D5DB' : '#6B7280',
            fontSize: 12,
          },
          showAxisLine: false,
          showAxisTick: false,
          splitLine: { show: false },
        }),
      },
    ],
    series: [
      {
        name: '매출',
        type: 'line',
        data: salesData.map(item => ({
          value: item.sales || 0,
          transactions: item.transactions || 0,
          averageOrderValue: item.averageOrderValue || 0,
        })),
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
              { offset: 1, color: BrandColors.primary + '0D' },
            ],
          },
        },
      },
      {
        name: '거래건수',
        type: 'bar',
        yAxisIndex: 1,
        data: salesData.map(item => item.transactions || 0),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: BrandColors.secondary + 'CC' },
              { offset: 1, color: BrandColors.secondary + '66' },
            ],
          },
          borderRadius: [4, 4, 0, 0],
        },
        barWidth: '40%',
      },
    ],
  };
}

/**
 * 고급 매출 통계 차트 옵션 (AdvancedSalesStatisticsResponse 기반)
 * @param {Array} advancedData - transformAdvancedSalesData 결과
 * @param {string} chartType - 'bar', 'line', 'pie' 중 선택
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} ECharts 옵션
 */
export function createAdvancedSalesChartOption(
  advancedData,
  chartType = 'bar',
  isDarkMode = false
) {
  if (!Array.isArray(advancedData) || advancedData.length === 0) {
    return createEmptyChartOption('고급 매출 데이터가 없습니다', isDarkMode);
  }

  const groupBy = advancedData[0]?.groupBy || 'DAY';
  const title = getChartTitle('sales', groupBy);

  if (chartType === 'pie') {
    return createPieChartOption(advancedData, title, isDarkMode);
  }

  // 데이터 정렬
  const sortedData = sortChartData(advancedData, groupBy);

  // X축 레이벨 회전 설정
  const shouldRotateLabels =
    groupBy === 'WEEK' || groupBy === 'MONTH' || groupBy === 'SECONDARY_ITEM';

  return {
    ...createTitleConfig(title, isDarkMode),
    tooltip: {
      ...createTooltipConfig(isDarkMode, {
        axisPointerType: 'shadow',
        formatter: params => {
          const param = params[0];
          const data = param.data || {};
          const salesAmount = data.totalSalesAmount || 0;
          const transactions = data.totalTransactions || 0;
          const avgOrder = data.averageOrderValue || 0;
          const discountRate = data.discountRate || 0;
          const couponRate = data.couponUsageRate || 0;

          return `<div style="font-weight: 600; margin-bottom: 8px;">${param.axisValue}</div>
                  <div style="margin-bottom: 4px;">매출: <strong>${formatCurrency(salesAmount)}</strong></div>
                  <div style="margin-bottom: 4px;">거래건수: <strong>${transactions}건</strong></div>
                  <div style="margin-bottom: 4px;">평균주문액: <strong>${formatCurrency(avgOrder)}</strong></div>
                  <div style="margin-bottom: 4px;">할인률: <strong>${discountRate}%</strong></div>
                  <div>쿠폰사용률: <strong>${couponRate}%</strong></div>`;
        },
      }),
    },
    grid: createGridConfig({
      bottom: shouldRotateLabels ? 80 : 60,
    }),
    xAxis: {
      ...createAxisConfig(isDarkMode, 'category', {
        data: sortedData.map(item => item.displayKey || ''),
        showAxisLine: true,
        showAxisTick: false,
        axisLabel: {
          rotate: shouldRotateLabels ? 45 : 0,
          interval: 0,
        },
      }),
    },
    yAxis: {
      ...createAxisConfig(isDarkMode, 'value', {
        name: '매출 (원)',
        nameTextStyle: {
          color: isDarkMode ? '#D1D5DB' : '#6B7280',
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
        type: chartType,
        data: sortedData.map(item => ({
          value: item.totalSalesAmount || 0,
          totalSalesAmount: item.totalSalesAmount || 0,
          totalTransactions: item.totalTransactions || 0,
          averageOrderValue: item.averageOrderValue || 0,
          discountRate: item.discountRate || 0,
          couponUsageRate: item.couponUsageRate || 0,
        })),
        itemStyle: {
          color:
            chartType === 'line'
              ? BrandColors.primary
              : {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [
                    { offset: 0, color: BrandColors.primary },
                    { offset: 1, color: BrandColors.primary + 'CC' },
                  ],
                },
          borderRadius: chartType === 'bar' ? [4, 4, 0, 0] : 0,
        },
        barWidth: '60%',
        smooth: chartType === 'line',
        lineStyle: chartType === 'line' ? { width: 3 } : undefined,
        areaStyle:
          chartType === 'line'
            ? {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [
                    { offset: 0, color: BrandColors.primary + '33' },
                    { offset: 1, color: BrandColors.primary + '0D' },
                  ],
                },
              }
            : undefined,
      },
    ],
  };
}

/**
 * 할인 효과 분석 차트 옵션
 * @param {Array} discountData - 할인 분석 데이터
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} ECharts 옵션
 */
export function createDiscountAnalysisChartOption(discountData, isDarkMode = false) {
  if (!Array.isArray(discountData) || discountData.length === 0) {
    return createEmptyChartOption('할인 분석 데이터가 없습니다', isDarkMode);
  }

  // 데이터 검증 및 정리
  const validData = discountData.filter(
    item =>
      item &&
      item.displayKey &&
      !isNaN(item.totalSalesAmount) &&
      !isNaN(item.totalDiscountAmount) &&
      !isNaN(item.discountRate)
  );

  if (validData.length === 0) {
    return createEmptyChartOption('유효한 할인 분석 데이터가 없습니다', isDarkMode);
  }

  const groupBy = validData[0]?.groupBy || 'DAY';
  const title = getChartTitle('discount', groupBy);

  // 데이터 정렬
  const sortedData = sortChartData(validData, groupBy);

  // X축 레이벨 회전 설정
  const shouldRotateLabels = groupBy === 'WEEK' || groupBy === 'MONTH';

  return {
    ...createTitleConfig(title, isDarkMode),
    tooltip: {
      ...createTooltipConfig(isDarkMode, {
        axisPointerType: 'cross',
        formatter: params => {
          let result = `<div style="font-weight: 600; margin-bottom: 8px;">${params[0].axisValue}</div>`;
          params.forEach(param => {
            const color = param.color;
            const rawValue = param.value || 0;
            const value = param.seriesName.includes('률')
              ? formatValue(rawValue, 'percent')
              : formatCurrency(rawValue);
            result += `<div style="display: flex; align-items: center; margin-bottom: 4px;">
              <span style="display: inline-block; width: 8px; height: 8px; background: ${color}; border-radius: 50%; margin-right: 8px;"></span>
              <span style="margin-right: 8px;">${param.seriesName}:</span>
              <span style="font-weight: bold;">${value}</span>
            </div>`;
          });
          return result;
        },
      }),
    },
    legend: {
      ...createLegendConfig(isDarkMode, {
        data: ['총 매출', '할인 금액', '쿠폰 할인', '할인률'],
        top: 40,
      }),
    },
    grid: createGridConfig({
      bottom: shouldRotateLabels ? 80 : 60,
    }),
    xAxis: {
      ...createAxisConfig(isDarkMode, 'category', {
        data: sortedData.map(item => item.displayKey || ''),
        showAxisLine: true,
        showAxisTick: false,
        axisLabel: {
          rotate: shouldRotateLabels ? 45 : 0,
          interval: 0,
        },
      }),
    },
    yAxis: [
      {
        ...createAxisConfig(isDarkMode, 'value', {
          name: '금액 (원)',
          nameTextStyle: {
            color: isDarkMode ? '#D1D5DB' : '#6B7280',
            fontSize: 12,
          },
          axisLabel: {
            formatter: value => formatValue(value, 'compact'),
          },
          showAxisLine: false,
          showAxisTick: false,
        }),
      },
      {
        ...createAxisConfig(isDarkMode, 'value', {
          name: '할인률 (%)',
          nameTextStyle: {
            color: isDarkMode ? '#D1D5DB' : '#6B7280',
            fontSize: 12,
          },
          axisLabel: {
            formatter: value => formatValue(value, 'percent'),
          },
          showAxisLine: false,
          showAxisTick: false,
          splitLine: { show: false },
          max: 100,
        }),
      },
    ],
    series: [
      {
        name: '총 매출',
        type: 'bar',
        data: sortedData.map(item => Math.floor(item.totalSalesAmount || 0)),
        itemStyle: {
          color: BrandColors.primary,
          borderRadius: [4, 4, 0, 0],
        },
        barWidth: '20%',
      },
      {
        name: '할인 금액',
        type: 'bar',
        data: sortedData.map(item => Math.floor(item.totalDiscountAmount || 0)),
        itemStyle: {
          color: BrandColors.danger,
          borderRadius: [4, 4, 0, 0],
        },
        barWidth: '20%',
      },
      {
        name: '쿠폰 할인',
        type: 'bar',
        data: sortedData.map(item => Math.floor(item.totalCouponDiscountAmount || 0)),
        itemStyle: {
          color: BrandColors.accent,
          borderRadius: [4, 4, 0, 0],
        },
        barWidth: '20%',
      },
      {
        name: '할인률',
        type: 'line',
        yAxisIndex: 1,
        data: sortedData.map(item => {
          const rate = item.discountRate || 0;
          return Number(rate);
        }),
        lineStyle: {
          color: BrandColors.secondary,
          width: 3,
        },
        itemStyle: {
          color: BrandColors.secondary,
        },
        symbol: 'circle',
        symbolSize: 6,
      },
    ],
  };
}

/**
 * 매출 요약 지표 카드 데이터 생성
 * @param {Object} summaryData - SalesSummaryResponse 데이터
 * @returns {Array} 지표 카드 데이터 배열
 */
export function createSummaryCardsData(summaryData) {
  if (!summaryData) return [];

  const totalSales = summaryData.totalSales || 0;
  const dailyAverage = summaryData.dailyAverage || 0;
  const totalTransactions = summaryData.totalTransactions || 0;
  const averageOrderValue = summaryData.averageOrderValue || 0;

  return [
    {
      icon: '💰',
      label: '총 매출',
      value: formatCurrency(totalSales),
      trend: '',
      trendType: 'neutral',
      variant: 'primary',
    },
    {
      icon: '📊',
      label: '일평균 매출',
      value: formatCurrency(dailyAverage),
      trend: '',
      trendType: 'neutral',
      variant: 'success',
    },
    {
      icon: '🛍️',
      label: '총 거래건수',
      value: `${Math.floor(totalTransactions).toLocaleString()}건`,
      trend: '',
      trendType: 'neutral',
      variant: 'info',
    },
    {
      icon: '💳',
      label: '평균 주문액',
      value: formatCurrency(averageOrderValue),
      trend: '',
      trendType: 'neutral',
      variant: 'warning',
    },
  ];
}

/**
 * 파이 차트 옵션 생성
 * @param {Array} data - 차트 데이터
 * @param {string} title - 차트 제목
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} 파이 차트 옵션
 */
function createPieChartOption(data, title, isDarkMode = false) {
  const totalSales = data.reduce((sum, item) => sum + (item.totalSalesAmount || 0), 0);

  const pieData = data
    .map(item => ({
      name: item.displayKey || '',
      value: item.totalSalesAmount || 0,
      totalSalesAmount: item.totalSalesAmount || 0,
      totalTransactions: item.totalTransactions || 0,
      percentage:
        totalSales > 0 ? (((item.totalSalesAmount || 0) / totalSales) * 100).toFixed(1) : 0,
    }))
    .filter(item => item.value > 0);

  return {
    ...createTitleConfig(title, isDarkMode),
    tooltip: {
      ...createTooltipConfig(isDarkMode, {
        trigger: 'item',
        formatter: params => {
          return `${params.name}: ${formatCurrency(params.value)} (${params.data.percentage}%)`;
        },
      }),
    },
    legend: createPieLegendConfig(pieData, isDarkMode),
    series: [
      {
        type: 'pie',
        radius: '65%',
        center: ['40%', '55%'],
        data: pieData,
        itemStyle: {
          borderRadius: 8,
          borderColor: isDarkMode ? '#1F2937' : '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
        },
        labelLine: {
          show: false,
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  };
}
