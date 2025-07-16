'''
<template>
  <BaseDrawer
    v-model="visible"
    title="고객 수정"
    position="right"
    size="md"
    :closable="true"
    :mask-closable="true"
    :z-index="zIndex"
    @after-leave="handleAfterLeave"
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
          <option v-for="option in genderOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
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
          :base-z-index="zIndex + 1"
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
          <option :value="null">유입경로 없음</option>
          <option v-for="channel in channelOptions" :key="channel.id" :value="channel.id">
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
          <option :value="null" disabled>등급 선택</option>
          <option v-for="grade in gradeOptions" :key="grade.gradeId" :value="grade.gradeId">
            {{ grade.gradeName }}
          </option>
        </select>
        <div v-if="errors.grade" class="error-message">{{ errors.grade }}</div>
      </div>
      <div class="form-row">
        <label class="form-label">
          <input v-model="form.marketingConsent" type="checkbox" class="form-checkbox" />
          마케팅 수신 동의
        </label>
      </div>
      <div class="form-row">
        <label class="form-label">
          <input v-model="form.notificationConsent" type="checkbox" class="form-checkbox" />
          안내문자 수신 동의
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
    <BaseToast ref="toastRef" />
  </BaseDrawer>
</template>

<script setup>
  import { ref, watch, defineEmits, defineProps, onMounted, computed } from 'vue';
  import BaseDrawer from '@/components/common/BaseDrawer.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import Multiselect from '@vueform/multiselect';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import '@vueform/multiselect/themes/default.css';
  import { useAuthStore } from '@/store/auth.js';
  import { useMetadataStore } from '@/store/metadata.js';
  import { storeToRefs } from 'pinia';
  import customersAPI from '../api/customers.js';
  import BaseToast from '@/components/common/BaseToast.vue';

  const props = defineProps({
    modelValue: {
      type: Boolean,
      default: false,
    },
    customer: {
      type: Object,
      default: null,
    },
    zIndex: {
      type: Number,
      default: 1000,
    },
  });

  const emit = defineEmits(['update:modelValue', 'update', 'close', 'afterLeave']);

  const visible = ref(false);
  const form = ref(getBlankForm());
  const errors = ref({ name: '', phone: '', grade: '', birthdate: '', channelId: '', gender: '' });

  const authStore = useAuthStore();
  const metadataStore = useMetadataStore();

  const {
    grades: gradeOptions,
    tags: tagOptions,
    staff: staffOptions,
    channels: channelOptions,
  } = storeToRefs(metadataStore);

  const genderOptions = computed(() => [
    { value: 'M', label: '남성' },
    { value: 'F', label: '여성' },
  ]);

  const toastRef = ref(null);

  onMounted(() => {
    metadataStore.loadMetadata();
  });

  const today = new Date().toISOString().slice(0, 10);

  function getBlankForm() {
    return {
      name: '',
      phone: '',
      gender: '',
      birthdate: '',
      staffId: null,
      channelId: null,
      customerGradeId: null,
      tags: [],
      memo: '',
      marketingConsent: false,
      notificationConsent: false,
    };
  }

  async function populateFromCustomerProp() {
    if (!props.customer) {
      resetForm();
      return;
    }

    let c = props.customer;

    // 필요한 추가 정보가 없으면 상세 조회
    if (
      c.marketingConsent === undefined ||
      c.notificationConsent === undefined ||
      c.gender === '남성' ||
      c.gender === '여성'
    ) {
      try {
        const detail = await customersAPI.getCustomerDetail(c.customerId);
        if (detail) c = { ...c, ...detail };
      } catch (err) {
        console.warn('고객 상세 정보 조회 실패:', err);
      }
    }

    if (!c.tags && props.customer.tags) {
      c.tags = props.customer.tags;
    }

    form.value = {
      name: c.customerName || '',
      phone: c.phoneNumber || '',
      gender:
        c.gender === '남성' || c.gender === '여성'
          ? c.gender === '남성'
            ? 'M'
            : 'F'
          : c.gender || '',
      birthdate: c.birthdate || '',
      staffId: c.staff?.staffId ? Number(c.staff.staffId) : null,
      channelId: c.acquisitionChannel?.acquisitionChannelId
        ? Number(c.acquisitionChannel.acquisitionChannelId)
        : c.channelId
          ? Number(c.channelId)
          : null,
      customerGradeId: c.customerGrade?.customerGradeId
        ? Number(c.customerGrade.customerGradeId)
        : Number(c.customerGradeId) || null,
      tags: Array.isArray(c.tags) ? c.tags.map(t => t.tagId ?? t) : [],
      memo: c.memo || '',
      marketingConsent: c.marketingConsent === true,
      notificationConsent: c.notificationConsent === true,
    };

    errors.value = { name: '', phone: '', grade: '' };
  }

  watch(
    () => props.customer,
    () => {
      populateFromCustomerProp();
    },
    { immediate: true }
  );

  watch(
    () => props.modelValue,
    newValue => {
      visible.value = newValue;
    }
  );

  watch(visible, newValue => {
    emit('update:modelValue', newValue);
    if (!newValue) {
      emit('close');
    }
  });

  function handleAfterLeave() {
    resetForm();
    emit('afterLeave');
  }

  function validateField(field) {
    if (field === 'name') errors.value.name = !form.value.name.trim() ? '이름을 입력해주세요' : '';
    if (field === 'phone') {
      if (!form.value.phone.trim()) errors.value.phone = '연락처를 입력해주세요';
      else if (!/^01[016789]\d{7,8}$/.test(form.value.phone))
        errors.value.phone = '올바른 형식으로 작성해주세요';
      else errors.value.phone = '';
    }
    if (field === 'grade')
      errors.value.grade = !form.value.customerGradeId ? '등급을 선택해주세요' : '';
    if (field === 'birthdate')
      errors.value.birthdate = !form.value.birthdate ? '생년월일을 입력해주세요' : '';
    if (field === 'channelId')
      errors.value.channelId = !form.value.channelId ? '유입경로를 선택해주세요' : '';
    if (field === 'gender') errors.value.gender = !form.value.gender ? '성별을 선택해주세요' : '';
  }

  async function validateAndSubmit() {
    validateField('name');
    validateField('phone');
    validateField('grade');
    validateField('birthdate');
    validateField('channelId');
    validateField('gender');

    if (Object.values(errors.value).some(e => e)) {
      return;
    }

    try {
      const cleanedPhone = (form.value.phone || '').replace(/-/g, '');
      const payload = {
        customerId: props.customer?.customerId,
        customerName: form.value.name,
        phoneNumber: cleanedPhone,
        gender: form.value.gender,
        birthdate: form.value.birthdate,
        staffId: form.value.staffId,
        channelId: form.value.channelId,
        customerGradeId: form.value.customerGradeId,
        tags: form.value.tags,
        memo: form.value.memo,
        marketingConsent: form.value.marketingConsent,
        notificationConsent: form.value.notificationConsent,
      };

      // Optional fields: '' -> null
      if (payload.gender === '') payload.gender = null;
      if (payload.birthdate === '') payload.birthdate = null;
      if (payload.memo.trim() === '') payload.memo = null;
      // staffId and channelId can be null from the select box directly

      // 태그 변경 처리
      const originalTags = Array.isArray(props.customer?.tags)
        ? props.customer.tags.map(t => t.tagId ?? t)
        : [];
      const newTags = Array.isArray(form.value.tags) ? form.value.tags : [];
      const originalTagSet = new Set(originalTags);
      const newTagSet = new Set(newTags);
      const customerId = props.customer?.customerId;

      // 태그 추가
      for (const tagId of newTagSet) {
        if (!originalTagSet.has(tagId)) {
          try {
            await customersAPI.addTagToCustomer(customerId, tagId);
            await metadataStore.loadMetadata(true);
            toastRef.value?.success('태그가 추가되었습니다.');
          } catch (e) {
            toastRef.value?.error('태그 추가 실패');
          }
        }
      }
      // 태그 삭제
      for (const tagId of originalTagSet) {
        if (!newTagSet.has(tagId)) {
          try {
            await customersAPI.removeTagFromCustomer(customerId, tagId);
            await metadataStore.loadMetadata(true);
            toastRef.value?.success('태그가 삭제되었습니다.');
          } catch (e) {
            toastRef.value?.error('태그 삭제 실패');
          }
        }
      }

      await customersAPI.updateCustomer(props.customer.customerId, payload);
      toastRef.value?.success('고객 정보가 수정되었습니다.');
      emit('update', { ...payload, customerId: props.customer.customerId });
      visible.value = false;
    } catch (error) {
      console.error('고객 정보 수정 실패:', error);
      toastRef.value?.error('고객 정보 수정에 실패했습니다.');
      alert('고객 정보 수정에 실패했습니다.');
    }
  }

  function closeDrawer() {
    visible.value = false;
  }

  function resetForm() {
    form.value = getBlankForm();
    errors.value = { name: '', phone: '', grade: '', birthdate: '', channelId: '', gender: '' };
  }
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
    margin-bottom: 20px;
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
    gap: 0.75rem;
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
<style src="@vueform/multiselect/themes/default.css"></style>
''
