<template>
  <div class="loading-wave">
    <div class="wave-dot"></div>
    <div class="wave-dot"></div>
    <div class="wave-dot"></div>
  </div>
</template>

<style scoped>
  .loading-wave {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 0.2rem 0;
  }

  .wave-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: #999;
    animation: wave 1.4s ease-in-out infinite;
  }

  .wave-dot:nth-child(1) {
    animation-delay: -0.32s;
  }

  .wave-dot:nth-child(2) {
    animation-delay: -0.16s;
  }

  .wave-dot:nth-child(3) {
    animation-delay: 0s;
  }

  @keyframes wave {
    0%,
    60%,
    100% {
      transform: translateY(0);
      opacity: 0.4;
    }
    30% {
      transform: translateY(-10px);
      opacity: 1;
    }
  }

  /* 봇 메시지 버블 내에서의 스타일 조정 */
  .from-bot .loading-wave .wave-dot {
    background-color: #666;
  }
</style>
