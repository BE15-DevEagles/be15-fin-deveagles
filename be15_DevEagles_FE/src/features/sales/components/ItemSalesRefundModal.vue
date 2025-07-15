<template>
  <BaseModal
    :model-value="modelValue"
    title="상품 매출 환불"
    animation-class="back-in-left"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="confirm-message">
      <p class="main">정말 환불하시겠습니까?</p>
      <p class="sub">환불된 상품 매출은 복구할 수 없습니다.</p>
    </div>

    <template #footer>
      <div class="button-group">
        <BaseButton @click="$emit('update:modelValue', false)">취소</BaseButton>
        <BaseButton class="danger" @click="handleRefund">삭제</BaseButton>
      </div>
    </template>
  </BaseModal>
</template>

<script setup>
  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import { refundSales } from '@/features/sales/api/sales.js';

  const props = defineProps({
    modelValue: Boolean,
    salesId: Number,
  });

  const emit = defineEmits(['update:modelValue', 'confirm']);

  const handleRefund = async () => {
    try {
      await refundSales(props.salesId);
      emit('confirm'); // 성공 시 상위에서 후처리 (ex: 목록 새로고침)
      emit('update:modelValue', false);
      alert('환불이 완료되었습니다.');
      window.location.reload();
    } catch (e) {
      console.error('[환불 실패]', e);
      alert('환불 처리 중 오류가 발생했습니다.');
    }
  };
</script>

<style scoped>
  .confirm-message {
    text-align: center;
    margin: 1.5rem 0;
  }
  .confirm-message .main {
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 0.5rem;
  }
  .confirm-message .sub {
    font-size: 14px;
    color: #666;
  }
  .button-group {
    display: flex;
    justify-content: center;
    gap: 12px;
  }
  .danger {
    background-color: red;
    color: white;
    border-color: red;
  }
</style>
