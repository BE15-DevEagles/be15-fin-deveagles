<template>
  <form class="form-box" @submit.prevent="handleSubmit">
    <BaseForm
      v-model="form.password"
      type="password"
      label="변경 비밀번호"
      placeholder="특수문자, 영문자, 숫자를 포함한 8자리 이상"
      :error="errors.password"
    />
    <BaseForm
      v-model="form.confirmPassword"
      type="password"
      label="비밀번호 확인"
      placeholder="비밀번호 확인 입력"
      :error="errors.confirmPassword"
    />

    <div class="button-group">
      <BaseButton type="primary" html-type="submit">확인</BaseButton>
      <BaseButton type="secondary" outline @click="handleCancel">취소</BaseButton>
    </div>

    <BaseModal v-model="isSuccessModalOpen" title="">
      <template #default>
        <p style="text-align: center; font-weight: 600">비밀번호 변경이 완료되었습니다.</p>
      </template>
      <template #footer>
        <div style="display: flex; justify-content: center">
          <BaseButton type="primary" @click="handleSuccessConfirm">확인</BaseButton>
        </div>
      </template>
    </BaseModal>
  </form>
</template>

<script setup>
  import { reactive, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import { patchPwd } from '@/features/users/api/users.js';

  const route = useRoute();
  const email = route.query.email;
  const props = defineProps({
    onCancel: {
      type: Function,
      default: () => {},
    },
    redirectPath: {
      type: String,
      default: '/login',
    },
  });

  const form = reactive({
    password: '',
    confirmPassword: '',
  });

  const errors = reactive({
    password: '',
    confirmPassword: '',
  });

  const isSuccessModalOpen = ref(false);
  const router = useRouter();

  const validate = () => {
    let valid = true;
    errors.password = '';
    errors.confirmPassword = '';

    const passwordRegex = new RegExp(
      '^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+{}\\[\\]|;:\'",.<>?/]).{8,}$'
    );

    if (!form.password) {
      errors.password = '비밀번호를 입력해주세요.';
      valid = false;
    } else if (!passwordRegex.test(form.password)) {
      errors.password = '비밀번호는 영문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.';
      valid = false;
    }

    if (!form.confirmPassword) {
      errors.confirmPassword = '비밀번호 확인을 입력해주세요.';
      valid = false;
    } else if (form.confirmPassword !== form.password) {
      errors.confirmPassword = '비밀번호가 일치하지 않습니다.';
      valid = false;
    }

    return valid;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    try {
      await patchPwd({
        email: email,
        password: form.password,
      });

      isSuccessModalOpen.value = true;
    } catch (e) {
      console.error(e);
    }
  };

  const handleCancel = () => {
    props.onCancel?.();
  };

  const handleSuccessConfirm = () => {
    isSuccessModalOpen.value = false;
    router.push(props.redirectPath);
  };
</script>

<style scoped>
  .form-box {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .button-group {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-top: 10px;
  }
</style>
