<script setup>
  import BaseModal from '@/components/common/BaseModal.vue';
  import { computed } from 'vue';

  const props = defineProps({
    modelValue: Boolean,
    template: Object,
    grades: Array,
  });

  const emit = defineEmits(['update:modelValue']);

  const modalOpen = computed({
    get: () => props.modelValue,
    set: val => emit('update:modelValue', val),
  });

  const customerGradeName = computed(() => {
    const gradeId = props.template?.customerGradeId;
    if (!gradeId) return '전체';
    return props.grades?.find(g => g.id === gradeId)?.name || '전체';
  });
</script>

<template>
  <BaseModal v-model="modalOpen" title="템플릿 상세 보기">
    <div class="template-detail-wrapper">
      <div class="card">
        <div class="row">
          <span class="label">템플릿명</span>
          <span class="value">{{ template?.templateName || '-' }}</span>
        </div>
        <div class="row">
          <span class="label">유형</span>
          <span class="value">{{ template?.templateType || '-' }}</span>
        </div>
        <div class="row">
          <span class="label">등록일</span>
          <span class="value">{{ template?.createdAt || '-' }}</span>
        </div>
        <div class="row">
          <span class="label">등급</span>
          <span class="value">{{ customerGradeName }}</span>
        </div>
      </div>

      <div class="card content-card">
        <div class="content-title">템플릿 내용</div>
        <div class="content-body">
          {{ template?.templateContent || '내용 없음' }}
        </div>
      </div>
    </div>
  </BaseModal>
</template>

<style scoped>
  .template-detail-wrapper {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
    padding: 4px 0;
  }

  .card {
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 1.25rem 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .row {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .label {
    font-weight: 600;
    color: #4b5563;
    font-size: 14px;
    min-width: 90px;
  }

  .value {
    flex: 1;
    text-align: right;
    font-size: 14px;
    color: #111827;
  }

  .content-card .content-title {
    font-weight: 600;
    font-size: 14px;
    color: #374151;
    margin-bottom: 6px;
  }

  .content-body {
    background-color: #f9fafb;
    border: 1px solid #e5e7eb;
    padding: 1rem;
    border-radius: 6px;
    white-space: pre-wrap;
    font-size: 14px;
    color: #1f2937;
    line-height: 1.5;
  }
</style>
