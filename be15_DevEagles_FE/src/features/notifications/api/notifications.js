import api from '@/plugins/axios.js';

/**
 * 현재 로그인한 사용자의 알림 목록을 페이징하여 조회합니다.
 * shopId는 토큰에서 자동으로 인식되므로 파라미터에서 제거합니다.
 * @param {object} params - 페이징 정보 (page, size)
 * @returns {Promise} 페이징된 알림 목록 API 응답
 */
export const getMyNotifications = (params = { page: 0, size: 10 }) => {
  // GET /api/v1/notifications?page={page}&size={size}
  return api.get(`/notifications`, { params });
};

/**
 * 특정 알림을 읽음 상태로 변경합니다.
 * @param {number} notificationId - 읽음 처리할 알림의 ID
 * @returns {Promise} API 응답
 */
export const markNotificationAsRead = notificationId => {
  return api.patch(`/notifications/${notificationId}/read`);
};

export const markAllNotificationsAsRead = () => {
  return api.put('/notifications/read-all');
};
