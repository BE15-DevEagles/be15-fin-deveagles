export const scheduleRoutes = [
  {
    path: '/reserve/:shopId/staff',
    name: 'reserveDesigner',
    component: () => import('@/features/schedules/views/DesignerListView.vue'),
  },

  {
    path: '/reserve/:shopId/staff/:staffId',
    name: 'reserve',
    component: () => import('@/features/schedules/views/ReserveView.vue'),
  },
];
