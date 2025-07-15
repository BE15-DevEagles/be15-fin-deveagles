<template>
  <div class="mypage-container">
    <div v-if="isLoading">로딩 중 ...</div>

    <form v-else class="profile-card-vertical">
      <div class="form-fields">
        <div class="label-row">
          <label for="email">아이디</label>
        </div>
        <BaseForm id="loginId" v-model="staff.loginId" type="loginId" disabled />

        <div class="label-row">
          <label for="email">이메일 주소</label>
        </div>
        <BaseForm
          id="email"
          v-model="staff.email"
          type="email"
          :error="errors.email"
          autocomplete="email"
          @blur="emailChecked"
          @focus="clearError('email')"
        />

        <div class="label-row">
          <label for="phoneNumber"> 전화번호 </label>
          <p class="inform-rule">'-' 제외, 빈 값 입력 시 변경사항이 저장되지 않습니다.</p>
        </div>
        <BaseForm
          id="phoneNumber"
          v-model="staff.phoneNumber"
          type="text"
          :error="errors.phoneNumber"
          autocomplete="phoneNumber"
          @blur="phoneChecked"
          @focus="clearError('phoneNumber')"
        />

        <div class="label-row">
          <label for="newPassword">변경할 비밀번호</label>
          <p class="inform-rule">특수문자, 영문자, 숫자를 포함한 8자리 이상</p>
        </div>
        <BaseForm
          id="newPassword"
          v-model="staff.newPassword"
          type="password"
          placeholder="변경할 비밀번호를 입력해주세요."
          :error="errors.password"
          autocomplete="new-password"
          @blur="passwordChecked"
          @focus="clearError('password')"
        />

        <div class="label-row">
          <label for="confirmPassword">비밀번호 확인</label>
        </div>
        <BaseForm
          id="checkPwd"
          v-model="staff.checkPwd"
          type="password"
          placeholder="비밀번호를 한 번 더 입력해주세요."
          :error="errors.checkPwd"
          autocomplete="new-password"
          @blur="checkPwdChecked"
          @focus="clearError('checkPwd')"
        />

        <div class="button-row">
          <BaseButton class="edit-button" type="primary" @click="handleEdit">
            변경사항 저장
          </BaseButton>
        </div>
      </div>
    </form>
    <BaseToast ref="toastRef" />
  </div>
</template>
<script setup>
  import { onMounted, ref } from 'vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import { getAccount, patchAccount, validEmail } from '@/features/users/api/users.js';
  import BaseToast from '@/components/common/BaseToast.vue';
  import { useAuthStore } from '@/store/auth.js';

  const authStore = useAuthStore();

  const toastRef = ref();
  const isLoading = ref(true);

  const originalStaff = ref({});
  const staff = ref({
    loginId: '',
    email: '',
    newPassword: '',
    checkPwd: '',
    phoneNumber: '',
  });

  const errors = ref({
    email: '',
    password: '',
  });

  const emailCheckPassed = ref(false);
  const passwordPassed = ref(false);
  const checkPwdPassed = ref(false);
  const phonePassed = ref(false);

  onMounted(async () => {
    const staffId = authStore.userId;

    try {
      const res = await getAccount({ staffId });
      const { email, phoneNumber } = res.data.data;

      originalStaff.value = { email, phoneNumber };
      staff.value = {
        loginId: authStore.username,
        email,
        phoneNumber,
        newPassword: '',
        checkPwd: '',
      };
    } catch (err) {
      handleError(err);
    } finally {
      isLoading.value = false;
    }
  });

  const handleEdit = async () => {
    // todo : userStore.staffId;
    const staffId = 1;

    const params = {
      staffId,
      email: staff.value.email !== originalStaff.value.email ? staff.value.email : '',
      phoneNumber:
        staff.value.phoneNumber !== originalStaff.value.phoneNumber
          ? staff.value.phoneNumber
          : originalStaff.value.phoneNumber,
      password: staff.value.newPassword ? staff.value.newPassword : '',
    };

    if (staff.value.newPassword && staff.value.newPassword !== staff.value.checkPwd) {
      toastRef.value?.error?.('비밀번호가 일치하지 않습니다.');
      return;
    } else if (!staff.value.newPassword) {
      staff.value.checkPwd = '';
    }

    try {
      await patchAccount(params);
      toastRef.value?.success?.('변경사항이 저장되었습니다.');
      originalStaff.value.email = staff.value.email;
      originalStaff.value.phoneNumber = staff.value.phoneNumber;
      staff.value.newPassword = '';
      staff.value.checkPwd = '';
    } catch (err) {
      handleError(err);
    }
  };

  const handleError = err => {
    const res = err.response?.data;
    if (!res || !res.message) {
      toastRef.value?.error?.('존재하지 않는 회원입니다');
      return;
    }
    toastRef.value?.error?.(res.message);
  };

  const clearError = field => {
    errors.value[field] = '';
  };

  const emailChecked = async () => {
    if (!staff.value.email || staff.value.email === originalStaff.value.email) return;

    const checkItem = staff.value.email?.trim();
    const isValidEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(checkItem);
    if (!isValidEmail) {
      errors.value.email = '올바른 이메일 형식을 입력해주세요.';
      emailCheckPassed.value = false;
      return;
    }

    try {
      const res = await validEmail({ checkItem });
      if (res.data.data === true) {
        errors.value.email = '';
        emailCheckPassed.value = true;
      } else {
        errors.value.email = '이미 사용 중인 이메일입니다.';
        emailCheckPassed.value = false;
      }
    } catch (err) {
      errors.value.email = '중복 확인 중 오류가 발생했습니다.';
      emailCheckPassed.value = false;
    }
  };

  const passwordChecked = () => {
    if (!staff.value.newPassword) return;
    const password = staff.value.newPassword;

    const isValidPwd =
      /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\\[\]{};':"\\|,.<>/?]).{8,}$/.test(password);
    if (!isValidPwd) {
      errors.value.password = '올바른 비밀번호 형식을 입력해주세요.';
      passwordPassed.value = true;
      return;
    }
    errors.value.password = '';
    passwordPassed.value = true;
  };

  const checkPwdChecked = () => {
    if (!staff.value.newPassword) {
      clearError('checkPwd');
      return;
    }

    const checkPwd = staff.value.checkPwd?.trim();
    const password = staff.value.newPassword;

    if (!checkPwd) {
      errors.value.checkPwd = '비밀번호를 한 번 더 입력해주세요.';
      checkPwdPassed.value = false;
      return;
    }

    if (checkPwd !== password) {
      errors.value.checkPwd = '비밀번호가 일치하지 않습니다.';
      checkPwdPassed.value = false;
      return;
    }
    errors.value.checkPwd = '';
    checkPwdPassed.value = true;
  };

  const phoneChecked = () => {
    if (!staff.value.phoneNumber || staff.value.phoneNumber === originalStaff.value.phoneNumber)
      return;

    const phoneNumber = staff.value.phoneNumber;

    const isValidPhone = /^[0-9]+$/.test(phoneNumber);

    if (!isValidPhone) {
      errors.value.phoneNumber = '올바른 전화번호 형식을 입력해주세요.';
      phonePassed.value = false;
      return;
    }

    if (phoneNumber.length === 11) {
      errors.value.phoneNumber = '';
      phonePassed.value = true;
    } else {
      errors.value.phoneNumber = '전화번호는 11자리 숫자만 입력 가능합니다.';
      phonePassed.value = false;
    }
  };
</script>
<style scoped>
  .profile-card-vertical {
    display: flex;
    flex-direction: column;
    align-items: center;
    border: 1px solid var(--color-gray-200);
    padding: 40px 24px;
    border-radius: 16px;
    background-color: #fff;
    width: 100%;
    max-width: 400px;
    margin: 0 auto;
    font-family: 'Noto Sans KR', sans-serif;
    color: var(--color-gray-800);
  }
  .form-fields {
    width: 360px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .label-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 4px;
    font-weight: 600;
    font-size: 14px;
    color: var(--color-gray-900);
  }
  .edit-button {
    margin-top: 32px;
    width: 100%;
  }
  .mypage-container {
    padding: 40px 20px;
    max-width: 800px;
    margin: 0 auto;
  }
  .inform-rule {
    font-size: 12px;
    font-weight: normal;
    color: var(--color-gray-500);
    margin-left: 8px;
  }
</style>
