<template>
  <div class="chat-room-loader">
    <div class="spinner-container">
      <div class="spinner"></div>
      <p class="loading-text">{{ loadingText }}</p>
    </div>
  </div>
</template>

<script setup>
  defineProps({
    loadingText: { type: String, default: '불러오는 중...' },
  });
</script>

<style scoped>
  .chat-room-loader {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    min-height: 200px;
    background-color: #f7f9fc;
  }

  .spinner-container {
    text-align: center;
  }

  .spinner {
    width: 40px;
    height: 40px;
    border: 4px solid #e0e0e0;
    border-top: 4px solid var(--color-primary-main);
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin: 0 auto 1rem;
  }

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(360deg);
    }
  }

  .loading-text {
    color: var(--color-gray-600);
    font-size: 14px;
    margin: 0;
  }
</style>
