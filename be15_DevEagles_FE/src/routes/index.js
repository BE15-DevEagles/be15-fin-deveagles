import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue';
import { staffRoutes } from '@/features/staffs/route.js';
import { scheduleRoutes } from '@/features/schedules/routes.js';
import { userRoutes } from '@/features/users/route.js';
import { useAuthStore } from '@/store/auth.js';

const routes = [
  // 레이아웃이 없는 관련 페이지 (최상위)

  ...userRoutes,
  ...scheduleRoutes,
  ...staffRoutes,
  {
    // 외부 사용자에게 공개될 실제 멀티프로필 페이지 경로
    // '/p/:shopId'는 '/p/tingkerview' 와 같이 동적으로 매장 ID를 받습니다.
    path: '/p/:shopId',
    name: 'PublicProfile',
    component: () => import('@/features/profilelink/views/PublicProfileView.vue'), // 새로 만들 컴포넌트
    meta: {
      layout: 'blank',
    },
  },

  // 레이아웃이 있는 모든 페이지들 (중첩 라우팅)
  {
    path: '/',
    component: () => import('@/components/layout/TheLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: Home,
      },
      {
        path: 'schedule/calendar',
        name: 'ScheduleCalendar',
        component: () => import('@/features/schedules/views/ScheduleCalendarView.vue'),
      },
      {
        path: 'reservation/list',
        name: 'ReservationList',
        component: () => import('@/features/schedules/views/ReservationListView.vue'),
      },
      {
        path: 'schedule/plan',
        name: 'SchedulePlanLeave',
        component: () => import('@/features/schedules/views/SchedulePlanLeaveView.vue'),
      },
      {
        path: 'reservation/requests',
        name: 'ReservationRequests',
        component: () => import('@/features/schedules/views/ReservationRequestsView.vue'),
      },
      {
        path: 'reservation/history',
        name: 'ReservationHistory',
        component: () => import('@/features/schedules/views/ReservationHistoryView.vue'),
      },
      {
        path: 'customer/list',
        name: 'CustomerList',
        component: () => import('@/features/customer/views/CustomerListView.vue'),
      },
      {
        path: 'customer/prepaid',
        name: 'CustomerPrepaid',
        component: Home,
      },
      {
        path: 'sales/management',
        name: 'SalesManagement',
        component: () => import('@/features/sales/views/SalesView.vue'),
      },
      {
        path: 'sales/staff',
        name: 'SalesStaff',
        component: () => import('@/features/staffsales/view/StaffSalesListView.vue'),
      },
      {
        path: 'item/service',
        name: 'ItemService',
        component: () => import('@/features/items/views/ItemView.vue'),
      },
      {
        path: 'item/membership',
        name: 'ItemMembership',
        component: () => import('@/features/membership/view/MembershipView.vue'),
      },
      {
        path: 'analytics/sales',
        name: 'SalesAnalytics',
        component: () => import('@/features/analytics/views/SalesAnalytics.vue'),
      },
      {
        path: 'analytics/usage',
        name: 'AnalyticsUsage',
        component: () => import('@/features/analytics/views/UsageAnalytics.vue'),
      },
      {
        path: 'analytics/revenue',
        name: 'AnalyticsRevenue',
        component: Home,
      },
      {
        path: 'message/history',
        name: 'MessageHistory',
        component: () => import('@/features/messages/views/MessagesListView.vue'),
      },
      {
        path: 'message/templates',
        name: 'MessageTemplates',
        component: () => import('@/features/messages/views/TemplateListView.vue'),
      },
      {
        path: 'message/settings',
        name: 'MessageSettings',
        component: () => import('@/features/messages/views/SettingsView.vue'),
      },
      {
        path: 'message/ab-test',
        name: 'MessageAbTest',
        component: Home,
      },
      {
        path: 'workflows',
        name: 'WorkflowList',
        component: () => import('@/features/workflows/views/WorkflowList.vue'),
      },
      {
        path: 'workflows/create',
        name: 'WorkflowCreate',
        component: () => import('@/features/workflows/views/WorkflowCreate.vue'),
      },
      {
        path: 'workflows/edit/:id',
        name: 'WorkflowEdit',
        component: () => import('@/features/workflows/views/WorkflowEdit.vue'),
      },
      {
        path: 'campaigns',
        name: 'CampaignList',
        component: () => import('@/features/campaigns/views/CampaignList.vue'),
      },
      {
        path: 'campaigns/:id',
        name: 'CampaignDetail',
        component: () => import('@/features/campaigns/views/CampaignDetail.vue'),
      },
      {
        path: 'coupons',
        name: 'CouponList',
        component: () => import('@/features/coupons/views/CouponList.vue'),
      },
      {
        path: 'profile-link',
        name: 'ProfileLink',
        component: () => import('@/features/profilelink/views/ProfileLinkView.vue'),
      },
      {
        path: 'settings/store',
        name: 'SettingsStore',
        component: () => import('@/features/users/views/StoreSettingView.vue'),
      },
      {
        path: 'settings/reservation',
        name: 'SettingsReservation',
        component: () => import('@/features/schedules/views/SettingsReservation.vue'),
      },
      {
        path: 'settings/staff',
        name: 'SettingsStaff',
        component: () => import('@/features/staffs/views/StaffListView.vue'),
      },
      {
        path: 'settings/customer-grade',
        name: 'SettingsCustomerGrade',
        component: Home,
      },
      {
        path: 'settings/customer-tag',
        name: 'SettingsCustomerTag',
        component: Home,
      },
      {
        path: 'settings/account',
        name: 'SettingsAccount',
        component: () => import('@/features/users/views/AccountSettingView.vue'),
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/features/users/views/ProfileSettingView.vue'),
      },
      ...staffRoutes.map(route => ({
        ...route,
        path: route.path.startsWith('/') ? route.path.slice(1) : route.path,
      })),
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore();

  const publicPages = [
    '/login',
    '/sign-up',
    '/verify-reset',
    '/edit-pwd',
    /^\/reserve(\/.*)?$/,
    /^\/p\/\d+$/,
  ];

  const isPublicPage = publicPages.some(page => {
    if (page instanceof RegExp) {
      return page.test(to.path);
    }
    return page === to.path;
  });

  const authRequired = !isPublicPage;
  if (authRequired && !authStore.isAuthenticated) {
    // 인증이 필요한 페이지에서 토큰이 없으면 웹소켓 연결도 끊기
    try {
      const { disconnectSocket } = await import('@/features/chat/composables/socket.js');
      disconnectSocket();
      console.log('[Router] 웹소켓 연결 종료 (인증 실패)');
    } catch (e) {
      console.warn('[Router] 웹소켓 연결 종료 실패:', e);
    }
    return next('/login');
  }
  next();
});

export default router;
