<script setup>
  import { ref, watch } from 'vue';
  import TemplatesAPI from '@/features/messages/api/templates.js';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseModal from '@/components/common/BaseModal.vue';

  const props = defineProps({
    modelValue: Boolean,
    template: Object,
  });
  const emit = defineEmits(['update:modelValue', 'deleted']);

  const modelValue = ref(props.modelValue);

  watch(
    () => props.modelValue,
    val => {
      modelValue.value = val;
    }
  );

  async function handleDelete() {
    try {
      await TemplatesAPI.deleteTemplate(props.template.templateId);
      emit('deleted');
      emit('update:modelValue', false);
    } catch (e) {
      alert('삭제 실패');
    }
  }
</script>

<template>
  <BaseModal v-model="modelValue" title="템플릿 삭제">
    <p class="text-sm text-gray-700">
      "{{ props.template?.templateName }}" 템플릿을 정말 삭제하시겠습니까?
    </p>
    <template #footer>
      <BaseButton type="error" @click="handleDelete">삭제</BaseButton>
      <BaseButton @click="emit('update:modelValue', false)">취소</BaseButton>
    </template>
  </BaseModal>
</template>
