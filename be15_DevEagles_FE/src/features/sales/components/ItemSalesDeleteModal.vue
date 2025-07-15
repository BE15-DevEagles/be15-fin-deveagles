<template>
  <div>
    <BaseModal
      :model-value="modelValue"
      title="상품 매출 삭제"
      animation-class="back-in-left"
      @update:model-value="$emit('update:modelValue', $event)"
    >
      <div class="confirm-message">
        <p class="main">정말 삭제하시겠습니까?</p>
        <p class="sub">삭제된 데이터는 복구할 수 없습니다.</p>
      </div>

      <template #footer>
        <div class="button-group">
          <BaseButton @click="$emit('update:modelValue', false)">취소</BaseButton>
          <BaseButton class="danger" @click="handleDelete">삭제</BaseButton>
        </div>
      </template>
    </BaseModal>
  </div>
</template>

<script setup>
  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import { deleteSales } from '@/features/sales/api/sales.js';

  const props = defineProps({
    modelValue: Boolean,
    salesId: {
      type: Number,
      required: true,
    },
  });

  const emit = defineEmits(['update:modelValue', 'confirm']);

  const handleDelete = async () => {
    try {
      await deleteSales(props.salesId);
      emit('confirm'); // ✅ 삭제 성공 시 부모에 알림
      alert('삭제가 완료되었습니다.');
      window.location.reload();
    } catch (error) {
      console.error('상품 매출 삭제 실패:', error);
      alert('삭제 중 오류가 발생했습니다.');
    } finally {
      emit('update:modelValue', false); // 모달 닫기
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
