<template>
  <BaseModal v-model="modalVisible" title="필터">
    <div class="filter-form">
      <!-- 잔여 필드 -->
      <label class="section-title">잔여 {{ type === '선불' ? '선불권' : '횟수권' }}</label>
      <div class="range-inputs">
        <BaseForm
          v-model.number="min"
          type="number"
          step="100"
          :placeholder="`최소 잔여 ${type === '선불' ? '선불권' : '횟수권'}`"
        />
        <BaseForm
          v-model.number="max"
          type="number"
          step="100"
          :placeholder="`최대 잔여 ${type === '선불' ? '선불권' : '횟수권'}`"
        />
      </div>

      <!-- 만료 예정일 필드 -->
      <label class="section-title">만료(예정)일</label>
      <div class="range-inputs">
        <PrimeDatePicker v-model="startDate" placeholder="시작일 선택" class="date-picker" />
        <PrimeDatePicker v-model="endDate" placeholder="종료일 선택" class="date-picker" />
      </div>
    </div>

    <template #footer>
      <div class="footer-buttons">
        <BaseButton class="primary" outline @click="closeModal">닫기</BaseButton>
        <BaseButton class="primary" @click="applyFilter">저장</BaseButton>
      </div>
    </template>
  </BaseModal>
</template>

<script setup>
  import { ref, watch } from 'vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';

  const props = defineProps({
    modelValue: {
      type: Boolean,
      required: true,
    },
    type: {
      type: String,
      default: '선불',
    },
  });

  const emit = defineEmits(['update:modelValue', 'apply']);

  const modalVisible = ref(props.modelValue);
  watch(
    () => props.modelValue,
    val => (modalVisible.value = val)
  );
  watch(modalVisible, val => emit('update:modelValue', val));

  // 필터 상태
  const min = ref(null);
  const max = ref(null);
  const startDate = ref(null); // Date 객체
  const endDate = ref(null);

  const formatDateKST = (date, endOfDay = false) => {
    if (!date) return null;

    // 로컬 시간 기준 그대로 YYYY-MM-DD 추출
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');

    const time = endOfDay ? '23:59:59' : '00:00:00';
    return `${y}-${m}-${d}T${time}`;
  };
  // 모달 닫기
  const closeModal = () => {
    modalVisible.value = false;
  };

  // 필터 적용 → 상위로 emit
  const applyFilter = () => {
    const payload = {
      startDate: formatDateKST(startDate.value, false),
      endDate: formatDateKST(endDate.value, true),
    };

    if (props.type === '선불') {
      payload.minRemainingAmount = min.value;
      payload.maxRemainingAmount = max.value;
    } else {
      payload.minRemainingCount = min.value;
      payload.maxRemainingCount = max.value;
    }

    emit('apply', payload);
    closeModal();
  };
</script>

<style scoped>
  .filter-form {
    padding: 1rem 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }
  .section-title {
    font-weight: bold;
    font-size: 16px;
  }
  .range-inputs {
    display: flex;
    gap: 1rem;
  }
  .date-picker {
    flex: 1;
    min-width: 0;
  }
  .footer-buttons {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding: 0 1.5rem 1rem;
  }
</style>
