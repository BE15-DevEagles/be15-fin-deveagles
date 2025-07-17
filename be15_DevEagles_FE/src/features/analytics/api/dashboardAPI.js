import api from '@/plugins/axios.js';
import { BaseAnalyticsAPI } from '@/features/analytics/api/baseAnalyticsAPI.js';
import { fetchReservationList } from '@/features/schedules/api/schedules.js';
import { getSalesList } from '@/features/sales/api/sales.js';

export class DashboardAPI extends BaseAnalyticsAPI {
  constructor() {
    super('DashboardAPI');
  }

  async getTodayReservations() {
    try {
      const today = new Date();
      const yyyy = today.getFullYear();
      const mm = String(today.getMonth() + 1).padStart(2, '0');
      const dd = String(today.getDate()).padStart(2, '0');
      const from = `${yyyy}-${mm}-${dd}`;
      const to = `${yyyy}-${mm}-${dd}`;
      this.logger.info('오늘 예약 조회 시작', { date: from });
      const data = await fetchReservationList({
        from,
        to,
        page: 0,
        size: 100,
      });
      this.logger.info('오늘 예약 조회 성공', { count: data?.content?.length });
      return { data };
    } catch (error) {
      this.logger.error('오늘 예약 조회 실패', { error: error.message });
      throw this.handleApiError(error);
    }
  }

  async getTodayOrderSales() {
    try {
      const today = new Date();
      const yyyy = today.getFullYear();
      const mm = String(today.getMonth() + 1).padStart(2, '0');
      const dd = String(today.getDate()).padStart(2, '0');
      const startDate = `${yyyy}-${mm}-${dd}T00:00:00`;
      const endDate = `${yyyy}-${mm}-${dd}T23:59:59`;
      this.logger.info('오늘 매출 조회 시작', { startDate, endDate });
      const data = await getSalesList({
        startDate,
        endDate,
        page: 1,
        size: 1000,
      });
      this.logger.info('오늘 매출 조회 성공', { count: data?.list?.length });
      return { data };
    } catch (error) {
      this.logger.error('오늘 매출 조회 실패', { error: error.message });
      throw this.handleApiError(error);
    }
  }

  async getNewCustomersCount() {
    try {
      this.logger.info('신규 고객 수 조회 시작');
      const response = await api.get('/segments/NEW/customers');
      this.logger.info('신규 고객 수 조회 성공', { count: response.data?.customerCount });
      return response.data;
    } catch (error) {
      this.logger.error('신규 고객 수 조회 실패', { error: error.message });
      throw this.handleApiError(error);
    }
  }

  async getChurnRiskCustomersCount() {
    try {
      this.logger.info('이탈 위험 고객 수 조회 시작');
      const response = await api.get('/segments/CHURN_RISK_HIGH/customers');
      this.logger.info('이탈 위험 고객 수 조회 성공', { count: response.data?.customerCount });
      return response.data;
    } catch (error) {
      this.logger.error('이탈 위험 고객 수 조회 실패', { error: error.message });
      throw this.handleApiError(error);
    }
  }

  async getDashboardData() {
    try {
      this.logger.info('대시보드 데이터 전체 조회 시작');
      const [todayReservations, todaySales, newCustomers, churnRiskCustomers] =
        await Promise.allSettled([
          this.getTodayReservations(),
          this.getTodayOrderSales(),
          this.getNewCustomersCount(),
          this.getChurnRiskCustomersCount(),
        ]);
      const recentReservations =
        todayReservations.status === 'fulfilled'
          ? { data: { content: (todayReservations.value?.data?.content || []).slice(0, 5) } }
          : null;
      const recentSales =
        todaySales.status === 'fulfilled'
          ? { data: { list: (todaySales.value?.data?.list || []).slice(0, 5) } }
          : null;
      const todaySalesTotal =
        todaySales.status === 'fulfilled'
          ? (todaySales.value?.list || []).reduce(
              (sum, sale) => sum + (Number(sale.totalAmount) || 0),
              0
            )
          : 0;
      const result = {
        todayReservations:
          todayReservations.status === 'fulfilled' ? todayReservations.value : null,
        todaySales: todaySales.status === 'fulfilled' ? todaySales.value : null,
        todaySalesTotal,
        newCustomers: newCustomers.status === 'fulfilled' ? newCustomers.value : null,
        churnRiskCustomers:
          churnRiskCustomers.status === 'fulfilled' ? churnRiskCustomers.value : null,
        recentReservations,
        recentSales,
        errors: {
          todayReservations:
            todayReservations.status === 'rejected' ? todayReservations.reason?.message : null,
          todaySales: todaySales.status === 'rejected' ? todaySales.reason?.message : null,
          newCustomers: newCustomers.status === 'rejected' ? newCustomers.reason?.message : null,
          churnRiskCustomers:
            churnRiskCustomers.status === 'rejected' ? churnRiskCustomers.reason?.message : null,
          recentReservations: null,
          recentSales: null,
        },
      };
      this.logger.info('대시보드 데이터 전체 조회 완료', {
        successCount: Object.values(result).filter(v => v !== null && typeof v === 'object').length,
      });
      return result;
    } catch (error) {
      this.logger.error('대시보드 데이터 전체 조회 실패', { error: error.message });
      throw this.handleApiError(error);
    }
  }
}

export const dashboardAPI = new DashboardAPI();
