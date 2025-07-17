<template>
  <div class="home">
    <div class="dashboard-header">
      <h1 class="font-screen-title text-neutral-dark">대시보드</h1>
    </div>

    <div class="dashboard-grid">
      <!-- 통계 카드들 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-header">
            <h3 class="font-section-inner text-gray-700">오늘 잔여 예약 / 매출건수</h3>
            <CalendarIcon :size="24" class="stat-icon" />
          </div>
          <div class="stat-value stat-duo">
            <div class="stat-duo-block">
              <span class="font-screen-title text-primary-main">
                {{ loading ? '...' : getRemainReservationCount() }}
              </span>
              <span class="stat-label">잔여 예약</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-duo-block">
              <span class="font-screen-title text-primary-main">
                {{ loading ? '...' : getTodaySalesCount() }}
              </span>
              <span class="stat-label">매출건수</span>
            </div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <h3 class="font-section-inner text-gray-700">매출액</h3>
            <DollarIcon :size="24" class="stat-icon" />
          </div>
          <div class="stat-value">
            <span class="font-screen-title text-primary-main">
              {{ loading ? '...' : formatCurrency(getTotalSalesAmount()) }}
            </span>
            <span class="stat-change neutral">오늘</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <h3 class="font-section-inner text-gray-700">신규 고객</h3>
            <UsersIcon :size="24" class="stat-icon" />
          </div>
          <div class="stat-value">
            <span class="font-screen-title text-primary-main">
              {{ loading ? '...' : dashboardData.newCustomers?.data?.customerCount || 0 }}
            </span>
            <span class="stat-change neutral">최근 30일</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <h3 class="font-section-inner text-gray-700">이탈 위험</h3>
            <svg
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              class="stat-icon"
            >
              <path
                d="M1 4V10H7"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M23 20V14H17"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M20.49 9C20.0412 7.05369 18.9988 5.30968 17.5095 4.06331C16.0201 2.81693 14.1736 2.13743 12.2453 2.13743C10.3171 2.13743 8.47056 2.81693 6.98124 4.06331C5.49191 5.30968 4.44952 7.05369 4.00076 9L1 4V10H7"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M3.51 15C3.95886 16.9463 5.00122 18.6903 6.49055 19.9367C7.97987 21.1831 9.82641 21.8626 11.7547 21.8626C13.6829 21.8626 15.5294 21.1831 17.0188 19.9367C18.5081 18.6903 19.5505 16.9463 19.9992 15L23 20V14H17"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>
          <div class="stat-value">
            <span class="font-screen-title text-primary-main">
              {{ loading ? '...' : dashboardData.churnRiskCustomers?.data?.customerCount || 0 }}
            </span>
            <span
              v-if="!loading && dashboardData.churnRiskCustomers?.data?.customerCount > 0"
              class="stat-change negative"
            >
              위험
            </span>
            <span v-else-if="!loading" class="stat-change neutral"> 양호 </span>
          </div>
        </div>
      </div>

      <!-- 최근 활동 -->
      <div class="activity-section">
        <div class="activity-grid">
          <div class="card">
            <div class="card-header">
              <div class="header-left">
                <h2 class="card-title">방문 예정</h2>
                <span
                  v-if="!loading && dashboardData.todayReservations?.data?.content"
                  class="count-badge"
                >
                  총 {{ dashboardData.todayReservations.data.content.length }}건
                </span>
              </div>
              <router-link
                to="/reservation/list"
                class="btn btn-sm btn-outline btn-primary header-btn"
                >전체 보기</router-link
              >
            </div>
            <div class="recent-bookings">
              <div v-if="loading" class="loading-state">
                <div class="font-small text-gray-500">로딩 중...</div>
              </div>
              <div v-else-if="errors.recentReservations" class="error-state">
                <div class="font-small text-error-600">{{ errors.recentReservations }}</div>
              </div>
              <div
                v-else-if="!dashboardData.recentReservations?.data?.content?.length"
                class="empty-state"
              >
                <div class="font-small text-gray-500">최근 예약이 없습니다.</div>
              </div>
              <div
                v-for="reservation in dashboardData.recentReservations.data.content"
                v-else
                :key="reservation.reservationId"
                class="booking-item"
              >
                <div class="booking-info">
                  <div class="customer-name font-one-liner-semibold">
                    {{ reservation.customerName }}
                  </div>
                  <div class="service-name font-small text-gray-600">
                    {{ reservation.itemNames }}
                  </div>
                </div>
                <div class="booking-time">
                  <div class="time font-small-semibold">
                    {{ formatTime(reservation.reservationStartAt) }}
                  </div>
                  <div
                    class="status badge"
                    :class="getStatusBadgeClass(reservation.reservationStatusName)"
                  >
                    {{ getStatusText(reservation.reservationStatusName) }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-header">
              <div class="header-left">
                <h2 class="card-title">오늘 매출</h2>
                <span v-if="!loading && dashboardData.todaySales?.data?.list" class="count-badge">
                  총 {{ dashboardData.todaySales.data.list.length }}건
                </span>
              </div>
              <router-link
                to="/sales/management"
                class="btn btn-sm btn-outline btn-primary header-btn"
                >전체 보기</router-link
              >
            </div>
            <div class="recent-sales">
              <div v-if="loading" class="loading-state">
                <div class="font-small text-gray-500">로딩 중...</div>
              </div>
              <div v-else-if="errors.todaySales" class="error-state">
                <div class="font-small text-error-600">{{ errors.todaySales }}</div>
              </div>
              <div v-else-if="!dashboardData.todaySales?.data?.list?.length" class="empty-state">
                <div class="font-small text-gray-500">오늘 매출이 없습니다.</div>
              </div>
              <div
                v-for="sale in dashboardData.recentSales?.data?.list || []"
                v-else
                :key="sale.salesId"
                class="sale-item"
              >
                <div class="sale-info">
                  <div class="customer-name font-one-liner-semibold">{{ sale.customerName }}</div>
                  <div class="sale-details font-small text-gray-600">
                    {{ getSaleItemName(sale) }} / {{ sale.salesType }}
                  </div>
                </div>
                <div class="sale-amount">
                  <div class="amount font-small-semibold">
                    {{ formatCurrency(sale.totalAmount) }}
                  </div>
                  <div class="time font-small text-gray-500">{{ formatTime(sale.salesDate) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, onMounted } from 'vue';
  import { CalendarIcon, DollarIcon, UsersIcon } from '../components/icons';
  import { dashboardAPI } from '@/features/analytics/api/dashboardAPI.js';

  const dashboardData = ref({
    todayReservations: null,
    todaySales: null,
    newCustomers: null,
    churnRiskCustomers: null,
    recentReservations: null,
  });

  const loading = ref(true);
  const errors = ref({});

  const loadDashboardData = async () => {
    try {
      loading.value = true;
      const data = await dashboardAPI.getDashboardData();
      dashboardData.value = data;
      errors.value = data.errors || {};
    } catch (error) {
      console.error('대시보드 데이터 로딩 실패:', error);
      errors.value.general = error.message;
    } finally {
      loading.value = false;
    }
  };

  const formatCurrency = amount => {
    if (!amount) return '0원';
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW',
    }).format(amount);
  };

  const formatTime = dateTimeStr => {
    if (!dateTimeStr) return '';
    const date = new Date(dateTimeStr);
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  };

  const getStatusBadgeClass = status => {
    switch (status) {
      case 'CONFIRMED':
      case '확정':
        return 'badge-success';
      case 'PENDING':
      case '대기':
        return 'badge-warning';
      case 'CANCELLED':
      case '취소':
        return 'badge-error';
      case 'NO_SHOW':
      case '노쇼':
        return 'badge-error';
      default:
        return 'badge-neutral';
    }
  };

  const getStatusText = status => {
    switch (status) {
      case 'CONFIRMED':
        return '확정';
      case 'PENDING':
        return '대기';
      case 'CANCELLED':
        return '취소';
      case 'NO_SHOW':
        return '노쇼';
      default:
        return status;
    }
  };

  const getTodayReservationCount = () => {
    if (!dashboardData.value.todayReservations?.data?.content) return 0;
    return dashboardData.value.todayReservations.data.content.length;
  };

  const getTotalSalesAmount = () => {
    if (!dashboardData.value.todaySales?.data?.list) return 0;
    return dashboardData.value.todaySales.data.list.reduce((total, sale) => {
      return total + (sale.totalAmount || 0);
    }, 0);
  };

  const getSaleItemName = sale => {
    if (sale.secondaryItemName) return sale.secondaryItemName;
    if (sale.prepaidPassName) return sale.prepaidPassName;
    if (sale.sessionPassName) return sale.sessionPassName;
    return '상품명 없음';
  };

  // 잔여 예약: 전체 예약 - 완료/취소/노쇼 제외
  const getRemainReservationCount = () => {
    if (!dashboardData.value.todayReservations?.data?.content) return 0;
    return dashboardData.value.todayReservations.data.content.filter(r => {
      return !['COMPLETED', '완료', 'CANCELLED', '취소', 'NO_SHOW', '노쇼'].includes(
        r.reservationStatusName
      );
    }).length;
  };
  // 매출건수: todaySales 전체 건수
  const getTodaySalesCount = () => {
    if (!dashboardData.value.todaySales?.data?.list) return 0;
    return dashboardData.value.todaySales.data.list.length;
  };

  onMounted(() => {
    loadDashboardData();
  });
</script>

<style scoped>
  .home {
    padding: 0;
  }

  .dashboard-header {
    margin-bottom: 2rem;
  }

  .dashboard-header h1 {
    margin: 0 0 0.5rem 0;
  }

  .dashboard-header p {
    margin: 0;
  }

  .dashboard-grid {
    display: grid;
    gap: 1.5rem;
  }

  .stats-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1rem;
  }

  .stat-card {
    background: var(--color-neutral-white);
    padding: 1.5rem;
    border-radius: 0.75rem;
    border: 1px solid var(--color-gray-200);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  }

  .stat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
  }

  .stat-header h3 {
    margin: 0;
  }

  .stat-icon {
    color: var(--color-gray-500);
    opacity: 0.7;
  }

  .stat-value {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
  }

  .stat-value span:first-child {
    margin: 0;
  }

  .stat-change {
    font-size: 14px;
    font-weight: 600;
    padding: 0.125rem 0.5rem;
    border-radius: 0.25rem;
  }

  .stat-change.positive {
    color: var(--color-success-600);
    background-color: var(--color-success-50);
  }

  .stat-change.negative {
    color: var(--color-error-600);
    background-color: var(--color-error-50);
  }

  .stat-change.neutral {
    color: var(--color-gray-600);
    background-color: var(--color-gray-100);
  }

  .activity-section {
    grid-column: 1 / -1;
  }

  .activity-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
  }

  .recent-bookings,
  .recent-sales {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .booking-item,
  .sale-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem;
    background-color: var(--color-gray-50);
    border-radius: 0.5rem;
  }

  .booking-info,
  .sale-info {
    flex: 1;
  }

  .customer-name {
    margin-bottom: 0.25rem;
  }

  .booking-time,
  .sale-amount {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 0.25rem;
  }

  .booking-time {
    flex-direction: row;
    align-items: center;
    gap: 0.75rem;
  }

  .loading-state,
  .error-state,
  .empty-state {
    padding: 2rem;
    text-align: center;
    background-color: var(--color-gray-50);
    border-radius: 0.5rem;
  }

  .error-state {
    background-color: var(--color-error-50);
  }

  .count-badge {
    display: inline-block;
    margin-left: 0.75rem;
    padding: 0.15em 0.8em;
    font-size: 0.95em;
    font-weight: 600;
    color: var(--color-gray-700);
    background: var(--color-gray-100);
    border-radius: 1em;
    vertical-align: middle;
    letter-spacing: -0.5px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .header-left {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  .count-badge {
    margin-left: 0.5rem;
    padding: 0.15em 0.8em;
    font-size: 0.95em;
    font-weight: 600;
    color: var(--color-gray-700);
    background: var(--color-gray-100);
    border-radius: 1em;
    vertical-align: middle;
    letter-spacing: -0.5px;
  }
  .header-btn {
    margin-left: auto;
  }

  .stat-duo {
    display: flex;
    align-items: stretch;
    gap: 1.5rem;
  }
  .stat-duo-block {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
  }
  .stat-label {
    font-size: 0.95em;
    color: var(--color-gray-600);
    margin-top: 0.25em;
    font-weight: 500;
  }
  .stat-divider {
    width: 1px;
    background: var(--color-gray-200);
    margin: 0 1rem;
    height: 2.5em;
    align-self: center;
  }

  @media (max-width: 768px) {
    .stats-row {
      grid-template-columns: 1fr;
    }

    .activity-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
