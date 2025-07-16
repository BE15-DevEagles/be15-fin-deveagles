<script setup>
  import BaseModal from '@/components/common/BaseModal.vue';
  import { computed } from 'vue';

  const props = defineProps({
    modelValue: Boolean,
    template: Object,
  });

  const emit = defineEmits(['update:modelValue']);

  const modalOpen = computed({
    get: () => props.modelValue,
    set: val => emit('update:modelValue', val),
  });
</script>

<template>
  <BaseModal v-model="modalOpen" title="템플릿 상세 보기">
    <div class="detail-content">
      <p><strong>템플릿명:</strong> {{ template?.templateName }}</p>
      <p><strong>유형:</strong> {{ template?.templateType || '없음' }}</p>
      <p><strong>등록일:</strong> {{ template?.createdAt }}</p>
      <p><strong>내용:</strong></p>
      <div class="content-box">{{ template?.templateContent }}</div>
    </div>
  </BaseModal>
</template>

<style scoped>
  .detail-content {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    font-size: 14px;
  }
  .content-box {
    background-color: #f9f9f9;
    padding: 1rem;
    border-radius: 8px;
    white-space: pre-wrap;
    border: 1px solid #ddd;
  }
</style>
