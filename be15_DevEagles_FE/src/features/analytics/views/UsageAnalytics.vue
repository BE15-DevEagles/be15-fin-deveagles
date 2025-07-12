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
          <h1 class="analytics-page-title">고객 이용 통계</h1>
        </div>

        <!-- 필터 섹션 -->
        <div class="analytics-filters">
          <div class="analytics-filter-row">
            <!-- 날짜 범위 선택 -->
            <div class="analytics-filter-group">
              <label class="analytics-filter-label">시작일</label>
              <div class="date-input-wrapper">
                <input
                  ref="startDateInput"
                  v-model="filters.dateRange.start"
                  type="date"
                  class="analytics-filter-select"
                  @change="
                    updateFilters({
                      dateRange: { ...filters.dateRange, start: $event.target.value },
                    })
                  "
                />
                <div class="date-input-icon" @click="$refs.startDateInput.showPicker()"></div>
              </div>
            </div>

            <div class="analytics-filter-group">
              <label class="analytics-filter-label">종료일</label>
              <div class="date-input-wrapper">
                <input
                  ref="endDateInput"
                  v-model="filters.dateRange.end"
                  type="date"
                  class="analytics-filter-select"
                  :min="filters.dateRange.start"
                  @change="
                    updateFilters({ dateRange: { ...filters.dateRange, end: $event.target.value } })
                  "
                />
                <div class="date-input-icon" @click="$refs.endDateInput.showPicker()"></div>
              </div>
            </div>

            <!-- 다크모드 토글 스위치 -->
            <div class="analytics-filter-group">
              <LocalDarkModeToggle />
            </div>

            <!-- 새로고침 버튼 -->
            <div class="analytics-filter-group">
              <button
                class="analytics-filter-reset-btn"
                :disabled="loading"
                @click="loadUsageData(true)"
              >
                {{ loading ? '로딩 중...' : '새로고침' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 주요 지표 카드 -->
    <div class="analytics-stats-grid">
      <StatCard
        icon="📊"
        label="전체 예약율"
        :value="usageData.overallUtilization + '%'"
        variant="primary"
      />

      <StatCard
        icon="⏰"
        label="타임슬롯 기준"
        :value="usageData.averageUsageTime + '분'"
        variant="success"
      />

      <StatCard
        icon="👥"
        label="직원 가동률"
        :value="usageData.staffUtilization + '%'"
        variant="info"
      />

      <StatCard
        icon="🏆"
        label="피크 시간 예약율"
        :value="usageData.peakHourUtilization + '%'"
        variant="warning"
      />
    </div>

    <!-- 에러 표시 -->
    <div v-if="error && !loading" class="analytics-error">
      <div class="analytics-error-icon">⚠️</div>
      <div class="analytics-error-message">{{ error }}</div>
      <button class="analytics-filter-reset-btn" @click="loadUsageData(true)">다시 시도</button>
    </div>

    <!-- 차트 그리드 -->
    <div v-if="!error" class="dashboard-content">
      <!-- 시간대별/직원별 차트 그리드 (2열 배치) -->
      <div class="dashboard-charts-section">
        <div class="dashboard-charts-grid">
          <!-- 시간대별 고객 방문건수 -->
          <div class="dashboard-chart-container">
            <div class="dashboard-chart-header">
              <h3 class="dashboard-chart-title">시간대별 고객 방문건수</h3>
              <p class="dashboard-chart-subtitle">시간대별 남성/여성 고객 방문 현황</p>
            </div>
            <BaseChart
              v-if="!loading && usageData.hourlyUsage.length > 0"
              :option="getHourlyUsageChartOption(isDarkMode)"
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
              <div class="dashboard-empty-icon">👥</div>
              <div class="dashboard-empty-message">시간대별 방문객 데이터가 없습니다</div>
            </div>
          </div>

          <!-- 직원별 예약건수 -->
          <div class="dashboard-chart-container">
            <div class="dashboard-chart-header">
              <h3 class="dashboard-chart-title">직원별 예약건수</h3>
              <p class="dashboard-chart-subtitle">직원별 예약 처리 현황</p>
            </div>
            <BaseChart
              v-if="!loading && usageData.staffUsage.length > 0"
              :option="getStaffReservationChartOption(isDarkMode)"
              :loading="loading"
              :is-dark-mode="isDarkMode"
              height="400px"
            />
            <div v-else-if="loading" class="dashboard-loading">
              <div class="dashboard-loading-spinner"></div>
              <span class="dashboard-loading-text">직원별 예약건수 데이터 로딩 중...</span>
            </div>
            <div v-else class="dashboard-empty">
              <div class="dashboard-empty-icon">👨‍💼</div>
              <div class="dashboard-empty-message">직원별 예약건수 데이터가 없습니다</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 일별 고객 방문건수 (성별 구분) -->
      <div class="dashboard-charts-section">
        <div class="dashboard-chart-container dashboard-chart-full">
          <div class="dashboard-chart-header">
            <h3 class="dashboard-chart-title">일별 고객 방문건수</h3>
            <p class="dashboard-chart-subtitle">
              {{ filters.dateRange.start }} ~ {{ filters.dateRange.end }} 기간의 일별 남성/여성 고객
              방문 현황
            </p>
          </div>
          <BaseChart
            v-if="!loading && usageData.dailyVisitorData.length > 0"
            :option="getDailyVisitorChartOption(isDarkMode)"
            :loading="loading"
            :is-dark-mode="isDarkMode"
            height="400px"
            @click="onChartClick"
          />
          <div v-else-if="loading" class="dashboard-loading">
            <div class="dashboard-loading-spinner"></div>
            <span class="dashboard-loading-text">일별 방문객 데이터를 불러오는 중...</span>
          </div>
          <div v-else class="dashboard-empty">
            <div class="dashboard-empty-icon">📅</div>
            <div class="dashboard-empty-message">일별 방문객 데이터가 없습니다</div>
          </div>
        </div>
      </div>

      <!-- 요일별 시간대 예약율 히트맵 -->
      <div class="dashboard-charts-section">
        <div class="dashboard-chart-container dashboard-chart-full">
          <div class="dashboard-chart-header">
            <h3 class="dashboard-chart-title">요일별 시간대 예약율 히트맵</h3>
            <p class="dashboard-chart-subtitle">요일과 시간대에 따른 예약율 패턴 분석</p>
          </div>
          <BaseChart
            v-if="!loading && usageData.heatmapData.length > 0"
            :option="getHeatmapChartOption(isDarkMode)"
            :loading="loading"
            :is-dark-mode="isDarkMode"
            height="280px"
          />
          <div v-else-if="loading" class="dashboard-loading">
            <div class="dashboard-loading-spinner"></div>
            <span class="dashboard-loading-text">히트맵 데이터를 불러오는 중...</span>
          </div>
          <div v-else class="dashboard-empty">
            <div class="dashboard-empty-icon">🗓️</div>
            <div class="dashboard-empty-message">요일별 시간대 예약율 데이터가 없습니다</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import { onMounted } from 'vue';
  import BaseChart from '../components/charts/BaseChart.vue';
  import StatCard from '../components/StatCard.vue';
  import LocalDarkModeToggle from '../components/LocalDarkModeToggle.vue';
  import { useUsageAnalytics } from '../composables/useUsageAnalytics.js';
  import { useLocalDarkMode } from '../composables/useLocalDarkMode.js';

  export default {
    name: 'UsageAnalytics',
    components: {
      BaseChart,
      StatCard,
      LocalDarkModeToggle,
    },
    setup() {
      // 로컬 다크모드 상태 관리
      const { isDarkMode, isTransitioning, initializeLocalDarkMode } = useLocalDarkMode();

      // 이용률 분석 컴포저블 사용
      const {
        loading,
        error,
        usageData,
        filters,
        loadUsageData,
        updateFilters,
        getHourlyUsageChartOption,
        getDailyVisitorChartOption,
        getStaffReservationChartOption,
        getHeatmapChartOption,
        formatPercentage,
      } = useUsageAnalytics();

      // 차트 클릭 이벤트 핸들러
      const onChartClick = event => {
        console.log('차트 클릭:', event);
        // 차트 클릭 시 상세 정보 표시 로직 추가
      };

      // 컴포넌트 마운트 시 데이터 로드 및 로컬 다크모드 초기화
      onMounted(() => {
        loadUsageData();
        initializeLocalDarkMode();
      });

      // 템플릿에서 사용할 모든 변수와 함수 반환
      return {
        // 상태
        isDarkMode,
        isTransitioning,
        loading,
        error,
        usageData,
        filters,

        // 메서드
        loadUsageData,
        updateFilters,
        onChartClick,

        // 차트 옵션
        getHourlyUsageChartOption,
        getDailyVisitorChartOption,
        getStaffReservationChartOption,
        getHeatmapChartOption,

        // 유틸리티
        formatPercentage,
      };
    },
  };
</script>

<style scoped>
  @import '../styles/analytics.css';
</style>
