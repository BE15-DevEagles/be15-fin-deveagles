<template>
  <div class="staff-form">
    <div class="profile-section">
      <ProfileUpload v-model="profilePreview" v-model:file="profileFile" />

      <div class="color-picker-inline">
        <PickColors v-model:value="form.colorCode" class="color-picker" />

        <div class="tooltip-wrapper">
          <InfoTooltipIcon />
          <div class="tooltip-bubble">대표 색상을 변경할 수 있어요</div>
        </div>
      </div>
    </div>

    <BaseForm
      v-model="form.staffName"
      label="직원명"
      :error="errors.staffName"
      @focus="clearError('staffName')"
    />
    <BaseForm
      v-model="form.email"
      label="이메일"
      :error="errors.email"
      @blur="emailChecked"
      @focus="clearError('email')"
    />
    <BaseForm
      v-model="form.phoneNumber"
      label="전화번호"
      :error="errors.phoneNumber"
      @blur="phoneChecked"
      @focus="clearError('phoneNumber')"
    />
    <BaseForm
      v-model="form.grade"
      label="직급"
      :error="errors.grade"
      @focus="clearError('grade')"
    />
    <BaseForm
      v-model="form.working"
      label="재직 상태"
      type="select"
      :options="[
        { value: true, text: '재직중' },
        { value: false, text: '퇴사' },
      ]"
      :error="errors.working"
      @focus="clearError('working')"
    />
    <PrimeDatePicker v-model="form.joinedDate" label="입사일" />
    <PrimeDatePicker
      v-show="form.working === false || form.working === 'false'"
      v-model="form.leftDate"
      label="퇴사일"
    />
    <BaseForm v-model="form.description" label="메모" type="textarea" :rows="4" />
  </div>
</template>
<script setup>
  import { ref, toRaw, watch } from 'vue';
  import isEqual from 'lodash.isequal';
  import PickColors from 'vue-pick-colors';
  import ProfileUpload from '@/features/users/components/ProfileUpload.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import InfoTooltipIcon from '@/components/icons/InfoTooltipIcon.vue';
  import { validEmail } from '@/features/users/api/users.js';

  const props = defineProps({
    modelValue: Object,
    file: File,
    preview: String,
  });

  const emit = defineEmits(['update:modelValue', 'update:file', 'update:preview']);

  const form = ref({
    staffName: '',
    email: '',
    phoneNumber: '',
    grade: '',
    description: '',
    joinedDate: null,
    leftDate: null,
    working: false,
    colorCode: '#3FC1C9',
  });

  const profilePreview = ref('');
  const profileFile = ref(null);

  // ✅ props 값 → 내부 form 초기화 (개별 필드 할당)

  const initialized = ref(false); // 최초 1회 초기화 플래그

  watch(
    () => props.modelValue,
    val => {
      if (!val) return;

      const newForm = {
        ...form.value,
        staffName: val.staffName ?? form.value.staffName,
        email: val.email ?? form.value.email,
        phoneNumber: val.phoneNumber ?? form.value.phoneNumber,
        grade: val.grade ?? form.value.grade,
        description: val.description ?? form.value.description,
        joinedDate: val.joinedDate ?? form.value.joinedDate,
        leftDate: val.leftDate ?? form.value.leftDate,
        working: val.working ?? form.value.working,
        colorCode: val.colorCode ?? form.value.colorCode,
        permissions: val.permissions ?? form.value.permissions,
      };

      if (!isEqual(newForm, toRaw(form.value))) {
        form.value = newForm;
      }

      if (!initialized.value && val.profileUrl) {
        profilePreview.value = val.profileUrl;
        initialized.value = true;
      }
    },
    { immediate: true }
  );

  // ✅ 내부 form 변경 → 부모에게 emit
  let prevEmittedForm = {};

  watch(
    form,
    () => {
      const rawForm = toRaw(form.value);
      if (!isEqual(rawForm, prevEmittedForm)) {
        emit('update:modelValue', { ...rawForm });
        prevEmittedForm = { ...rawForm }; // 최신 값 기억
      }
    },
    { deep: true }
  );

  watch(profilePreview, val => emit('update:preview', val));
  watch(profileFile, val => emit('update:file', val));

  // ✅ 재직 상태에 따른 퇴사일 처리
  watch(
    () => form.value.working,
    newVal => {
      if (newVal === false) return;
      form.value.leftDate = null;
    }
  );

  // ✅ validation
  const errors = ref({
    staffName: '',
    email: '',
    phoneNumber: '',
    grade: '',
    working: '',
  });

  const clearError = field => {
    errors.value[field] = '';
  };

  const emailCheckPassed = ref(false);
  const phonePassed = ref(false);

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

    if (!form.value.email || !form.value.email.includes('@')) {
      errors.value.email = '올바른 이메일 형식을 입력해주세요.';
      valid = false;
    }

    if (!form.value.staffName) {
      errors.value.staffName = '직원명을 입력해주세요.';
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

    if (!form.value.grade) {
      errors.value.grade = '직급을 입력해주세요.';
      valid = false;
    }

    if ((form.value.working === 'false' || form.value.working === false) && !form.value.leftDate) {
      errors.value.working = '퇴사일을 입력하셔야 합니다.';
      valid = false;
    }

    return valid;
  };

  defineExpose({ validate });
</script>

<style scoped>
  .staff-form {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 24px;
    border: 1px solid #e0e0e0;
    border-radius: 12px;
    background-color: #fff;
    max-width: 600px;
    margin: 0 auto;
  }
  .profile-section {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .color-picker {
    margin-top: 4px;
    z-index: 10;
  }
  .color-picker-inline {
    display: flex;
    align-items: center;
    gap: 0;
  }

  .tooltip-wrapper {
    position: relative;
    display: flex;
    align-items: center;
    margin-left: 0px;
    transform: translateX(-0.5px);
  }

  .tooltip-icon {
    width: 16px;
    height: 16px;
    color: #9ca3af;
    cursor: default;
  }

  .tooltip-bubble {
    position: absolute;
    bottom: 125%;
    left: 50%;
    transform: translateX(-50%);
    background-color: #333;
    color: white;
    padding: 6px 8px;
    border-radius: 6px;
    font-size: 12px;
    white-space: nowrap;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.2s ease;
    z-index: 10;
  }

  .tooltip-wrapper:hover .tooltip-bubble {
    opacity: 1;
    pointer-events: auto;
  }
  .staff-form :deep(.input-container) {
    margin-bottom: 16px;
  }
</style>
