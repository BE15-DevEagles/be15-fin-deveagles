<script setup></script>

<template>
  <div id="app">
    <router-view />
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
    font-family: 'Noto Sans KR', sans-serif;
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
