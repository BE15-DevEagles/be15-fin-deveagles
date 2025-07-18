<template>
  <form class="campaign-form" @submit.prevent="handleSubmit">
    <!-- 캠페인명 -->
    <BaseForm
      id="campaignName"
      v-model="formData.name"
      label="캠페인명*"
      type="text"
      placeholder="캠페인명을 입력하세요"
      :error="errors.name"
      required
    />

    <!-- 캠페인 설명 -->
    <BaseForm
      id="description"
      v-model="formData.description"
      label="캠페인 설명"
      type="textarea"
      placeholder="캠페인에 대한 설명을 입력하세요"
      :error="errors.description"
    />

    <!-- 캠페인 기간 -->
    <PrimeDatePicker
      v-model="campaignDateRange"
      selection-mode="range"
      label="캠페인 기간*"
      placeholder="캠페인 기간을 선택하세요"
      :min-date="new Date()"
      :error="errors.dateRange"
      clearable
    />

    <!-- 2열: 등급/태그 -->
    <div class="form-row">
      <div class="form-col">
        <label class="form-label">대상 고객 등급*</label>
        <select v-model="formData.customerGradeId" class="form-select">
          <option value="" disabled>등급 선택</option>
          <option v-for="g in gradeOptions" :key="g.value" :value="g.value">{{ g.text }}</option>
        </select>
        <div v-if="errors.customerGradeId" class="error-message">{{ errors.customerGradeId }}</div>
      </div>
      <div class="form-col">
        <label class="form-label">대상 고객 태그</label>
        <select v-model="formData.customerTagId" class="form-select">
          <option value="" disabled>태그 선택</option>
          <option v-for="t in tagOptions" :key="t.value" :value="t.value">{{ t.text }}</option>
        </select>
        <div v-if="errors.customerTagId" class="error-message">{{ errors.customerTagId }}</div>
      </div>
    </div>

    <!-- 2열: 쿠폰/메시지 템플릿 -->
    <div class="form-row">
      <div class="form-col">
        <CompactCouponSelector
          v-model="selectedCoupon"
          label="쿠폰 선택"
          :multiple="false"
          :placeholder="'쿠폰을 선택하세요'"
          :filter-options="{ shopId: authStore.shopId, onlyActive: true }"
          @coupon-selected="handleCouponSelected"
          @update:model-value="handleCouponModelValueUpdate"
        />
      </div>
      <div class="form-col">
        <CompactTemplateSelector
          v-model="formData.templateId"
          label="메시지 템플릿"
          :filter-options="{ shopId: authStore.shopId }"
        />
      </div>
    </div>

    <!-- 메시지 예약 발송일시 -->
    <div class="form-row">
      <PrimeDatePicker
        v-model="formData.messageSendAt"
        label="메시지 예약 발송일시"
        placeholder="날짜와 시간을 선택하세요"
        :show-icon="true"
        :clearable="true"
        :show-time="true"
      />
    </div>

    <!-- 버튼들 -->
    <div class="form-actions">
      <BaseButton type="secondary" outline style="flex: 1" html-type="button" @click="handleCancel">
        취소
      </BaseButton>
      <BaseButton type="primary" style="flex: 1" html-type="submit"> 저장 </BaseButton>
    </div>
  </form>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { storeToRefs } from 'pinia';
  import { useMetadataStore } from '@/store/metadata.js';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import CompactCouponSelector from '@/features/coupons/components/CompactCouponSelector.vue';
  import CompactTemplateSelector from '@/components/common/CompactTemplateSelector.vue';
  import { useAuthStore } from '@/store/auth.js';
  import dayjs from 'dayjs';

  const emit = defineEmits(['save', 'cancel']);

  const metadataStore = useMetadataStore();
  const { grades, tags } = storeToRefs(metadataStore);
  const authStore = useAuthStore();

  const formData = ref({
    name: '',
    description: '',
    startDate: '',
    endDate: '',
    customerGradeId: '',
    customerTagId: '',
    couponId: '',
    messageSendAt: '',
    templateId: '',
  });

  // 선택된 쿠폰 관리
  const selectedCoupon = ref(null);

  // 쿠폰 선택 핸들러
  const handleCouponSelected = coupon => {
    if (coupon) {
      selectedCoupon.value = coupon;
      formData.value.couponId = coupon.id || coupon.value;
    }
  };

  const handleCouponModelValueUpdate = coupon => {
    if (coupon) {
      selectedCoupon.value = coupon;
      formData.value.couponId = coupon.id || coupon.value;
    }
  };

  const errors = ref({});

  const gradeOptions = computed(() =>
    grades.value.map(g => ({ value: g.id, text: g.name || g.customerGradeName }))
  );
  const tagOptions = computed(() => tags.value.map(t => ({ value: t.tagId, text: t.tagName })));

  const campaignDateRange = computed({
    get() {
      if (formData.value.startDate && formData.value.endDate) {
        return [new Date(formData.value.startDate), new Date(formData.value.endDate)];
      }
      return null;
    },
    set(value) {
      if (value && Array.isArray(value) && value.length === 2) {
        if (value[0] && value[1]) {
          formData.value.startDate = formatDateToLocal(value[0]);
          formData.value.endDate = formatDateToLocal(value[1]);
        } else if (value[0]) {
          formData.value.startDate = formatDateToLocal(value[0]);
          formData.value.endDate = '';
        }
      } else {
        formData.value.startDate = '';
        formData.value.endDate = '';
      }
    },
  });

  function formatDateToLocal(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  function validateForm() {
    errors.value = {};
    if (!formData.value.name.trim()) {
      errors.value.name = '캠페인명은 필수입니다.';
    }
    if (!formData.value.startDate || !formData.value.endDate) {
      errors.value.dateRange = '캠페인 기간 선택은 필수입니다.';
    }
    if (!formData.value.customerGradeId) {
      errors.value.customerGradeId = '대상 고객 등급 선택은 필수입니다.';
    }
    // 날짜 유효성 검사
    if (formData.value.startDate && formData.value.endDate) {
      const startDate = new Date(formData.value.startDate);
      const endDate = new Date(formData.value.endDate);
      if (endDate <= startDate) {
        errors.value.dateRange = '종료일은 시작일보다 늦어야 합니다.';
      }
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      startDate.setHours(0, 0, 0, 0);
      if (startDate < today) {
        errors.value.dateRange = '시작일은 오늘 이후여야 합니다.';
      }
    }
    return Object.keys(errors.value).length === 0;
  }

  function handleSubmit() {
    if (validateForm()) {
      const payload = { ...formData.value };
      payload.startDate = payload.startDate ? dayjs(payload.startDate).format('YYYY-MM-DD') : '';
      payload.endDate = payload.endDate ? dayjs(payload.endDate).format('YYYY-MM-DD') : '';
      payload.messageSendAt = payload.messageSendAt
        ? dayjs(payload.messageSendAt).format('YYYY-MM-DDTHH:mm:ss')
        : '';
      if (payload.couponId && typeof payload.couponId === 'object') {
        payload.couponId = payload.couponId.id;
      }
      if (payload.templateId && typeof payload.templateId === 'object') {
        payload.templateId = payload.templateId.value ?? payload.templateId.id;
      }
      payload.couponId = payload.couponId ? Number(payload.couponId) : null;
      payload.templateId = payload.templateId ? Number(payload.templateId) : null;
      payload.customerGradeId = payload.customerGradeId ? Number(payload.customerGradeId) : null;
      payload.tagId = payload.customerTagId ? Number(payload.customerTagId) : null;
      payload.staffId = authStore.userId ? Number(authStore.userId) : null;
      payload.shopId = authStore.shopId ? Number(authStore.shopId) : null;
      delete payload.customerTagId;
      emit('save', payload);
    }
  }
  function handleCancel() {
    emit('cancel');
  }
  function resetForm() {
    formData.value = {
      name: '',
      description: '',
      startDate: '',
      endDate: '',
      customerGradeId: '',
      customerTagId: '',
      couponId: '',
      messageSendAt: '',
      templateId: '',
    };
    selectedCoupon.value = null;
    errors.value = {};
  }

  onMounted(() => {
    if (!grades.value.length || !tags.value.length) {
      metadataStore.loadMetadata();
    }
    if (!authStore.shopId) {
      authStore.fetchShopId && authStore.fetchShopId();
    }
  });
</script>

<style scoped>
  .campaign-form {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
    min-width: 400px;
  }
  .form-row {
    display: flex;
    flex-direction: row;
    gap: 1.5rem;
  }
  .form-col {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }
  .form-label {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-neutral-dark);
  }
  .form-select {
    width: 100%;
    padding: 0.5rem 0.75rem;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    font-size: 14px;
    background: #fff;
  }
  .error-message {
    color: var(--color-error-300);
    font-size: 12px;
    line-height: 15.6px;
    margin-top: 2px;
  }
  .form-actions {
    display: flex;
    flex-direction: row;
    gap: 1rem;
    margin-top: 1.5rem;
  }
</style>
