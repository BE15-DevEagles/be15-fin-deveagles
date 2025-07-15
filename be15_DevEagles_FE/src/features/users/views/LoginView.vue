<template>
  <div class="login-container input-reset-scope">
    <form :class="['login-box', { shake }]" @submit.prevent="fetchUser">
      <img :src="Logo" alt="로고" class="logo" />

      <BaseForm
        v-model="params.loginId"
        label="아이디"
        type="text"
        placeholder="아이디를 입력해주세요."
        :error="errors.loginId"
        autocomplete="username"
        @focus="clearError('loginId')"
      />

      <BaseForm
        v-model="params.password"
        label="비밀번호"
        type="password"
        placeholder="비밀번호를 입력해주세요."
        :error="errors.password"
        autocomplete="current-password"
        @focus="clearError('password')"
      />

      <div class="login-links">
        <a href="#" @click.prevent="showFindPwdModal = true">비밀번호 초기화</a>
      </div>

      <div class="login-buttons">
        <a class="btn btn-primary" @click.prevent="goToSignup">회원가입</a>
        <button class="btn btn-primary" type="submit">로그인</button>
      </div>
    </form>
  </div>

  <!-- 비밀번호 초기화 -->
  <FindPwdModal v-model="showFindPwdModal" @submit="onFindPwdSubmit" />
  <FindPwdResModal v-model:show="showFindPwdResModal" :found-user-pwd="foundUserPwd" />

  <BaseToast ref="toastRef" />
</template>
<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { onMounted, ref } from 'vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import Logo from '@/images/logo_name_navy.png';
  import FindPwdModal from '@/features/users/components/FindPwdModal.vue';
  import FindPwdResModal from '@/features/users/components/FindPwdResModal.vue';
  import { login, checkEmail } from '@/features/users/api/users.js';
  import BaseToast from '@/components/common/BaseToast.vue';
  import { useAuthStore } from '@/store/auth.js';

  const router = useRouter();
  const authStore = useAuthStore();
  const toastRef = ref();
  const params = ref({
    loginId: '',
    password: '',
  });

  const errors = ref({
    loginId: '',
    password: '',
  });

  const clearError = field => {
    errors.value[field] = '';
  };

  const shake = ref(false);
  const showFindPwdModal = ref(false);
  const showFindPwdResModal = ref(false);
  const foundUserPwd = ref();

  const onFindPwdSubmit = async ({ staffName, email }) => {
    try {
      await checkEmail({ staffName, email });
      showFindPwdModal.value = false;
      foundUserPwd.value = true;
      showFindPwdResModal.value = true;
    } catch (err) {
      const res = err.response?.data;
      if (!res) {
        toastRef.value?.error?.('존재하지 않는 회원입니다.');
        return;
      }
      if (!res.message) {
        toastRef.value?.error?.('존재하지 않는 회원입니다.');
      } else {
        toastRef.value?.error?.(res.message);
      }
    } finally {
      staffName.value = null;
      email.value = null;
    }
  };

  const fetchUser = async () => {
    try {
      const res = await login(params.value);

      await authStore.setAuth(res.data.data.accessToken);
      await router.push('/');
    } catch (err) {
      shake.value = true;

      const res = err.response?.data;
      if (!res) {
        toastRef.value?.error?.('로그인 중 오류가 발생했습니다.');
        return;
      }
      if (!res.message) {
        toastRef.value?.error?.('존재하지 않는 회원입니다.');
      } else {
        if (res.errorCode === '11001') {
          errors.value.loginId = res.message;
          params.value.password = '';
          params.value.loginId = '';
        } else if (res.errorCode === '11002') {
          errors.value.password = res.message;
          params.value.password = '';
        }
      }
    }
  };

  const goToSignup = () => {
    router.push('/sign-up');
  };

  onMounted(() => {
    localStorage.removeItem('accessToken');
  });
</script>
<style scoped>
  .login-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    background-color: var(--color-gray-50);
  }

  .login-box {
    background-color: var(--color-neutral-white);
    border: 1px solid var(--color-gray-200);
    border-radius: 8px;
    padding: 40px;
    width: 400px;
    box-shadow: var(--shadow-drop);
    transition: transform 0.2s ease-in-out;
  }

  .logo {
    display: block;
    margin: 0 auto 24px;
    height: 300px;
  }

  .login-links {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    font-size: 12px;
    color: var(--color-gray-600);
    margin-top: 8px;
    margin-bottom: 24px;
  }

  .login-links a {
    color: var(--color-gray-700);
    text-decoration: none;
  }

  .login-buttons {
    display: flex;
    justify-content: space-between;
    gap: 12px;
  }

  .modal-text {
    text-align: center;
    font-size: 16px;
    font-weight: 700;
    line-height: 1.5;
    padding: 16px 0;
  }

  .modal-footer-buttons {
    display: flex;
    justify-content: center;
    gap: 12px;
  }
</style>
