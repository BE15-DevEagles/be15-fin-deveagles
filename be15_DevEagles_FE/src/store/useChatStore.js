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
  const isWaitingForResponse = ref(false);
  const isRoomLoading = ref(false); // ✅ 채팅방 로딩 상태
  const isListLoading = ref(false); // ✅ 채팅방 목록 로딩 상태
  const lastMessageTime = ref(0);
  const messageCooldown = 1000; // ✅ 1초 쿨다운

  // 알림 토스트 핸들링
  const toastQueue = ref([]);
  const isShowingToast = ref(false);
  const toastHandler = ref(null); // ✅ ref로 변경 (Vue 반응형 + 안정성)

  // 채팅 메시지 추가
  const addMessage = msg => {
    if (msg.from === 'bot' && msg.type !== 'loading') {
      const loadingMessages = messages.value.filter(m => m.type === 'loading');
      if (loadingMessages.length > 0) {
        messages.value = messages.value.filter(m => m.type !== 'loading');
      }
    }

    messages.value = [...messages.value, msg];
  };

  // 로딩 메시지 추가
  const addLoadingMessage = () => {
    // 기존 로딩 메시지가 있으면 추가하지 않음
    const existingLoading = messages.value.find(msg => msg.type === 'loading');
    if (existingLoading) {
      return existingLoading.id;
    }

    const loadingMessage = { type: 'loading', from: 'bot', id: 'loading-' + Date.now() };
    addMessage(loadingMessage);
    return loadingMessage.id;
  };

  // 로딩 메시지 제거
  const removeLoadingMessage = loadingId => {
    messages.value = messages.value.filter(msg => msg.id !== loadingId);
  };

  // 모든 로딩 메시지 제거
  const removeAllLoadingMessages = () => {
    const loadingMessages = messages.value.filter(msg => msg.type === 'loading');
    if (loadingMessages.length > 0) {
      messages.value = messages.value.filter(msg => msg.type !== 'loading');
    }
  };

  // 메시지 전송 가능 여부 확인
  const canSendMessage = () => {
    const now = Date.now();
    return now - lastMessageTime.value >= messageCooldown;
  };

  // 메시지 전송 시간 업데이트
  const updateMessageTime = () => {
    lastMessageTime.value = Date.now();
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
    // 로딩 메시지 확실히 제거
    removeAllLoadingMessages();

    messages.value = [];
    currentRoomId.value = null;
    currentUserId.value = null;
    subscribedRoomId.value = null;
    toastQueue.value = [];
    isShowingToast.value = false;
    isSubscribed.value = false;
    isAiActive.value = true;
    isWaitingForResponse.value = false;
    isRoomLoading.value = false;
    isListLoading.value = false;
    lastMessageTime.value = 0; // 쿨다운 초기화
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
    isWaitingForResponse,
    isRoomLoading,
    isListLoading,

    // 메서드
    addMessage,
    addLoadingMessage,
    removeLoadingMessage,
    removeAllLoadingMessages,
    canSendMessage,
    updateMessageTime,
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
    setWaitingForResponse: val => (isWaitingForResponse.value = val),
    setRoomLoading: val => (isRoomLoading.value = val),
    setListLoading: val => (isListLoading.value = val),
  };
});
