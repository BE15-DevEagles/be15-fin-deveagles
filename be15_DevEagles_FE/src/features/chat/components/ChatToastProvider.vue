<template>
  <BaseToast ref="toastRef" position="top-right" />
</template>

<script setup>
  import { ref, onMounted } from 'vue';
  import { useChatStore } from '@/store/useChatStore';
  import BaseToast from '@/components/common/BaseToast.vue';

  const chatStore = useChatStore();
  const toastRef = ref(null);

  onMounted(() => {
    chatStore.setToastHandler(async msg => {
      const title = '새 메시지';
      const rawContent = msg?.content || '확인해보세요.';
      const maxLength = 20;
      const content =
        rawContent.length > maxLength ? rawContent.substring(0, maxLength) + '...' : rawContent;

      toastRef.value?.success(`${title}: ${content}`, {
        duration: 5000,
        closable: true,
        showIcon: true,
      });
    });
  });
</script>
