/**
 * 아이템 분석 차트 옵션
 * 1차/2차 아이템 점유율, 판매건수 차트 옵션들을 관리
 */
import { formatCurrency } from '../utils/formatters.js';
import {
  createEmptyChartOption,
  createTooltipConfig,
  createTitleConfig,
  createPieLegendConfig,
} from './baseChartOptions.js';

/**
 * 1차 아이템 판매 점유율 차트 옵션 생성
 * @param {Array} data - 1차 아이템 매출 데이터
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} ECharts 파이차트 옵션
 */
export function createPrimaryItemShareChartOption(data, isDarkMode = false) {
  if (!Array.isArray(data) || data.length === 0) {
    return createEmptyChartOption('1차 아이템 데이터가 없습니다.', isDarkMode);
  }

  // 데이터 정렬 및 상위 10개만 선택
  const filteredData = [...data].filter(item => item && typeof item === 'object');
  const sortedData = filteredData
    .sort((a, b) => (b.totalSalesAmount || 0) - (a.totalSalesAmount || 0))
    .slice(0, 10);

  if (sortedData.length === 0) {
    return createEmptyChartOption('1차 아이템이 없습니다.', isDarkMode);
  }

  // 총 매출액 계산
  const totalSales = sortedData.reduce((sum, item) => sum + (item.totalSalesAmount || 0), 0);

  // 파이 차트 데이터 생성
  const pieData = sortedData.map(item => ({
    name: item.displayKey || item.primaryItemName || '미분류',
    value: item.totalSalesAmount || 0,
    totalSalesAmount: item.totalSalesAmount || 0,
    totalTransactions: item.totalTransactions || 0,
    percentage: totalSales > 0 ? (((item.totalSalesAmount || 0) / totalSales) * 100).toFixed(1) : 0,
  }));

  return {
    ...createTitleConfig('1차 아이템 판매 점유율', isDarkMode),
    tooltip: {
      ...createTooltipConfig(isDarkMode, {
        trigger: 'item',
        formatter: params => {
          const data = params.data || {};
          const salesAmount = data.totalSalesAmount || 0;
          const transactions = data.totalTransactions || 0;
          const percentage = data.percentage || 0;

          return `<div style="font-weight: 600; margin-bottom: 8px;">${params.name}</div>
                  <div>매출: ${formatCurrency(salesAmount)}</div>
                  <div>점유율: ${percentage}%</div>
                  <div>거래건수: ${transactions}건</div>`;
        },
      }),
    },
    legend: createPieLegendConfig(pieData, isDarkMode, { right: '10%' }),
    series: [
      {
        type: 'pie',
        radius: ['30%', '70%'],
        center: ['40%', '50%'],
        data: pieData,
        itemStyle: {
          borderRadius: 6,
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
            shadowColor: 'rgba(0, 0, 0, 0.3)',
          },
        },
      },
    ],
  };
}

/**
 * 2차 아이템 판매 점유율 차트 옵션 생성
 * @param {Array} data - 2차 아이템 매출 데이터
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} ECharts 파이차트 옵션
 */
export function createSecondaryItemShareChartOption(data, isDarkMode = false) {
  if (!Array.isArray(data) || data.length === 0) {
    return createEmptyChartOption('2차 아이템 데이터가 없습니다.', isDarkMode);
  }

  // 데이터 정렬 및 상위 15개만 선택
  const filteredData = [...data].filter(item => item && typeof item === 'object');
  const sortedData = filteredData
    .sort((a, b) => (b.totalSalesAmount || 0) - (a.totalSalesAmount || 0))
    .slice(0, 15);

  if (sortedData.length === 0) {
    return createEmptyChartOption('2차 아이템이 없습니다.', isDarkMode);
  }

  // 총 매출액 계산
  const totalSales = sortedData.reduce((sum, item) => sum + (item.totalSalesAmount || 0), 0);

  // 파이 차트 데이터 생성
  const pieData = sortedData.map(item => ({
    name: item.displayKey || item.secondaryItemName || '미분류',
    value: item.totalSalesAmount || 0,
    totalSalesAmount: item.totalSalesAmount || 0,
    totalTransactions: item.totalTransactions || 0,
    percentage: totalSales > 0 ? (((item.totalSalesAmount || 0) / totalSales) * 100).toFixed(1) : 0,
  }));

  return {
    ...createTitleConfig('2차 아이템 판매 점유율', isDarkMode),
    tooltip: {
      ...createTooltipConfig(isDarkMode, {
        trigger: 'item',
        formatter: params => {
          const data = params.data || {};
          const salesAmount = data.totalSalesAmount || 0;
          const transactions = data.totalTransactions || 0;
          const percentage = data.percentage || 0;

          return `<div style="font-weight: 600; margin-bottom: 8px;">${params.name}</div>
                  <div>매출: ${formatCurrency(salesAmount)}</div>
                  <div>점유율: ${percentage}%</div>
                  <div>거래건수: ${transactions}건</div>`;
        },
      }),
    },
    legend: createPieLegendConfig(pieData, isDarkMode, { right: '5%' }),
    series: [
      {
        type: 'pie',
        radius: ['25%', '65%'],
        center: ['40%', '50%'],
        data: pieData,
        itemStyle: {
          borderRadius: 4,
          borderColor: isDarkMode ? '#1F2937' : '#fff',
          borderWidth: 1,
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
            shadowColor: 'rgba(0, 0, 0, 0.3)',
          },
        },
      },
    ],
  };
}

/**
 * 1차 아이템 판매건수 점유율 차트 옵션 생성
 * @param {Array} data - 1차 아이템 매출 데이터
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} ECharts 파이차트 옵션
 */
export function createPrimaryItemTransactionShareChartOption(data, isDarkMode = false) {
  if (!Array.isArray(data) || data.length === 0) {
    return createEmptyChartOption('1차 아이템 판매건수 데이터가 없습니다.', isDarkMode);
  }

  // 데이터 정렬 및 상위 10개만 선택 (판매건수 기준)
  const filteredData = [...data].filter(item => item && typeof item === 'object');
  const sortedData = filteredData
    .sort((a, b) => (b.totalTransactions || 0) - (a.totalTransactions || 0))
    .slice(0, 10);

  if (sortedData.length === 0) {
    return createEmptyChartOption('1차 아이템 판매건수가 없습니다.', isDarkMode);
  }

  // 총 판매건수 계산
  const totalTransactions = sortedData.reduce(
    (sum, item) => sum + (item.totalTransactions || 0),
    0
  );

  // 파이 차트 데이터 생성
  const pieData = sortedData.map(item => ({
    name: item.displayKey || item.primaryItemName || '미분류',
    value: item.totalTransactions || 0,
    totalSalesAmount: item.totalSalesAmount || 0,
    totalTransactions: item.totalTransactions || 0,
    percentage:
      totalTransactions > 0
        ? (((item.totalTransactions || 0) / totalTransactions) * 100).toFixed(1)
        : 0,
  }));

  return {
    ...createTitleConfig('1차 아이템 판매건수 점유율', isDarkMode),
    tooltip: {
      ...createTooltipConfig(isDarkMode, {
        trigger: 'item',
        formatter: params => {
          const data = params.data || {};
          const salesAmount = data.totalSalesAmount || 0;
          const transactions = data.totalTransactions || 0;
          const percentage = data.percentage || 0;

          return `<div style="font-weight: 600; margin-bottom: 8px;">${params.name}</div>
                  <div>판매건수: ${transactions}건</div>
                  <div>점유율: ${percentage}%</div>
                  <div>매출: ${formatCurrency(salesAmount)}</div>`;
        },
      }),
    },
    legend: createPieLegendConfig(pieData, isDarkMode),
    series: [
      {
        type: 'pie',
        radius: ['30%', '70%'],
        center: ['40%', '50%'],
        data: pieData,
        itemStyle: {
          borderRadius: 6,
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
            shadowColor: 'rgba(0, 0, 0, 0.3)',
          },
        },
      },
    ],
  };
}

/**
 * 2차 아이템 판매건수 점유율 차트 옵션 생성
 * @param {Array} data - 2차 아이템 매출 데이터
 * @param {boolean} isDarkMode - 다크모드 여부
 * @returns {Object} ECharts 파이차트 옵션
 */
export function createSecondaryItemTransactionShareChartOption(data, isDarkMode = false) {
  if (!Array.isArray(data) || data.length === 0) {
    return createEmptyChartOption('2차 아이템 판매건수 데이터가 없습니다.', isDarkMode);
  }

  // 데이터 정렬 및 상위 15개만 선택 (판매건수 기준)
  const filteredData = [...data].filter(item => item && typeof item === 'object');
  const sortedData = filteredData
    .sort((a, b) => (b.totalTransactions || 0) - (a.totalTransactions || 0))
    .slice(0, 15);

  if (sortedData.length === 0) {
    return createEmptyChartOption('2차 아이템 판매건수가 없습니다.', isDarkMode);
  }

  // 총 판매건수 계산
  const totalTransactions = sortedData.reduce(
    (sum, item) => sum + (item.totalTransactions || 0),
    0
  );

  // 파이 차트 데이터 생성
  const pieData = sortedData.map(item => ({
    name: item.displayKey || item.secondaryItemName || '미분류',
    value: item.totalTransactions || 0,
    totalSalesAmount: item.totalSalesAmount || 0,
    totalTransactions: item.totalTransactions || 0,
    percentage:
      totalTransactions > 0
        ? (((item.totalTransactions || 0) / totalTransactions) * 100).toFixed(1)
        : 0,
  }));

  return {
    ...createTitleConfig('2차 아이템 판매건수 점유율', isDarkMode),
    tooltip: {
      ...createTooltipConfig(isDarkMode, {
        trigger: 'item',
        formatter: params => {
          const data = params.data || {};
          const salesAmount = data.totalSalesAmount || 0;
          const transactions = data.totalTransactions || 0;
          const percentage = data.percentage || 0;

          return `<div style="font-weight: 600; margin-bottom: 8px;">${params.name}</div>
                  <div>판매건수: ${transactions}건</div>
                  <div>점유율: ${percentage}%</div>
                  <div>매출: ${formatCurrency(salesAmount)}</div>`;
        },
      }),
    },
    legend: createPieLegendConfig(pieData, isDarkMode),
    series: [
      {
        type: 'pie',
        radius: ['25%', '65%'],
        center: ['40%', '50%'],
        data: pieData,
        itemStyle: {
          borderRadius: 4,
          borderColor: isDarkMode ? '#1F2937' : '#fff',
          borderWidth: 1,
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
            shadowColor: 'rgba(0, 0, 0, 0.3)',
          },
        },
      },
    ],
  };
}
