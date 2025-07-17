<script setup>
  import { onUpdated, ref } from 'vue';

  defineProps({ messages: Array });
  const emit = defineEmits(['switch']);
  const scrollContainer = ref(null);

  onUpdated(() => {
    if (scrollContainer.value) {
      scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight;
    }
  });

  // ✅ from 값 기준으로 클래스 리턴
  function messageClass(msg) {
    if (msg.from === 'me') return 'from-me';
    if (msg.from === 'user') return 'from-user';
    return 'from-bot';
  }
</script>

<template>
  <div ref="scrollContainer" class="chat-messages">
    <div v-for="(msg, index) in messages" :key="index" :class="['chat-message', messageClass(msg)]">
      <!-- 일반 텍스트 메시지 -->
      <template v-if="!msg.type || msg.type === 'text'">
        <div class="message-bubble">{{ msg.text }}</div>
      </template>

      <!-- 상담사 전환 버튼 메시지 (❌ 이제 사용 안 함) -->
      <!-- 제거 또는 무시 처리 가능 -->
    </div>
  </div>
</template>

<style scoped>
  .chat-messages {
    padding: 0.5rem 0;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .chat-message {
    display: flex;
  }

  .chat-message.from-me {
    justify-content: flex-end;
  }

  .chat-message.from-user,
  .chat-message.from-bot {
    justify-content: flex-start;
  }

  .message-bubble {
    max-width: 70%;
    padding: 0.6rem 0.9rem;
    border-radius: 16px;
    font-size: 14px;
    line-height: 1.4;
  }

  .from-me .message-bubble {
    background-color: var(--color-primary-main);
    color: white;
    border-bottom-right-radius: 0;
  }

  .from-user .message-bubble,
  .from-bot .message-bubble {
    background-color: #e0e0e0;
    color: black;
    border-bottom-left-radius: 0;
  }
</style>
