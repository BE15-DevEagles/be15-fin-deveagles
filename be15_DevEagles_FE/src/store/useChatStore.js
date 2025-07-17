import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useChatStore = defineStore('chat', () => {
  // 채팅 상태
  const messages = ref([]);
  const isChatModalOpen = ref(false);
  const currentRoomId = ref(null);
  const currentUserId = ref(null);
  const subscribedRoomId = ref(null);
  const isSubscribed = ref(false); // ✅ 중복 구독 방지용
  const isAiActive = ref(true); // ✅ AI 상태 여부

  // 알림 토스트 핸들링
  const toastQueue = ref([]);
  const isShowingToast = ref(false);
  const toastHandler = ref(null); // ✅ ref로 변경 (Vue 반응형 + 안정성)

  // 채팅 메시지 추가
  const addMessage = msg => {
    messages.value = [...messages.value, msg];
  };

  // 핸들러 등록
  const setToastHandler = handlerFn => {
    toastHandler.value = handlerFn;
  };

  // 알림 트리거
  const triggerToast = msg => {
    toastQueue.value.push(msg);
    processToastQueue();
  };

  // 큐를 처리하는 내부 로직
  const processToastQueue = async () => {
    if (isShowingToast.value || toastQueue.value.length === 0 || !toastHandler.value) return;

    isShowingToast.value = true;
    const msg = toastQueue.value.shift();

    try {
      await toastHandler.value(msg); // 반드시 Promise 반환
    } catch (e) {
      console.warn('❗ 알림 표시 중 오류', e);
    } finally {
      isShowingToast.value = false;
      processToastQueue(); // 다음 메시지 자동 처리
    }
  };

  // 상태 초기화 (toastHandler 유지)
  const resetChatState = () => {
    messages.value = [];
    currentRoomId.value = null;
    currentUserId.value = null;
    subscribedRoomId.value = null;
    toastQueue.value = [];
    isShowingToast.value = false;
    isSubscribed.value = false;
    isAiActive.value = true;
    // ❌ toastHandler.value = null; 절대 초기화하지 않음!
  };

  return {
    // 상태
    messages,
    isChatModalOpen,
    currentRoomId,
    currentUserId,
    subscribedRoomId,
    isSubscribed,
    isAiActive,

    // 메서드
    addMessage,
    setToastHandler,
    triggerToast,
    resetChatState,
    clearMessages: () => (messages.value = []),

    // Setter 유틸
    setChatModalOpen: val => (isChatModalOpen.value = val),
    setCurrentRoomId: id => (currentRoomId.value = id),
    setCurrentUserId: id => (currentUserId.value = id),
    setSubscribedRoomId: id => (subscribedRoomId.value = id),
    setIsSubscribed: val => (isSubscribed.value = val),
    setAiActive: val => (isAiActive.value = val),
  };
});
