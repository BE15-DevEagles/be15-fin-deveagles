<script setup>
  import BaseConfirm from '@/components/common/BaseConfirm.vue';
  import { defineProps, defineEmits } from 'vue';

  const props = defineProps({
    modelValue: {
      type: Boolean,
      required: true,
    },
    messageContent: {
      type: Object,
      required: false,
      default: () => ({}),
    },
    customers: {
      type: Array,
      default: () => [],
    },
  });

  const emit = defineEmits(['update:modelValue', 'confirm', 'cancel']);

  function onConfirm() {
    emit('confirm');
    emit('update:modelValue', false);
  }

  function onCancel() {
    emit('cancel');
    emit('update:modelValue', false);
  }
</script>

<template>
  <BaseConfirm
    :model-value="modelValue"
    title="메시지 전송"
    confirm-text="보내기"
    cancel-text="취소"
    confirm-type="primary"
    @update:model-value="val => emit('update:modelValue', val)"
    @confirm="onConfirm"
    @cancel="onCancel"
  >
    <div class="preview-wrapper">
      <div class="preview-message">
        <h3 class="preview-title">전송할 메시지를 다시 확인해주세요</h3>

        <div class="preview-section">
          <div class="preview-row">
            <span class="label">내용</span>
            <p class="value">{{ messageContent.content || '없음' }}</p>
          </div>
          <div class="preview-row">
            <span class="label">쿠폰</span>
            <p class="value">{{ messageContent.coupon?.name || '없음' }}</p>
          </div>
          <div class="preview-row">
            <span class="label">링크</span>
            <p class="value">{{ messageContent.link || '없음' }}</p>
          </div>
        </div>

        <div class="preview-section">
          <div class="preview-row">
            <span class="label">고객</span>
            <div class="customer-chip-wrap">
              <template v-if="customers.length">
                <span
                  v-for="(customer, idx) in customers.slice(0, 5)"
                  :key="idx"
                  class="customer-chip"
                >
                  {{ customer.name }}
                </span>
                <span v-if="customers.length > 5" class="more-chip">
                  +{{ customers.length - 5 }} 더 보기
                </span>
              </template>
              <span v-else class="value">없음</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </BaseConfirm>
</template>

<style scoped>
  .preview-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 24px;
  }

  .preview-message {
    background-color: var(--color-gray-50);
    border: 1px solid var(--color-gray-200);
    padding: 28px 36px;
    border-radius: 14px;
    font-size: 14px;
    color: var(--color-gray-800);
    width: 100%;
    max-width: 720px;
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.05);
  }

  .preview-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-gray-900);
    margin-bottom: 20px;
    text-align: center;
  }

  .preview-section {
    margin-bottom: 20px;
  }

  .preview-row {
    display: flex;
    gap: 12px;
    align-items: flex-start;
    margin-bottom: 8px;
    flex-wrap: wrap;
  }

  .label {
    font-weight: 600;
    min-width: 52px;
    color: var(--color-gray-700);
  }

  .value {
    flex: 1;
    color: var(--color-gray-900);
    margin: 0;
    white-space: pre-wrap;
  }

  .customer-chip-wrap {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 8px;
  }

  .customer-chip {
    background-color: var(--color-gray-100);
    color: var(--color-gray-800);
    font-size: 13px;
    padding: 6px 10px;
    border-radius: 14px;
  }

  .more-chip {
    background-color: var(--color-primary-50);
    color: var(--color-primary-600);
    font-size: 13px;
    padding: 6px 10px;
    border-radius: 14px;
  }
</style>
