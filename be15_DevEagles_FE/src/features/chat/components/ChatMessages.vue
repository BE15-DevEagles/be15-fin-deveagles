<script setup>
  import { onUpdated, ref } from 'vue';
  import { marked } from 'marked';
  import LoadingWave from './LoadingWave.vue';

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

  // 마크다운을 HTML로 변환하는 함수
  function renderMarkdown(text) {
    // 메시지 끝의 개행문자 제거
    const cleanText = text.replace(/\n+$/, '');

    // 연속된 개행을 최대 1개로 제한
    const limitedNewlines = cleanText.replace(/\n{2,}/g, '\n\n');

    return marked(limitedNewlines);
  }
</script>

<template>
  <div ref="scrollContainer" class="chat-messages">
    <div v-for="(msg, index) in messages" :key="index" :class="['chat-message', messageClass(msg)]">
      <!-- 로딩 메시지 -->
      <template v-if="msg.type === 'loading'">
        <div class="message-bubble">
          <LoadingWave />
        </div>
      </template>

      <!-- 일반 텍스트 메시지 -->
      <template v-else-if="!msg.type || msg.type === 'text'">
        <div class="message-bubble" v-html="renderMarkdown(msg.text)"></div>
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

  /* 마크다운 렌더링된 HTML 요소 스타일 */
  .message-bubble :deep(p) {
    margin: 0;
  }

  .message-bubble :deep(p:not(:last-child)) {
    margin-bottom: 0.5rem;
  }

  .message-bubble :deep(code) {
    background-color: rgba(0, 0, 0, 0.1);
    padding: 0.2rem 0.3rem;
    border-radius: 4px;
    font-family: monospace;
    font-size: 0.9em;
  }

  .message-bubble :deep(pre) {
    background-color: rgba(0, 0, 0, 0.1);
    padding: 0.5rem;
    border-radius: 8px;
    overflow-x: auto;
    margin: 0.5rem 0;
  }

  .message-bubble :deep(pre code) {
    background-color: transparent;
    padding: 0;
  }

  .message-bubble :deep(strong) {
    font-weight: bold;
  }

  .message-bubble :deep(em) {
    font-style: italic;
  }

  .message-bubble :deep(ul),
  .message-bubble :deep(ol) {
    margin: 0.5rem 0;
    padding-left: 1.2rem;
  }

  .message-bubble :deep(li) {
    margin: 0.2rem 0;
  }

  .message-bubble :deep(blockquote) {
    border-left: 3px solid rgba(0, 0, 0, 0.2);
    padding-left: 0.8rem;
    margin: 0.5rem 0;
    font-style: italic;
  }

  .message-bubble :deep(h1),
  .message-bubble :deep(h2),
  .message-bubble :deep(h3),
  .message-bubble :deep(h4),
  .message-bubble :deep(h5),
  .message-bubble :deep(h6) {
    margin: 0.5rem 0 0.3rem 0;
    font-weight: bold;
  }

  .message-bubble :deep(a) {
    color: inherit;
    text-decoration: underline;
  }

  /* 봇 메시지의 코드 블록 스타일 조정 */
  .from-bot .message-bubble :deep(code) {
    background-color: rgba(0, 0, 0, 0.1);
  }

  .from-bot .message-bubble :deep(pre) {
    background-color: rgba(0, 0, 0, 0.1);
  }

  /* 내 메시지의 코드 블록 스타일 조정 */
  .from-me .message-bubble :deep(code) {
    background-color: rgba(255, 255, 255, 0.2);
  }

  .from-me .message-bubble :deep(pre) {
    background-color: rgba(255, 255, 255, 0.2);
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
