import api from '@/plugins/axios';

// 일정 캘린더 전체 조회 API
export const getCalendarSchedules = async ({
  from,
  to,
  customerKeyword,
  itemKeyword,
  staffId,
  scheduleType,
}) => {
  const response = await api.get('/schedules/calendar', {
    params: {
      from,
      to,
      customerKeyword,
      itemKeyword,
      staffId,
      scheduleType,
    },
  });

  return response.data.data;
};

export const getRegularSchedules = async params => {
  const response = await api.get('/schedules/calendar/regular', { params });
  return response.data.data;
};

export const fetchPlanList = async params => {
  const response = await api.get('/schedules/plans', { params });
  return response.data.data;
};

export const fetchLeaveList = async params => {
  const response = await api.get('/schedules/leaves', { params });
  return response.data.data;
};

// 직원 목록 조회 API
export const getStaffList = async ({
  page = 1,
  size = 100,
  keyword = '',
  isActive = true,
} = {}) => {
  const response = await api.get('/staffs', {
    params: {
      page,
      size,
      keyword,
      isActive,
    },
  });

  return response.data.data.staffList ?? [];
};

export const deletePlans = async deleteRequests => {
  return await api.delete('/schedules/plans', { data: deleteRequests });
};

export const deleteLeaves = async deleteRequests => {
  return await api.delete('/schedules/leaves', { data: deleteRequests });
};

export const fetchScheduleDetail = async (type, id) => {
  switch (type) {
    case 'plan':
      return await api.get(`/schedules/plans/${id}`);
    case 'regular_plan':
      return await api.get(`/schedules/plans/regular/${id}`);
    case 'leave':
      return await api.get(`/schedules/leaves/${id}`);
    case 'regular_leave':
      return await api.get(`/schedules/leaves/regular/${id}`);
    default:
      throw new Error('Invalid schedule type');
  }
};

export const fetchReservationHistories = async ({ page = 0, size = 10 }) => {
  const response = await api.get('/schedules/reservations/history', {
    params: { page, size },
  });
  return response.data.data;
};

export const fetchReservationRequests = async ({ page = 0, size = 10 }) => {
  const response = await api.get('/schedules/reservations/requests', {
    params: { page, size },
  });
  return response.data.data;
};

export const fetchReservationList = async ({
  staffId,
  reservationStatusName,
  customerKeyword,
  from,
  to,
  page = 0,
  size = 10,
}) => {
  const response = await api.get('/schedules/reservations', {
    params: {
      staffId,
      reservationStatusName,
      customerKeyword,
      from,
      to,
      page,
      size,
    },
  });

  return response.data.data;
};

export const fetchReservationDetail = async id => {
  const response = await api.get(`/schedules/reservations/${id}`);
  return response.data.data;
};

export const deleteReservation = async reservationId => {
  await api.delete(`/schedules/reservations/${reservationId}`);
};

export const updateReservationStatuses = async reservationStatusList => {
  await api.put('/schedules/reservations/status', reservationStatusList);
};

export const fetchMyReservationSettings = async () => {
  const response = await api.get('/schedules/reservation/settings');
  return response.data;
};

export const fetchReservationSettings = async shopId => {
  const response = await api.get(`/schedules/reservation/settings/${shopId}`);
  return response.data;
};

export const updateReservationSettings = async settingsList => {
  return await api.put('/schedules/reservation/settings', settingsList);
};

export const searchCustomers = async keyword => {
  const response = await api.get('/customers/elasticsearch/search', {
    params: { keyword },
  });
  return response.data.data;
};

export const getActiveSecondaryItems = async () => {
  const response = await api.get('/secondary-items/active');
  return response.data.data;
};

export const getAllPrimaryItems = async () => {
  const response = await api.get('/primary-items');
  return response.data.data;
};

export const createReservation = async payload => {
  const response = await api.post('/schedules/reservations/shop', payload);
  return response.data.data;
};

export const createLeave = async payload => {
  const response = await api.post('/schedules/leaves', payload);
  return response.data.data;
};

export const createRegularLeave = async payload => {
  const response = await api.post('/schedules/regular-leaves', payload);
  return response.data.data;
};

export const createPlan = async payload => {
  const response = await api.post('/schedules/plans', payload);
  return response.data.data;
};

export const createRegularPlan = async payload => {
  const response = await api.post('/schedules/regular-plans', payload);
  return response.data.data;
};

export const updateLeave = (id, payload) => api.put(`/schedules/leaves/${id}`, payload);

export const updateRegularLeave = (id, payload) =>
  api.put(`/schedules/regular-leaves/${id}`, payload);

export const switchLeaveType = payload => api.post('/schedules/leaves/switch', payload);

export const updateReservation = (reservationId, payload) =>
  api.put(`/schedules/reservations/${reservationId}`, payload);
