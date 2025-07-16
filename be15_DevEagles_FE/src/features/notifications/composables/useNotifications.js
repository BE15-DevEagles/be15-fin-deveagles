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
    // ✨ [수정] 이제 로그인 상태는 watcher가 보장하므로, 여기서 중복 체크할 필요가 없습니다.
    if (isLoading.value) return;
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
    // ✨ [수정] 이중 체크를 제거하여 로직을 단순화합니다.
    if (!accessToken) return;

    if (eventSource && eventSource.readyState !== EventSource.CLOSED) {
      return; // 이미 연결되어 있으면 중복 실행 방지
    }

    const sseUrl = `${import.meta.env.VITE_API_BASE_URL}/notifications/subscribe?token=${accessToken}`;
    eventSource = new EventSource(sseUrl);

    eventSource.onopen = () => {
      isSseConnected.value = true;
    };
    eventSource.addEventListener('notification', event => {
      try {
        realtimeNotifications.value.unshift(JSON.parse(event.data));
        showToast('새로운 알림이 있습니다.');
      } catch (e) {
        console.error('SSE 데이터 파싱 오류', e);
      }
    });
    eventSource.onerror = () => {
      isSseConnected.value = false;
      if (eventSource) eventSource.close();
      // 재연결 시도
      setTimeout(() => {
        if (authStore.isAuthenticated) connect();
      }, 5000);
    };
  };

  const disconnect = () => {
    if (eventSource) {
      eventSource.close();
      eventSource = null;
    }
    // 로그아웃 또는 연결 종료 시 모든 알림 데이터를 깨끗하게 초기화합니다.
    historicalNotifications.value = [];
    realtimeNotifications.value = [];
    isSseConnected.value = false;
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

  /**
   * ✨ [핵심 최종 수정] '인증 초기화 여부'와 '로그인 여부' 두 가지 상태를 동시에 감시하는
   * 하나의 통합된 watcher를 사용합니다.
   */
  watch(
    // 감시할 대상을 배열로 묶어줍니다.
    [() => authStore.isInitialized, () => authStore.isAuthenticated],
    // 변경된 값을 배열로 받습니다.
    ([initialized, authenticated]) => {
      // 1. 아직 인증 상태 초기화가 끝나지 않았다면(사장님 출근 전), 아무것도 하지 않습니다.
      if (!initialized) {
        return;
      }

      // 2. 초기화가 끝났고, 사용자가 로그인 상태라면 알림 기능을 시작합니다.
      if (authenticated) {
        connect();
        fetchHistorical();
      }
      // 3. 초기화가 끝났지만, 사용자가 로그인 상태가 아니라면 모든 알림 기능을 종료하고 데이터를 정리합니다.
      else {
        disconnect();
      }
    },
    // immediate: true를 통해 컴포넌트가 처음 로드될 때도 이 로직이 실행되도록 합니다.
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
