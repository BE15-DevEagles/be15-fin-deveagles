<template>
  <BaseModal v-model="isVisible" title="필터" @close="close">
    <div class="filter-form">
      <!-- 판매일시 -->
      <div class="form-section">
        <label class="section-title">판매일시</label>
        <div class="range-inputs">
          <PrimeDatePicker v-model="startDate" placeholder="시작일" append-to="body" />
          <PrimeDatePicker v-model="endDate" placeholder="종료일" append-to="body" />
        </div>
      </div>

      <!-- 매출유형 -->
      <div class="form-section">
        <label class="section-title">매출 유형</label>
        <BaseForm
          v-model="types"
          type="checkbox"
          :options="[
            { value: '상품', text: '상품' },
            { value: '선불액·정기권', text: '선불액·정기권' },
            { value: '환불', text: '환불' },
          ]"
        />
      </div>

      <!-- 담당자 -->
      <div class="form-section">
        <BaseForm
          v-model="selectedStaff"
          type="select"
          label="담당자"
          :options="staffOptions.map(s => ({ value: s, text: s }))"
          placeholder="담당자 선택"
        />
      </div>
    </div>

    <!-- 푸터 -->
    <template #footer>
      <div class="footer-buttons">
        <BaseButton class="primary" outline @click="close">닫기</BaseButton>
        <BaseButton type="primary" @click="applyFilter">저장</BaseButton>
      </div>
    </template>
  </BaseModal>
</template>

<script setup>
  import { ref, watch } from 'vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';

  const props = defineProps({
    modelValue: Boolean,
  });

  const emit = defineEmits(['close', 'apply', 'update:modelValue']);

  const isVisible = ref(props.modelValue);
  const startDate = ref(null);
  const endDate = ref(null);
  const types = ref([]);
  const selectedStaff = ref('');

  const staffOptions = ['김경민', '홍길동', '김민지'];

  // v-model sync
  watch(
    () => props.modelValue,
    val => {
      isVisible.value = val;
    }
  );
  watch(isVisible, val => {
    emit('update:modelValue', val);
  });

  const close = () => {
    isVisible.value = false;
    emit('close');
  };

  const applyFilter = () => {
    const adjustedEndDate = endDate.value
      ? new Date(new Date(endDate.value).setHours(23, 59, 59, 999))
      : null;

    emit('apply', {
      startDate: startDate.value,
      endDate: adjustedEndDate,
      types: types.value,
      staff: selectedStaff.value,
    });
    close();
  };
</script>

<style scoped>
  .filter-form {
    padding: 1rem 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.2rem;
  }

  .form-section {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .section-title {
    font-weight: bold;
    font-size: 15px;
    margin-bottom: 0.25rem;
  }

  .range-inputs {
    display: flex;
    gap: 1rem;
  }

  .footer-buttons {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding: 0 1.5rem 1rem;
  }

  .footer-buttons button {
    min-height: 36px;
    padding: 6px 16px;
    font-size: 14px;
  }
</style>

<style>
  .p-datepicker {
    z-index: 10010 !important;
  }
</style>
