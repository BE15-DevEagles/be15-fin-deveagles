<template>
  <WorkflowFormBase
    title="새 워크플로우"
    subtitle="고객 자동화 규칙을 설정하세요"
    save-button-text="저장"
    @save="saveWorkflow"
    @cancel="goBack"
  />
</template>

<script>
  import { useRouter } from 'vue-router';
  import { useToast } from '@/composables/useToast';
  import WorkflowFormBase from '../components/WorkflowFormBase.vue';
  import { createWorkflow } from '@/features/workflows/api/workflows.js';
  import { useAuthStore } from '@/store/auth.js';

  export default {
    name: 'WorkflowCreate',
    components: {
      WorkflowFormBase,
    },
    setup() {
      const router = useRouter();
      const toast = useToast();

      const goBack = () => {
        router.push('/workflows');
      };

      const saveWorkflow = async formData => {
        try {
          const authStore = useAuthStore();
          const payload = { ...formData, shopId: authStore.shopId, staffId: authStore.userId };
          await createWorkflow(payload);

          toast.showSuccess('워크플로우가 성공적으로 생성되었습니다.');
          router.push('/workflows');
        } catch (error) {
          console.error('Failed to save workflow:', error);
          toast.showError('워크플로우 생성 중 오류가 발생했습니다.');
        }
      };

      return {
        goBack,
        saveWorkflow,
      };
    },
  };
</script>
