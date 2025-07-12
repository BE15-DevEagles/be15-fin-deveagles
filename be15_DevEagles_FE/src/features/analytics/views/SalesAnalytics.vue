<template>
  <div
    class="analytics-page"
    :class="{
      dark: isDarkMode,
      transitioning: isTransitioning,
    }"
  >
    <!-- 페이지 헤더 -->
    <div class="analytics-header">
      <div class="analytics-header-content">
        <div>
          <h1 class="analytics-page-title">매출 통계</h1>
        </div>

        <!-- 필터 및 컨트롤 -->
        <div class="analytics-filters">
          <div class="analytics-filter-row">
            <!-- 날짜 범위 선택 -->
            <div class="analytics-filter-group">
              <label class="analytics-filter-label">시작일</label>
              <div class="date-input-wrapper">
                <input
                  ref="startDateInput"
                  v-model="filters.startDate"
                  type="date"
                  class="analytics-filter-select"
                  @change="updateFilters({ startDate: $event.target.value })"
                />
                <div class="date-input-icon" @click="$refs.startDateInput.showPicker()"></div>
              </div>
            </div>

            <div class="analytics-filter-group">
              <label class="analytics-filter-label">종료일</label>
              <div class="date-input-wrapper">
                <input
                  ref="endDateInput"
                  v-model="filters.endDate"
                  type="date"
                  class="analytics-filter-select"
                  :min="filters.startDate"
                  @change="updateFilters({ endDate: $event.target.value })"
                />
                <div class="date-input-icon" @click="$refs.endDateInput.showPicker()"></div>
              </div>
            </div>

            <!-- 다크모드 토글 -->
            <div class="analytics-filter-group">
              <LocalDarkModeToggle />
            </div>

            <!-- 새로고침 버튼 -->
            <div class="analytics-filter-group">
              <button
                class="analytics-filter-reset-btn"
                :disabled="loading"
                @click="loadSalesData(true)"
              >
                {{ loading ? '로딩 중...' : '새로고침' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 에러 표시 -->
    <div v-if="error && !loading" class="analytics-error">
      <div class="analytics-error-icon">⚠️</div>
      <div class="analytics-error-message">{{ error }}</div>
      <button class="analytics-filter-reset-btn" @click="loadSalesData(true)">다시 시도</button>
    </div>

    <!-- 주요 지표 카드 -->
    <div v-if="!error" class="analytics-stats-grid">
      <StatCard
        v-for="card in dashboardData.summaryCards"
        :key="card.label"
        :icon="card.icon"
        :label="card.label"
        :value="card.value"
        :variant="card.variant"
      />
    </div>

    <!-- 대시보드 컨텐츠 -->
    <div v-if="!error" class="dashboard-content">
      <!-- 일별 매출 추이 -->
      <div class="dashboard-charts-section">
        <div class="dashboard-chart-container dashboard-chart-full">
          <div class="dashboard-chart-header">
            <h3 class="dashboard-chart-title">일별 매출 추이</h3>
            <p class="dashboard-chart-subtitle">
              {{ filters.startDate }} ~ {{ filters.endDate }} 기간의 일별 매출 및 거래 현황
            </p>
          </div>
          <BaseChart
            v-if="!loading && dashboardData.dailySales.length > 0"
            :option="getDailySalesChartOption(isDarkMode)"
            :loading="loading"
            :is-dark-mode="isDarkMode"
            height="400px"
            @click="onChartClick"
          />
          <div v-else-if="loading" class="dashboard-loading">
            <div class="dashboard-loading-spinner"></div>
            <span class="dashboard-loading-text">데이터를 불러오는 중...</span>
          </div>
          <div v-else class="dashboard-empty">
            <div class="dashboard-empty-icon">📊</div>
            <div class="dashboard-empty-message">표시할 데이터가 없습니다</div>
          </div>
        </div>
      </div>

      <!-- 분석 차트 그리드 -->
      <div class="dashboard-charts-section">
        <div class="dashboard-charts-grid">
          <!-- 카테고리별 매출 분석 -->
          <div class="dashboard-chart-container">
            <div class="dashboard-chart-header">
              <h3 class="dashboard-chart-title">카테고리별 매출 분석</h3>
              <p class="dashboard-chart-subtitle">서비스 카테고리별 매출 성과 비교</p>
            </div>
            <BaseChart
              v-if="!loading && dashboardData.categoryData.length > 0"
              :option="getCategoryChartOption(isDarkMode)"
              :loading="loading"
              :is-dark-mode="isDarkMode"
              height="400px"
              @click="onChartClick"
            />
            <div v-else-if="loading" class="dashboard-loading">
              <div class="dashboard-loading-spinner"></div>
              <span class="dashboard-loading-text">차트 데이터 로딩 중...</span>
            </div>
            <div v-else class="dashboard-empty">
              <div class="dashboard-empty-icon">📈</div>
              <div class="dashboard-empty-message">카테고리 데이터가 없습니다</div>
            </div>
          </div>

          <!-- 성별 매출 분포 -->
          <div class="dashboard-chart-container">
            <div class="dashboard-chart-header">
              <h3 class="dashboard-chart-title">성별 매출 분포</h3>
              <p class="dashboard-chart-subtitle">고객 성별에 따른 매출 비율</p>
            </div>
            <BaseChart
              v-if="!loading && dashboardData.genderData.length > 0"
              :option="getGenderChartOption(isDarkMode)"
              :loading="loading"
              :is-dark-mode="isDarkMode"
              height="400px"
            />
            <div v-else-if="loading" class="dashboard-loading">
              <div class="dashboard-loading-spinner"></div>
            </div>
            <div v-else class="dashboard-empty">
              <div class="dashboard-empty-icon">👥</div>
              <div class="dashboard-empty-message">성별 데이터가 없습니다</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 추가 분석 차트 그리드 -->
      <div class="dashboard-charts-section">
        <div class="dashboard-charts-grid">
          <!-- 매출 추이 분석 -->
          <div class="dashboard-chart-container">
            <div class="dashboard-chart-header">
              <h3 class="dashboard-chart-title">{{ getTrendChartTitle }}</h3>
              <p class="dashboard-chart-subtitle">{{ getTrendChartSubtitle }}</p>
            </div>
            <BaseChart
              v-if="!loading && dashboardData.trendData.length > 0"
              :option="getTrendChartOption(isDarkMode)"
              :loading="loading"
              :is-dark-mode="isDarkMode"
              height="400px"
            />
            <div v-else-if="loading" class="dashboard-loading">
              <div class="dashboard-loading-spinner"></div>
            </div>
            <div v-else class="dashboard-empty">
              <div class="dashboard-empty-icon">📈</div>
              <div class="dashboard-empty-message">매출 추이 데이터가 없습니다</div>
            </div>
          </div>

          <!-- 할인 효과 분석 -->
          <div class="dashboard-chart-container">
            <div class="dashboard-chart-header">
              <h3 class="dashboard-chart-title">{{ getDiscountChartTitle }}</h3>
              <p class="dashboard-chart-subtitle">{{ getDiscountChartSubtitle }}</p>
            </div>
            <BaseChart
              v-if="!loading && dashboardData.discountData.length > 0"
              :option="getDiscountChartOption(isDarkMode)"
              :loading="loading"
              :is-dark-mode="isDarkMode"
              height="400px"
            />
            <div v-else-if="loading" class="dashboard-loading">
              <div class="dashboard-loading-spinner"></div>
            </div>
            <div v-else class="dashboard-empty">
              <div class="dashboard-empty-icon">💰</div>
              <div class="dashboard-empty-message">할인 분석 데이터가 없습니다</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 1차 상품별 일별 매출추이 차트 -->
      <div class="dashboard-charts-section">
        <div class="dashboard-chart-container dashboard-chart-full">
          <div class="dashboard-chart-header">
            <h3 class="dashboard-chart-title">1차 상품별 일별 매출추이</h3>
            <p class="dashboard-chart-subtitle">
              {{ filters.startDate }} ~ {{ filters.endDate }} 기간의 주요 상품별 일별 매출 변화
              (상위 10개)
            </p>
          </div>
          <BaseChart
            v-if="!loading && dashboardData.primaryItemDailyTrendData.length > 0"
            :option="getPrimaryItemDailyTrendChartOption(isDarkMode)"
            :loading="loading"
            :is-dark-mode="isDarkMode"
            height="500px"
            @click="onChartClick"
          />
          <div v-else-if="loading" class="dashboard-loading">
            <div class="dashboard-loading-spinner"></div>
            <span class="dashboard-loading-text">1차 상품별 일별 매출추이 데이터 로딩 중...</span>
          </div>
          <div v-else class="dashboard-empty">
            <div class="dashboard-empty-icon">📈</div>
            <div class="dashboard-empty-message">1차 상품별 일별 매출추이 데이터가 없습니다</div>
          </div>
        </div>
      </div>

      <!-- 아이템 점유율 분석 차트 그리드 -->
      <div class="dashboard-charts-section">
        <div class="dashboard-charts-grid">
          <!-- 1차 아이템 판매 점유율 -->
          <div class="dashboard-chart-container">
            <div class="dashboard-chart-header">
              <h3 class="dashboard-chart-title">1차 아이템 판매 점유율</h3>
              <p class="dashboard-chart-subtitle">주요 서비스별 매출 비중 분석 (상위 10개)</p>
            </div>
            <BaseChart
              v-if="
                !loading &&
                !itemDataLoading &&
                dashboardData.primaryItemData &&
                dashboardData.primaryItemData.length > 0
              "
              :option="getPrimaryItemShareChartOption(isDarkMode)"
              :loading="false"
              :is-dark-mode="isDarkMode"
              height="500px"
            />
            <div v-else-if="loading || itemDataLoading" class="dashboard-loading">
              <div class="dashboard-loading-spinner"></div>
              <span class="dashboard-loading-text">1차 아이템 데이터 로딩 중...</span>
            </div>
            <div v-else class="dashboard-empty">
              <div class="dashboard-empty-icon">🥇</div>
              <div class="dashboard-empty-message">1차 아이템 데이터가 없습니다</div>
              <div
                class="dashboard-empty-debug"
                style="font-size: 12px; color: #999; margin-top: 8px"
              >
                배열 여부: {{ Array.isArray(dashboardData.primaryItemData) }}, 길이:
                {{ dashboardData.primaryItemData?.length || 0 }}
              </div>
            </div>
          </div>

          <!-- 2차 아이템 판매 점유율 -->
          <div class="dashboard-chart-container">
            <div class="dashboard-chart-header">
              <h3 class="dashboard-chart-title">2차 아이템 판매 점유율</h3>
              <p class="dashboard-chart-subtitle">세부 서비스별 매출 비중 분석 (상위 15개)</p>
            </div>
            <BaseChart
              v-if="
                !loading &&
                !itemDataLoading &&
                dashboardData.secondaryItemData &&
                dashboardData.secondaryItemData.length > 0
              "
              :option="getSecondaryItemShareChartOption(isDarkMode)"
              :loading="false"
              :is-dark-mode="isDarkMode"
              height="500px"
            />
            <div v-else-if="loading || itemDataLoading" class="dashboard-loading">
              <div class="dashboard-loading-spinner"></div>
              <span class="dashboard-loading-text">2차 아이템 데이터 로딩 중...</span>
            </div>
            <div v-else class="dashboard-empty">
              <div class="dashboard-empty-icon">🥈</div>
              <div class="dashboard-empty-message">2차 아이템 데이터가 없습니다</div>
              <div
                class="dashboard-empty-debug"
                style="font-size: 12px; color: #999; margin-top: 8px"
              >
                배열 여부: {{ Array.isArray(dashboardData.secondaryItemData) }}, 길이:
                {{ dashboardData.secondaryItemData?.length || 0 }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 아이템 판매건수 점유율 분석 차트 그리드 -->
      <div class="dashboard-charts-section">
        <div class="dashboard-charts-grid">
          <!-- 1차 아이템 판매건수 점유율 -->
          <div class="dashboard-chart-container">
            <div class="dashboard-chart-header">
              <h3 class="dashboard-chart-title">1차 아이템 판매건수 점유율</h3>
              <p class="dashboard-chart-subtitle">주요 서비스별 판매건수 비중 분석 (상위 10개)</p>
            </div>
            <BaseChart
              v-if="
                !loading &&
                !itemDataLoading &&
                dashboardData.primaryItemData &&
                dashboardData.primaryItemData.length > 0
              "
              :option="getPrimaryItemTransactionShareChartOption(isDarkMode)"
              :loading="false"
              :is-dark-mode="isDarkMode"
              height="500px"
            />
            <div v-else-if="loading || itemDataLoading" class="dashboard-loading">
              <div class="dashboard-loading-spinner"></div>
              <span class="dashboard-loading-text">1차 아이템 판매건수 데이터 로딩 중...</span>
            </div>
            <div v-else class="dashboard-empty">
              <div class="dashboard-empty-icon">🥇📊</div>
              <div class="dashboard-empty-message">1차 아이템 판매건수 데이터가 없습니다</div>
            </div>
          </div>

          <!-- 2차 아이템 판매건수 점유율 -->
          <div class="dashboard-chart-container">
            <div class="dashboard-chart-header">
              <h3 class="dashboard-chart-title">2차 아이템 판매건수 점유율</h3>
              <p class="dashboard-chart-subtitle">세부 서비스별 판매건수 비중 분석 (상위 15개)</p>
            </div>
            <BaseChart
              v-if="
                !loading &&
                !itemDataLoading &&
                dashboardData.secondaryItemData &&
                dashboardData.secondaryItemData.length > 0
              "
              :option="getSecondaryItemTransactionShareChartOption(isDarkMode)"
              :loading="false"
              :is-dark-mode="isDarkMode"
              height="500px"
            />
            <div v-else-if="loading || itemDataLoading" class="dashboard-loading">
              <div class="dashboard-loading-spinner"></div>
              <span class="dashboard-loading-text">2차 아이템 판매건수 데이터 로딩 중...</span>
            </div>
            <div v-else class="dashboard-empty">
              <div class="dashboard-empty-icon">🥈📊</div>
              <div class="dashboard-empty-message">2차 아이템 판매건수 데이터가 없습니다</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import { onMounted, computed } from 'vue';
  import BaseChart from '../components/charts/BaseChart.vue';
  import StatCard from '../components/StatCard.vue';
  import LocalDarkModeToggle from '../components/LocalDarkModeToggle.vue';
  import { useSalesAnalytics } from '../composables/useSalesAnalytics.js';
  import { useLocalDarkMode } from '../composables/useLocalDarkMode.js';

  export default {
    name: 'SalesAnalytics',
    components: {
      BaseChart,
      StatCard,
      LocalDarkModeToggle,
    },
    setup() {
      // 로컬 다크모드 상태 관리
      const { isDarkMode, isTransitioning, initializeLocalDarkMode } = useLocalDarkMode();

      // 매출 분석 컴포저블 사용
      const {
        loading,
        error,
        dashboardData,
        filters,
        itemDataLoading,
        loadSalesData,
        updateFilters,
        getDailySalesChartOption,
        getCategoryChartOption,
        getGenderChartOption,
        getTrendChartOption,
        getDiscountChartOption,
        getPrimaryItemShareChartOption,
        getSecondaryItemShareChartOption,
        getPrimaryItemTransactionShareChartOption,
        getSecondaryItemTransactionShareChartOption,
        getPrimaryItemDailyTrendChartOption,
        formatCurrency,
      } = useSalesAnalytics();

      // 차트 클릭 이벤트 핸들러
      const onChartClick = event => {
        console.log('차트 클릭:', event);
        // 차트 클릭 시 드릴다운 분석 등의 로직 추가 가능
        if (event.data && event.data.displayKey) {
          // 특정 항목 클릭 시 상세 분석으로 이동하는 로직
          console.log('선택된 항목:', event.data.displayKey);
        }
      };

      // 동적 차트 제목 생성
      const getTrendChartTitle = computed(() => {
        const trendData = dashboardData.trendData;
        if (trendData.length === 0) return '매출 추이';

        const groupBy = trendData[0]?.groupBy || 'WEEK';
        const titles = {
          WEEK: '주별 매출 추이',
          MONTH: '월별 매출 추이',
        };
        return titles[groupBy] || '매출 추이';
      });

      const getTrendChartSubtitle = computed(() => {
        const trendData = dashboardData.trendData;
        if (trendData.length === 0) return '매출 트렌드와 성장률 추적';

        const groupBy = trendData[0]?.groupBy || 'WEEK';
        const subtitles = {
          WEEK: '주별 매출 트렌드와 성장률 추적',
          MONTH: '월별 매출 트렌드와 성장률 추적',
        };
        return subtitles[groupBy] || '매출 트렌드와 성장률 추적';
      });

      const getDiscountChartTitle = computed(() => {
        const discountData = dashboardData.discountData;
        if (discountData.length === 0) return '할인 효과 분석';

        const groupBy = discountData[0]?.groupBy || 'WEEK';
        const titles = {
          WEEK: '주별 할인 효과 분석',
          MONTH: '월별 할인 효과 분석',
        };
        return titles[groupBy] || '할인 효과 분석';
      });

      const getDiscountChartSubtitle = computed(() => {
        const discountData = dashboardData.discountData;
        if (discountData.length === 0) return '할인 캠페인의 효과와 ROI 측정';

        const groupBy = discountData[0]?.groupBy || 'WEEK';
        const subtitles = {
          WEEK: '주별 할인 캠페인의 효과와 ROI 측정',
          MONTH: '월별 할인 캠페인의 효과와 ROI 측정',
        };
        return subtitles[groupBy] || '할인 캠페인의 효과와 ROI 측정';
      });

      // 컴포넌트 마운트 시 초기화
      onMounted(async () => {
        initializeLocalDarkMode();
        await loadSalesData();
      });

      return {
        // 상태
        isDarkMode,
        isTransitioning,
        loading,
        error,
        dashboardData,
        filters,
        itemDataLoading,

        // 메서드
        loadSalesData,
        updateFilters,
        onChartClick,

        // 차트 옵션
        getDailySalesChartOption,
        getCategoryChartOption,
        getGenderChartOption,
        getTrendChartOption,
        getDiscountChartOption,
        getPrimaryItemShareChartOption,
        getSecondaryItemShareChartOption,
        getPrimaryItemTransactionShareChartOption,
        getSecondaryItemTransactionShareChartOption,
        getPrimaryItemDailyTrendChartOption,

        // 동적 제목
        getTrendChartTitle,
        getTrendChartSubtitle,
        getDiscountChartTitle,
        getDiscountChartSubtitle,

        // 유틸리티
        formatCurrency,
      };
    },
  };
</script>

<style scoped>
  @import '../styles/analytics.css';
</style>
