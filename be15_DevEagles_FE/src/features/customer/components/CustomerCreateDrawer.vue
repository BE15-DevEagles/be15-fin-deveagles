<template>
  <BaseDrawer
    v-model="visible"
    title="신규 고객 등록"
    position="right"
    size="md"
    :closable="true"
    :mask-closable="true"
    @after-leave="resetForm"
  >
    <form
      class="customer-create-form"
      autocomplete="off"
      novalidate
      @submit.prevent="validateAndSubmit"
    >
      <div class="form-row">
        <label class="form-label"> 이름<span class="required">*</span> </label>
        <input
          v-model="form.name"
          type="text"
          class="form-input"
          :class="{ 'input-error': errors.name }"
          placeholder="고객명"
          @blur="validateField('name')"
        />
        <div v-if="errors.name" class="error-message">{{ errors.name }}</div>
      </div>
      <div class="form-row">
        <label class="form-label"> 연락처<span class="required">*</span> </label>
        <input
          v-model="form.phone"
          type="text"
          class="form-input"
          :class="{ 'input-error': errors.phone }"
          placeholder="01000000000"
          @blur="validateField('phone')"
        />
        <div v-if="errors.phone" class="error-message">{{ errors.phone }}</div>
      </div>
      <div class="form-row">
        <label class="form-label">성별<span class="required">*</span></label>
        <select
          v-model="form.gender"
          class="form-input"
          :class="{ 'input-error': errors.gender }"
          @blur="validateField('gender')"
        >
          <option value="" disabled>성별 선택</option>
          <option value="남성">남성</option>
          <option value="여성">여성</option>
        </select>
        <div v-if="errors.gender" class="error-message">{{ errors.gender }}</div>
      </div>
      <div class="form-row">
        <label class="form-label">생년월일<span class="required">*</span></label>
        <PrimeDatePicker
          v-model="form.birthdate"
          :max-date="new Date(today)"
          placeholder="생일을 선택하세요"
          :disabled="false"
          :error="errors.birthdate"
        />
        <div v-if="errors.birthdate" class="error-message">{{ errors.birthdate }}</div>
      </div>
      <div class="form-row">
        <label class="form-label">담당자</label>
        <select v-model="form.staffId" class="form-input">
          <option value="" disabled>담당자 선택</option>
          <option :value="null">담당자 없음</option>
          <option v-for="staff in staffOptions" :key="staff.id" :value="staff.id">
            {{ staff.name }}
          </option>
        </select>
      </div>
      <div class="form-row">
        <label class="form-label">유입경로<span class="required">*</span></label>
        <select
          v-model="form.channelId"
          class="form-input"
          :class="{ 'input-error': errors.channelId }"
          @blur="validateField('channelId')"
        >
          <option value="" disabled>유입경로 선택</option>
          <option
            v-for="channel in acquisitionChannelOptions"
            :key="channel.id"
            :value="channel.id"
          >
            {{ channel.channelName }}
          </option>
        </select>
        <div v-if="errors.channelId" class="error-message">{{ errors.channelId }}</div>
      </div>
      <div class="form-row">
        <label class="form-label">태그</label>
        <Multiselect
          :key="`tags-${JSON.stringify(tagOptions.value)}`"
          v-model="form.tags"
          :options="tagOptions"
          mode="tags"
          :close-on-select="false"
          :searchable="true"
          :create-option="false"
          :hide-selected="false"
          label="tagName"
          value-prop="tagId"
          track-by="tagId"
          placeholder="태그 선택"
          class="multiselect-custom"
          :loading="metadataStore.isLoading"
        />
      </div>
      <div class="form-row">
        <label class="form-label">메모</label>
        <textarea
          v-model="form.memo"
          class="form-input"
          rows="2"
          placeholder="메모 입력"
        ></textarea>
      </div>
      <div class="form-row">
        <label class="form-label"> 등급<span class="required">*</span> </label>
        <select
          v-model="form.customerGradeId"
          class="form-input"
          :class="{ 'input-error': errors.grade }"
          @blur="validateField('grade')"
        >
          <option value="" disabled>등급 선택</option>
          <option v-for="grade in gradeOptions" :key="grade.id" :value="grade.id">
            {{ grade.name }}
          </option>
        </select>
        <div v-if="errors.grade" class="error-message">{{ errors.grade }}</div>
      </div>
      <div class="form-row form-row-checkbox">
        <label class="form-label-checkbox">
          <input v-model="form.marketingConsent" type="checkbox" class="form-checkbox" />
          <span>마케팅 정보 수신 동의</span>
        </label>
      </div>
      <div class="form-row form-row-checkbox">
        <label class="form-label-checkbox">
          <input v-model="form.notificationConsent" type="checkbox" class="form-checkbox" />
          <span>알림(예약 등) 수신 동의</span>
        </label>
      </div>
    </form>
    <template #footer>
      <div class="drawer-footer-actions">
        <BaseButton type="secondary" size="sm" outline @click="closeDrawer">취소</BaseButton>
        <BaseButton type="primary" size="sm" native-type="submit" @click="validateAndSubmit"
          >저장</BaseButton
        >
      </div>
    </template>
  </BaseDrawer>
</template>

<script setup>
  import { ref, watch, defineEmits, defineProps, nextTick, onMounted } from 'vue';
  import BaseDrawer from '@/components/common/BaseDrawer.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import Multiselect from '@vueform/multiselect';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue'; // 반드시 추가!
  import '@vueform/multiselect/themes/default.css';
  import { useAuthStore } from '@/store/auth.js';
  import { useMetadataStore } from '@/store/metadata.js';
  import { storeToRefs } from 'pinia';

  const props = defineProps({
    modelValue: { type: Boolean, default: false },
  });
  const emit = defineEmits(['update:modelValue', 'create']);

  const visible = ref(props.modelValue);
  watch(
    () => props.modelValue,
    v => {
      visible.value = v;
      if (v) nextTick(resetForm);
    }
  );
  watch(visible, v => emit('update:modelValue', v));

  const metadataStore = useMetadataStore();
  const {
    staff: staffOptions,
    tags: tagOptions,
    grades: gradeOptions,
    channels: acquisitionChannelOptions,
  } = storeToRefs(metadataStore);

  const today = new Date().toISOString().slice(0, 10);

  const authStore = useAuthStore();

  const initialForm = () => ({
    name: '',
    phone: '',
    gender: '',
    birthdate: '',
    staffId: '',
    channelId: '',
    tags: [],
    memo: '',
    customerGradeId: '',
    marketingConsent: false,
    notificationConsent: false,
  });
  const form = ref(initialForm());
  const errors = ref({ name: '', phone: '', grade: '', birthdate: '', channelId: '', gender: '' });

  function validateField(field) {
    if (field === 'name') errors.value.name = !form.value.name.trim() ? '이름을 입력해주세요' : '';
    if (field === 'phone') {
      if (!form.value.phone.trim()) errors.value.phone = '연락처를 입력해주세요';
      else if (!/^01[016789]\d{7,8}$/.test(form.value.phone))
        errors.value.phone = '올바른 형식으로 작성해주세요';
      else errors.value.phone = '';
    }
    if (field === 'gender') errors.value.gender = !form.value.gender ? '성별을 선택해주세요' : '';
    if (field === 'grade')
      errors.value.grade = !form.value.customerGradeId ? '등급을 선택해주세요' : '';
    if (field === 'birthdate')
      errors.value.birthdate = !form.value.birthdate ? '생년월일을 입력해주세요' : '';
    if (field === 'channelId')
      errors.value.channelId = !form.value.channelId ? '유입경로를 선택해주세요' : '';
  }

  function validateAndSubmit() {
    validateField('name');
    validateField('phone');
    validateField('grade');
    validateField('birthdate');
    validateField('channelId');
    validateField('gender');
    if (
      !errors.value.name &&
      !errors.value.phone &&
      !errors.value.grade &&
      !errors.value.birthdate &&
      !errors.value.channelId &&
      !errors.value.gender
    ) {
      const payload = { ...form.value };

      // 필드명 매핑
      payload.customerName = payload.name;
      delete payload.name;
      payload.phoneNumber = payload.phone?.replace(/-/g, '');
      delete payload.phone;

      // gender enum 변환 및 null 처리
      if (payload.gender === '남성') {
        payload.gender = 'M';
      } else if (payload.gender === '여성') {
        payload.gender = 'F';
      } else if (payload.gender === '') {
        payload.gender = null;
      }

      // birthdate 포맷팅 및 null 처리
      if (payload.birthdate) {
        const d = new Date(payload.birthdate);
        payload.birthdate = d.toISOString().split('T')[0];
      } else {
        payload.birthdate = null;
      }

      // Optional fields: '' -> null
      if (payload.staffId === '') payload.staffId = null;
      if (payload.channelId === '') payload.channelId = null;
      if (payload.memo.trim() === '') {
        payload.memo = null;
      }

      // 태그는 숫자 ID 배열로 전송
      payload.tags = Array.isArray(payload.tags) ? payload.tags : [];

      // staff_name, grade 속성 제거
      delete payload.staff_name;
      delete payload.grade;

      emit('create', payload);
      visible.value = false;
      resetForm();
    }
  }
  function closeDrawer() {
    visible.value = false;
    resetForm();
  }
  function resetForm() {
    form.value = initialForm();
    errors.value = { name: '', phone: '', grade: '', birthdate: '', channelId: '', gender: '' };
  }

  onMounted(async () => {
    await metadataStore.loadMetadata();
  });
</script>

<style>
  .multiselect-custom {
    --ms-font-size: 15px;
    --ms-line-height: 1.4;
    --ms-border-color: #ddd;
    --ms-radius: 8px;
    --ms-py: 8px;
    --ms-px: 12px;
    --ms-ring-width: 0px;
    --ms-ring-color: transparent;
    --ms-tag-bg: #e6f9fa;
    --ms-tag-color: #1b7f8c;
    --ms-tag-radius: 14px;
  }
</style>
<style scoped>
  .customer-create-form {
    display: flex;
    flex-direction: column;
    gap: 18px;
    padding: 0;
    min-height: 100%;
    box-sizing: border-box;
    flex: 1 1 auto;
  }
  .form-row {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .form-label {
    font-size: 14px;
    font-weight: 500;
    color: #222;
  }
  .required {
    color: #e53935;
    margin-left: 2px;
    font-size: 15px;
    font-weight: bold;
  }
  .form-input {
    height: 40px;
    border: 1px solid #ddd;
    border-radius: 8px;
    font-size: 15px;
    padding: 0 12px;
    background: #fff;
    transition: border 0.2s;
  }
  .form-input:focus {
    outline: none;
    border-color: #364f6b;
    background: #f8fafd;
  }
  .input-error {
    border-color: #e53935 !important;
  }
  .error-message {
    color: #e53935;
    font-size: 12px;
    margin-top: 2px;
  }
  .form-input[type='date'] {
    padding-right: 0;
  }
  .drawer-footer-actions {
    display: flex;
    gap: 10px;
    justify-content: flex-end;
    margin-top: 0;
    margin-bottom: 0;
  }
  .form-row-checkbox {
    flex-direction: row;
    align-items: center;
    gap: 8px;
  }
  .form-label-checkbox {
    display: flex;
    align-items: center;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    color: #333;
  }
  .form-checkbox {
    width: 16px;
    height: 16px;
    margin-right: 8px;
    accent-color: #364f6b;
  }
</style>
