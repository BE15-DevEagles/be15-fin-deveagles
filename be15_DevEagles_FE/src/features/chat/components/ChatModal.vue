<script setup>
  import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue';

  import ChatMessages from './ChatMessages.vue';
  import ChatInput from './ChatInput.vue';
  import ChatListView from './ChatListView.vue';
  import ListIcon from '@/components/icons/ListIcon.vue';
  import HomeIcon from '@/components/icons/HomeIcon.vue';

  import { safeSubscribeToRoom, sendSocketMessage } from '@/features/chat/composables/socket.js';

  import {
    createChatRoom,
    sendGreetingMessage,
    switchToStaff,
    getChatMessages,
  } from '@/features/chat/api/chat.js';

  import { useAuthStore } from '@/store/auth.js';
  import { useChatStore } from '@/store/useChatStore.js';

  const emit = defineEmits(['close']);
  const auth = useAuthStore();
  const chatStore = useChatStore();

  const currentView = ref('home');
  const containerRef = ref(null);
  const scrollArea = ref(null);

  function handleClickOutside(event) {
    if (containerRef.value && !containerRef.value.contains(event.target)) {
      emit('close');
    }
  }

  onMounted(() => {
    chatStore.setChatModalOpen(true);
    document.addEventListener('mousedown', handleClickOutside);
  });

  onUnmounted(() => {
    chatStore.resetChatState();
    document.removeEventListener('mousedown', handleClickOutside);
  });

  watch(
    () => chatStore.messages,
    () => {
      nextTick(() => {
        if (scrollArea.value) {
          scrollArea.value.scrollTop = scrollArea.value.scrollHeight;
        }
      });
    },
    { deep: true }
  );

  async function openNewChat() {
    try {
      const res = await createChatRoom();
      const roomId = res.data.roomId;

      chatStore.setCurrentRoomId(roomId);
      chatStore.clearMessages();
      currentView.value = 'chat';

      if (chatStore.subscribedRoomId !== roomId) {
        safeSubscribeToRoom(roomId, msg => {
          const from =
            String(msg.senderId) === String(auth.userId) ? 'me' : msg.isCustomer ? 'user' : 'bot';
          chatStore.addMessage({ from, text: msg.content });
        });
        chatStore.setSubscribedRoomId(roomId);
      }

      await sendGreetingMessage(roomId);
      chatStore.setAiActive(true);
    } catch (e) {
      console.error('❌ 채팅방 생성 실패:', e);
    }
  }

  function handleSend(text) {
    const isStaff = auth.userId === 17;
    const msg = {
      roomId: chatStore.currentRoomId,
      senderId: auth.userId,
      senderName: auth.username ?? auth.staffName,
      content: text,
      isCustomer: !isStaff,
    };
    sendSocketMessage(chatStore.currentRoomId, msg);
  }

  async function handleSwitch() {
    try {
      await switchToStaff(chatStore.currentRoomId);
      chatStore.setAiActive(false);
      chatStore.addMessage({
        from: 'bot',
        text: '상담사에게 연결되었어요. 잠시만 기다려주세요.',
      });
    } catch (err) {
      chatStore.addMessage({
        from: 'bot',
        text: '상담사 전환에 실패했어요. 나중에 다시 시도해주세요.',
      });
    }
  }

  function openList() {
    currentView.value = 'list';
  }

  function goHome() {
    currentView.value = 'home';
    chatStore.resetChatState();
  }

  async function enterExistingChat(chatRoomId) {
    try {
      chatStore.setCurrentRoomId(chatRoomId);
      chatStore.clearMessages();
      currentView.value = 'chat';

      if (chatStore.subscribedRoomId !== chatRoomId) {
        safeSubscribeToRoom(chatRoomId, msg => {
          const from =
            String(msg.senderId) === String(auth.userId) ? 'me' : msg.isCustomer ? 'user' : 'bot';
          chatStore.addMessage({ from, text: msg.content });
        });
        chatStore.setSubscribedRoomId(chatRoomId);
      }

      const res = await getChatMessages(chatRoomId);
      res.data.forEach(msg => {
        const from =
          String(msg.senderId) === String(auth.userId) ? 'me' : msg.isCustomer ? 'user' : 'bot';
        chatStore.addMessage({ from, text: msg.content });
      });

      await nextTick();
    } catch (e) {
      console.error('❌ 기존 채팅방 입장 실패:', e);
      alert('기존 채팅방 입장에 실패했습니다.');
    }
  }
</script>

<template>
  <div ref="containerRef" class="chat-widget-panel">
    <div class="chat-modal-header">
      <img src="@/images/logo_positive.png" class="chat-modal-logo" alt="Beautifly 로고" />
      <div class="chat-modal-header-text">
        <p class="chat-modal-title">Beautifly 상담센터</p>
        <p class="chat-modal-subtitle">운영시간 평일 10:00 ~ 18:00</p>
      </div>
      <div class="chat-modal-actions">
        <button
          v-if="currentView === 'chat' && chatStore.isAiActive"
          class="switch-button"
          @click="handleSwitch"
        >
          상담사 전환
        </button>
        <button class="chat-modal-close" @click="$emit('close')">✖</button>
      </div>
    </div>

    <div ref="scrollArea" class="chat-modal-body">
      <div v-if="currentView === 'home'">
        <p class="chat-greeting">안녕하세요 😊 Beautifly 상담센터입니다.</p>
        <div class="home-action">
          <button class="home-new-button" @click="openNewChat">새 문의하기</button>
        </div>
      </div>
      <ChatMessages
        v-else-if="currentView === 'chat'"
        :messages="chatStore.messages"
        @switch="handleSwitch"
      />
      <ChatListView v-else-if="currentView === 'list'" @select="enterExistingChat" />
    </div>

    <div class="chat-modal-footer">
      <ChatInput v-if="currentView === 'chat'" @send="handleSend" />
      <div class="chat-footer-buttons">
        <div class="chat-footer-half">
          <button class="chat-btn" title="대화 목록" @click="openList">
            <ListIcon :size="24" />
          </button>
        </div>
        <div class="chat-footer-half">
          <button class="chat-btn" title="홈으로" @click="goHome">
            <HomeIcon :size="24" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
  .chat-widget-panel {
    width: 360px;
    height: 520px;
    background-color: #fff;
    border-radius: 1rem;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    position: fixed;
    bottom: 60px;
    right: 24px;
    z-index: 9999;
  }

  .chat-modal-header {
    background-color: var(--color-primary-main);
    color: white;
    padding: 1rem;
    display: flex;
    align-items: center;
    gap: 0.75rem;
    border-top-left-radius: 1rem;
    border-top-right-radius: 1rem;
  }

  .chat-modal-logo {
    width: 20px;
    height: 20px;
  }

  .chat-modal-header-text {
    display: flex;
    flex-direction: column;
  }

  .chat-modal-title {
    font-size: 1rem;
    font-weight: bold;
  }

  .chat-modal-subtitle {
    font-size: 0.75rem;
    opacity: 0.85;
  }

  .chat-modal-actions {
    margin-left: auto;
    display: flex;
    gap: 0.5rem;
    align-items: center;
  }

  .switch-button {
    padding: 0.3rem 0.8rem;
    background-color: white;
    color: var(--color-primary-main);
    font-weight: bold;
    border: none;
    border-radius: 8px;
    font-size: 13px;
    cursor: pointer;
  }

  .chat-modal-close {
    font-size: 1.2rem;
    background: none;
    border: none;
    color: white;
    cursor: pointer;
  }

  .chat-modal-body {
    flex: 1;
    overflow-y: auto;
    background-color: #f7f9fc;
    padding: 1rem;
    padding-bottom: 12px;
  }

  .chat-modal-footer {
    border-top: 1px solid #e0e0e0;
    padding: 0.75rem 1rem;
    background-color: #fff;
  }

  .chat-footer-buttons {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    margin-top: 0.5rem;
  }

  .chat-footer-half {
    flex: 1;
    display: flex;
    justify-content: center;
  }

  .chat-btn {
    background: none;
    border: none;
    cursor: pointer;
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .chat-btn :deep(svg) {
    color: var(--color-gray-600);
    transition: color 0.2s ease;
  }

  .chat-btn:hover :deep(svg) {
    color: var(--color-primary-main);
  }

  .chat-greeting {
    font-size: 15px;
    padding: 2rem 0 1rem;
    text-align: center;
    color: var(--color-gray-700);
  }

  .home-action {
    text-align: center;
    margin-top: 1rem;
  }

  .home-new-button {
    background-color: var(--color-primary-main);
    color: white;
    padding: 0.5rem 1rem;
    border-radius: 8px;
    font-weight: bold;
    cursor: pointer;
    font-size: 14px;
    border: none;
  }
</style>
