import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import api from '@/plugins/axios.js';

function decodeJwtPayload(token) {
  const payload = token.split('.')[1];
  const decoded = atob(payload);
  const utf8Payload = new TextDecoder().decode(Uint8Array.from(decoded, c => c.charCodeAt(0)));
  return JSON.parse(utf8Payload);
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(null);
  const userId = ref(null);
  const expirationTime = ref(null);

  const shopId = ref(null);
  const username = ref(null);
  const staffName = ref(null);
  const profileUrl = ref(null);
  const userStatus = ref(null);
  const grade = ref(null);

  const isInitialized = ref(false);

  const isAuthenticated = computed(
    () => !!accessToken.value && Date.now() < (expirationTime.value || 0)
  );

  async function setAuth(at) {
    accessToken.value = at;
    try {
      const payload = decodeJwtPayload(at);
      shopId.value = payload.shopId;
      userId.value = payload.userId;
      username.value = payload.username;
      staffName.value = payload.staffName || null;
      grade.value = payload.grade;
      profileUrl.value = payload.profileUrl || null;
      expirationTime.value = payload.exp * 1000;
      userStatus.value = payload.userStatus;

      localStorage.setItem('accessToken', at);
    } catch (e) {
      clearAuth();
    }
  }

  function clearAuth() {
    // 인증 정보 삭제
    accessToken.value = null;
    shopId.value = null;
    userId.value = null;
    username.value = null;
    staffName.value = null;
    profileUrl.value = null;
    expirationTime.value = null;
    userStatus.value = null;

    localStorage.removeItem('accessToken');
    delete api.defaults.headers.common['Authorization'];
    console.log('[Auth] 인증 정보 삭제 완료');
  }

  async function initAuth() {
    const token = localStorage.getItem('accessToken');
    if (token) {
      await setAuth(token);
    }
    isInitialized.value = true;
  }

  return {
    accessToken,
    userId,
    shopId,
    username,
    staffName,
    grade,
    profileUrl,
    expirationTime,
    isAuthenticated,
    isInitialized,
    setAuth,
    clearAuth,
    initAuth,
  };
});
