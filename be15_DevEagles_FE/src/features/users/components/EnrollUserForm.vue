<template>
  <h2 class="font-section-title signup-title">회원가입</h2>

  <form class="form-fields">
    <div class="label-row">
      <label for="loginId">
        아이디
        <span v-if="isRequired('loginId')" class="required">*</span>
      </label>
    </div>
    <BaseForm
      id="loginId"
      v-model="form.loginId"
      placeholder="사용하실 아이디를 입력해주세요."
      type="text"
      :error="errors.loginId"
      autocomplete="loginId"
      @blur="idChecked"
      @focus="clearError('loginId')"
    />

    <div class="label-row">
      <label for="email">
        E-mail
        <span v-if="isRequired('email')" class="required">*</span>
      </label>
    </div>
    <BaseForm
      id="email"
      v-model="form.email"
      placeholder="E-mail 주소를 입력해주세요."
      type="email"
      :error="errors.email"
      autocomplete="new-email"
      @blur="emailChecked"
      @focus="clearError('email')"
    />

    <div class="label-row">
      <label for="password">
        비밀번호
        <span v-if="isRequired('password')" class="required">*</span>
      </label>
      <p class="inform-rule">특수문자, 영문자, 숫자를 포함한 8자리 이상</p>
    </div>
    <BaseForm
      id="password"
      v-model="form.password"
      placeholder="비밀번호를 입력해주세요."
      type="password"
      :error="errors.password"
      autocomplete="new-password"
      @blur="passwordChecked"
      @focus="clearError('password')"
    />
    <BaseForm
      id="checkPwd"
      v-model="form.checkPwd"
      placeholder="비밀번호를 한 번 더 입력해주세요."
      type="password"
      :error="errors.checkPwd"
      autocomplete="new-password"
      @blur="checkPwdChecked"
      @focus="clearError('checkPwd')"
    />

    <div class="label-row">
      <label for="staffName">
        대표자명
        <span v-if="isRequired('staffName')" class="required">*</span>
      </label>
    </div>
    <BaseForm
      id="staffName"
      v-model="form.staffName"
      placeholder="대표자명을 입력해주세요."
      type="text"
      :error="errors.staffName"
      @focus="clearError('staffName')"
    />

    <div class="label-row">
      <label for="phoneNumber">
        전화번호
        <span v-if="isRequired('phoneNumber')" class="required">*</span>
      </label>
      <p class="inform-rule">'-' 제외</p>
    </div>
    <BaseForm
      id="phoneNumber"
      v-model="form.phoneNumber"
      placeholder="전화번호를 입력해주세요."
      type="text"
      :error="errors.phoneNumber"
      @blur="phoneChecked"
      @focus="clearError('phoneNumber')"
    />
  </form>
</template>
<script setup>
  import BaseForm from '@/components/common/BaseForm.vue';
  import { computed, ref } from 'vue';
  import { validEmail, validId } from '@/features/users/api/users.js';

  const props = defineProps({
    modelValue: Object,
    isRequired: Function,
  });

  const emit = defineEmits(['update:modelValue', 'email-check']);

  const form = computed({
    get: () => props.modelValue,
    set: val => emit('update:modelValue', val),
  });

  const errors = ref({
    loginId: '',
    email: '',
    password: '',
    checkPwd: '',
    staffName: '',
    phoneNumber: '',
  });

  const clearError = field => {
    errors.value[field] = '';
  };

  const idCheckPassed = ref(false);
  const emailCheckPassed = ref(false);
  const passwordPassed = ref(false);
  const checkPwdPassed = ref(false);
  const phonePassed = ref(false);

  const idChecked = async () => {
    const checkItem = form.value.loginId?.trim();
    if (!checkItem) {
      errors.value.loginId = '아이디를 입력해주세요.';
      idCheckPassed.value = false;
      return;
    }

    try {
      const res = await validId({ checkItem });
      if (res.data.data === true) {
        errors.value.loginId = '';
        idCheckPassed.value = true;
      } else {
        errors.value.loginId = '이미 사용 중인 아이디입니다.';
        idCheckPassed.value = false;
      }
    } catch (err) {
      errors.value.loginId = '중복 확인 중 오류가 발생했습니다.';
      idCheckPassed.value = false;
    }
  };

  const emailChecked = async () => {
    const checkItem = form.value.email?.trim();
    if (!checkItem) {
      errors.value.email = '이메일을 입력해주세요.';
      emailCheckPassed.value = false;
      return;
    }

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
    const password = form.value.password?.trim();
    if (!password) {
      errors.value.password = '비밀번호를 입력해주세요.';
      passwordPassed.value = false;
      return;
    }

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
    const checkPwd = form.value.checkPwd?.trim();
    const password = form.value.password?.trim();

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
    const phoneNumber = form.value.phoneNumber?.trim();

    if (!phoneNumber) {
      errors.value.phoneNumber = '전화번호를 입력해주세요.';
      phonePassed.value = false;
      return;
    }

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

  const validate = () => {
    let valid = true;

    if (!form.value.loginId) {
      errors.value.loginId = '아이디를 입력해주세요.';
      valid = false;
    }

    if (!form.value.email || !form.value.email.includes('@')) {
      errors.value.email = '올바른 이메일 형식을 입력해주세요.';
      valid = false;
      emit('email-check', false);
    }

    const password = form.value.password;
    if (!password) {
      errors.value.password = '비밀번호를 입력해주세요.';
      valid = false;
    } else if (
      !/^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\\[\]{};':"\\|,.<>/?]).{8,}$/.test(password)
    ) {
      errors.value.password = '사용할 수 없는 비밀번호입니다.';
      valid = false;
    }

    if (password !== form.value.checkPwd) {
      errors.value.checkPwd = '비밀번호가 일치하지 않습니다.';
      valid = false;
    }

    if (!form.value.staffName) {
      errors.value.staffName = '대표자명을 입력해주세요.';
      valid = false;
    }

    if (!form.value.phoneNumber || form.value.phoneNumber.length < 10) {
      const onlyNumberPattern = /^[0-9]+$/;
      if (!onlyNumberPattern.test(form.value.phoneNumber)) {
        errors.value.phoneNumber = '숫자만 입력해주세요.';
      } else {
        errors.value.phoneNumber = '전화번호를 정확히 입력해주세요.';
      }
      valid = false;
    }

    return valid;
  };

  defineExpose({ validate });
</script>
<style scoped>
  .signup-title {
    text-align: center;
    margin-bottom: 24px;
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
  .form-fields {
    width: 360px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .required {
    color: red;
    margin-left: 4px;
  }
  .check-text {
    font-size: 12px;
    color: var(--color-gray-500);
    cursor: pointer;
    font-weight: 500;
  }
  .validation-msg {
    font-size: 12px;
    color: var(--color-error, red);
    margin-top: -8px;
    margin-bottom: 4px;
  }
  .inform-rule {
    font-size: 12px;
    font-weight: normal;
    color: var(--color-gray-500);
    margin-left: 8px;
  }
</style>
