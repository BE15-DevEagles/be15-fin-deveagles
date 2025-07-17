<template>
  <WorkflowFormBase
    title="워크플로우 수정"
    subtitle="기존 워크플로우를 수정하세요"
    save-button-text="수정"
    :initial-data="workflowData"
    @save="updateWorkflow"
    @cancel="goBack"
  />
</template>

<script>
  import { ref, onMounted } from 'vue';
  import { useRouter, useRoute } from 'vue-router';
  import { useToast } from '@/composables/useToast';
  import WorkflowFormBase from '../components/WorkflowFormBase.vue';
  import { getWorkflow, updateWorkflow } from '@/features/workflows/api/workflows.js';
  import { useAuthStore } from '@/store/auth.js';

  export default {
    name: 'WorkflowEdit',
    components: {
      WorkflowFormBase,
    },
    setup() {
      const router = useRouter();
      const route = useRoute();
      const toast = useToast();
      const workflowData = ref(null);

      // API 데이터 → formData 변환 함수
      function mapApiToFormData(data) {
        console.log('[mapApiToFormData] input:', data);
        // 등급/태그는 id 배열로만 저장되어 있음 → 객체 배열로 변환
        const grades = (data.targetCustomerGrades || []).map(g =>
          typeof g === 'object' ? g.id : g
        );
        const tags = (data.targetTags || []).map(t => (typeof t === 'object' ? t.id : t));

        // triggerConfig, actionConfig는 json string일 수도 있으니 파싱
        let triggerConfig = {};
        if (typeof data.triggerConfig === 'string') {
          try {
            triggerConfig = JSON.parse(data.triggerConfig);
          } catch {
            triggerConfig = {};
          }
        } else {
          triggerConfig = data.triggerConfig || {};
        }

        let actionConfig = {};
        if (typeof data.actionConfig === 'string') {
          try {
            actionConfig = JSON.parse(data.actionConfig);
          } catch {
            actionConfig = {};
          }
        } else {
          actionConfig = data.actionConfig || {};
        }

        // sendTime: "14:30" → Date 객체
        if (actionConfig.sendTime && typeof actionConfig.sendTime === 'string') {
          const [h, m] = actionConfig.sendTime.split(':');
          const d = new Date();
          d.setHours(Number(h), Number(m), 0, 0);
          actionConfig.sendTime = d;
        }

        // selectedTemplate, selectedCoupon: select 컴포넌트용 객체로 복원
        if (actionConfig.messageTemplateId) {
          actionConfig.selectedTemplate = {
            id: actionConfig.messageTemplateId,
            value: actionConfig.messageTemplateId,
            name: actionConfig.messageTemplateName || '',
            text: actionConfig.messageTemplateName || '',
          };
        }
        if (actionConfig.couponId) {
          actionConfig.selectedCoupon = {
            id: actionConfig.couponId,
            value: actionConfig.couponId,
            name: actionConfig.couponName || '',
            text: actionConfig.couponName || '',
            couponTitle: actionConfig.couponName || '',
          };
        }

        const result = {
          id: data.id,
          title: data.title,
          description: data.description,
          isActive: data.isActive,
          targetCustomerGrades: grades,
          targetTags: tags,
          excludeDormantCustomers: data.excludeDormantCustomers,
          dormantPeriodMonths: data.dormantPeriodMonths,
          excludeRecentMessageReceivers: data.excludeRecentMessageReceivers,
          recentMessagePeriodDays: data.recentMessagePeriodDays,
          trigger: data.triggerType,
          triggerCategory: data.triggerCategory,
          triggerConfig,
          action: data.actionType,
          actionConfig,
        };
        console.log('[mapApiToFormData] output:', result);
        return result;
      }

      const fetchWorkflowData = async id => {
        console.log('[fetchWorkflowData] call getWorkflow with id:', id);
        const data = await getWorkflow(id);
        console.log('[fetchWorkflowData] getWorkflow result:', data);
        const mapped = mapApiToFormData(data);
        console.log('[fetchWorkflowData] mapped formData:', mapped);
        return mapped;
      };

      const updateWorkflowRequest = async formData => {
        try {
          const authStore = useAuthStore();
          const payload = { ...formData, shopId: authStore.shopId, staffId: authStore.userId };
          console.log('[updateWorkflowRequest] payload:', payload);
          await updateWorkflow(route.params.id, payload);

          toast.showSuccess('워크플로우가 성공적으로 수정되었습니다.');
          router.push('/workflows');
        } catch (error) {
          console.error('Failed to update workflow:', error);
          toast.showError('워크플로우 수정 중 오류가 발생했습니다.');
        }
      };

      const goBack = () => {
        router.push('/workflows');
      };

      onMounted(async () => {
        try {
          const workflowId = route.params.id;
          if (!workflowId) {
            toast.showError('워크플로우 ID가 올바르지 않습니다.');
            router.push('/workflows');
            return;
          }

          console.log('[onMounted] Fetching workflow data for ID:', workflowId);
          workflowData.value = await fetchWorkflowData(workflowId);
          console.log('[onMounted] Workflow data loaded successfully:', workflowData.value);
        } catch (error) {
          console.error('Error loading workflow data:', error);
          console.error('Error response:', error.response?.data);
          console.error('Error status:', error.response?.status);
          toast.showError(
            `워크플로우 데이터를 불러오는데 실패했습니다: ${error.response?.data?.message || error.message}`
          );
          router.push('/workflows');
        } finally {
          // Loading complete
        }
      });

      return {
        workflowData,
        updateWorkflow: updateWorkflowRequest,
        goBack,
      };
    },
  };
</script>
