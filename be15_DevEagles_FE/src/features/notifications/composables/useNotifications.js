import { ref, computed, watch, onUnmounted } from 'vue';
import { useAuthStore } from '@/store/auth';
import {
  getMyNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead,
} from '@/features/notifications/api/notifications.js';
import { useToast } from '@/composables/useToast';

const historicalNotifications = ref([]);
const realtimeNotifications = ref([]);
const isLoading = ref(false);
const isSseConnected = ref(false);
let eventSource = null;

export function useNotifications() {
  const authStore = useAuthStore();
  const { showToast } = useToast();

  const allNotifications = computed(() => {
    const historicalIds = new Set(historicalNotifications.value.map(n => n.notificationId));
    const uniqueRealtime = realtimeNotifications.value.filter(
      n => !historicalIds.has(n.notificationId)
    );
    return [...uniqueRealtime, ...historicalNotifications.value].sort(
      (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
    );
  });

  const unreadCount = computed(() => {
    return allNotifications.value.filter(n => !n.read).length;
  });

  const fetchHistorical = async () => {
    if (isLoading.value || historicalNotifications.value.length > 0) return;
    isLoading.value = true;
    try {
      const response = await getMyNotifications({ page: 0, size: 10 });
      historicalNotifications.value = response.data.content || [];
    } catch (err) {
      console.error('과거 알림 조회 실패:', err);
    } finally {
      isLoading.value = false;
    }
  };

  const connect = () => {
    const accessToken = authStore.accessToken;
    if (!accessToken || (eventSource && eventSource.readyState !== EventSource.CLOSED)) {
      return;
    }
    const sseUrl = `${import.meta.env.VITE_API_BASE_URL}/notifications/subscribe?token=${accessToken}`;
    eventSource = new EventSource(sseUrl);

    eventSource.onopen = () => {
      isSseConnected.value = true;
    };
    eventSource.addEventListener('notification', event => {
      try {
        realtimeNotifications.value.unshift(JSON.parse(event.data));
        // 새로운 알림이 오면 토스트 메시지를 띄웁니다.
        showToast('새로운 알림이 있습니다.');
      } catch (e) {
        console.error('SSE 데이터 파싱 오류', e);
      }
    });
    eventSource.onerror = () => {
      isSseConnected.value = false;
      if (eventSource) eventSource.close();
      setTimeout(connect, 5000);
    };
  };

  const disconnect = () => {
    if (eventSource) {
      eventSource.close();
      eventSource = null;
    }
  };

  const handleMarkAsRead = async notification => {
    if (notification.read) return;
    try {
      await markNotificationAsRead(notification.notificationId);
      const targetNotif = allNotifications.value.find(
        n => n.notificationId === notification.notificationId
      );
      if (targetNotif) targetNotif.read = true;
    } catch (error) {
      console.error('알림 읽음 처리 실패', error);
    }
  };

  const handleMarkAllAsRead = async () => {
    if (unreadCount.value === 0) return;

    try {
      allNotifications.value.forEach(notification => {
        if (!notification.read) {
          notification.read = true;
        }
      });

      await markAllNotificationsAsRead();
    } catch (error) {
      console.error('모두 읽음 처리 실패:', error);
    }
  };

  watch(
    () => authStore.isAuthenticated,
    isAuth => {
      if (isAuth) {
        connect();
        fetchHistorical();
      } else {
        disconnect();
        historicalNotifications.value = [];
        realtimeNotifications.value = [];
        isSseConnected.value = false;
      }
    },
    { immediate: true }
  );

  onUnmounted(disconnect);

  return {
    allNotifications,
    unreadCount,
    isLoading,
    isSseConnected,
    handleMarkAsRead,
    handleMarkAllAsRead,
  };
}
