<script setup>
  import { ref, onMounted, onUnmounted } from 'vue';
  import { useAuthStore } from '@/store/auth.js';
  import BaseToast from '@/components/common/BaseToast.vue';
  import { toastBus } from '@/composables/useToast';

  const authStore = useAuthStore();
  const toastRef = ref(null);

  /**
   * ✨ 3. 'show-toast'라는 글로벌 이벤트가 발생했을 때 실행될 리스너 함수입니다.
   * 이 함수는 BaseToast 컴포넌트의 메서드를 직접 호출하여 토스트를 화면에 표시합니다.
   * @param {object} payload - { message, type, options }
   */
  const onShowToast = payload => {
    const { message, type = 'success', options = {} } = payload;
    if (toastRef.value && typeof toastRef.value[type] === 'function') {
      toastRef.value[type](message, options);
    }
  };

  onMounted(async () => {
    await authStore.initAuth(); // 기존 인증 초기화 로직
    // ✨ 4. App.vue 컴포넌트가 생성될 때, 글로벌 토스트 이벤트 수신을 시작합니다.
    toastBus.on('show-toast', onShowToast);
  });

  onUnmounted(() => {
    // ✨ 5. App.vue 컴포넌트가 소멸될 때, 메모리 누수를 방지하기 위해 이벤트 수신을 중단합니다.
    toastBus.off('show-toast', onShowToast);
  });
</script>

<template>
  <div id="app">
    <router-view />

    <!--
      ✨ 6. 앱 전체에서 단 하나만 존재하는 BaseToast 컴포넌트를 설치합니다.
      ref="toastRef"를 통해 스크립트에서 이 컴포넌트의 메서드를 호출할 수 있게 됩니다.
    -->
    <BaseToast ref="toastRef" />
  </div>
</template>

<style>
  /* 스타일가이드와 컴포넌트 CSS import */
  @import './assets/css/styleguide.css';
  @import './assets/css/components.css';

  /* 기본 앱 스타일 */
  * {
    box-sizing: border-box;
  }

  html,
  body {
    margin: 0;
    padding: 0;
    height: 100%;
    font-family:
      'Noto Sans KR',
      -apple-system,
      BlinkMacSystemFont,
      'Segoe UI',
      sans-serif;
    background-color: var(--color-gray-50);
  }

  #app {
    min-height: 100vh;
    overflow: hidden;
  }

  /* 스크롤바 스타일링 */
  ::-webkit-scrollbar {
    width: 6px;
  }

  ::-webkit-scrollbar-track {
    background: var(--color-gray-100);
  }

  ::-webkit-scrollbar-thumb {
    background: var(--color-gray-400);
    border-radius: 3px;
  }

  ::-webkit-scrollbar-thumb:hover {
    background: var(--color-gray-500);
  }

  /* 링크 기본 스타일 */
  a {
    color: var(--color-primary-main);
    text-decoration: none;
  }

  a:hover {
    text-decoration: underline;
  }

  /* 버튼 기본 스타일 리셋 */
  button {
    border: none;
    background: none;
    cursor: pointer;
    font-family: inherit;
  }

  /* 입력 요소 기본 스타일 */
  input,
  select,
  textarea {
    font-family: inherit;
  }

  /* 포커스 아웃라인 스타일 */
  *:focus {
    outline: 2px solid var(--color-primary-main);
    outline-offset: 2px;
  }

  *:focus:not(:focus-visible) {
    outline: none;
  }
</style>
