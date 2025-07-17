import api from '@/plugins/axios';
import { useAuthStore } from '@/store/auth.js';

// 워크플로우 생성
export const createWorkflow = async formData => {
  const authStore = useAuthStore();
  const {
    title,
    description,
    isActive,
    targetCustomerGrades,
    targetTags,
    excludeDormantCustomers,
    dormantPeriodMonths,
    excludeRecentMessageReceivers,
    recentMessagePeriodDays,
    trigger,
    triggerCategory,
    triggerConfig,
    action,
    actionConfig,
  } = formData;

  // Clean actionConfig for backend - remove display-only fields
  const cleanActionConfig = { ...actionConfig };
  delete cleanActionConfig.messageTemplateName;
  delete cleanActionConfig.couponName;
  delete cleanActionConfig.selectedTemplate;
  delete cleanActionConfig.selectedCoupon;

  const payload = {
    title,
    description,
    shopId: authStore.shopId,
    staffId: authStore.userId,
    targetCustomerGrades: JSON.stringify(targetCustomerGrades ?? []),
    targetTags: JSON.stringify(targetTags ?? []),
    excludeDormantCustomers,
    dormantPeriodMonths,
    excludeRecentMessageReceivers,
    recentMessagePeriodDays,
    triggerType: trigger,
    triggerCategory,
    triggerConfig: JSON.stringify(triggerConfig ?? {}),
    actionType: action,
    actionConfig: JSON.stringify(cleanActionConfig ?? {}),
    isActive,
  };

  const res = await api.post('/workflows', payload);
  return res.data?.data;
};

// 워크플로우 수정
export const updateWorkflow = async (workflowId, formData) => {
  const authStore = useAuthStore();
  const {
    title,
    description,
    isActive,
    targetCustomerGrades,
    targetTags,
    excludeDormantCustomers,
    dormantPeriodMonths,
    excludeRecentMessageReceivers,
    recentMessagePeriodDays,
    trigger,
    triggerCategory,
    triggerConfig,
    action,
    actionConfig,
  } = formData;

  // Clean actionConfig for backend - remove display-only fields
  const cleanActionConfig = { ...actionConfig };
  delete cleanActionConfig.messageTemplateName;
  delete cleanActionConfig.couponName;
  delete cleanActionConfig.selectedTemplate;
  delete cleanActionConfig.selectedCoupon;

  const payload = {
    title,
    description,
    shopId: authStore.shopId,
    staffId: authStore.userId,
    targetCustomerGrades: JSON.stringify(targetCustomerGrades ?? []),
    targetTags: JSON.stringify(targetTags ?? []),
    excludeDormantCustomers,
    dormantPeriodMonths,
    excludeRecentMessageReceivers,
    recentMessagePeriodDays,
    triggerType: trigger,
    triggerCategory,
    triggerConfig: JSON.stringify(triggerConfig ?? {}),
    actionType: action,
    actionConfig: JSON.stringify(cleanActionConfig ?? {}),
    isActive,
  };

  const res = await api.put(`/workflows/${workflowId}`, payload);
  return res.data?.data;
};

// 워크플로우 삭제
export const deleteWorkflow = async workflowId => {
  const res = await api.delete(`/workflows/${workflowId}`);
  return res.data?.data;
};

// 워크플로우 상태 토글
export const toggleWorkflowStatus = async workflowId => {
  const res = await api.patch(`/workflows/${workflowId}/toggle`);
  return res.data?.data;
};

// 워크플로우 상세 조회
export const getWorkflow = async workflowId => {
  const res = await api.get(`/workflows/${workflowId}`);
  return res.data?.data;
};

// 워크플로우 검색 (리스트)
export const searchWorkflows = async params => {
  // params: { searchQuery, statusFilter, triggerCategory, triggerType, actionType, isActive, page, size, sortBy, sortDirection }
  const merged = { page: 0, size: 20, ...params };
  const res = await api.get('/workflows', { params: merged });
  return res.data?.data;
};

// 워크플로우 통계 조회
export const getWorkflowStats = async () => {
  const res = await api.get('/workflows/stats');
  return res.data?.data;
};

// 트리거 카테고리별 워크플로우 조회
export const getWorkflowsByTriggerCategory = async triggerCategory => {
  const res = await api.get(`/workflows/trigger-category/${triggerCategory}`);
  return res.data?.data;
};

// 트리거 타입별 워크플로우 조회
export const getWorkflowsByTriggerType = async triggerType => {
  const res = await api.get(`/workflows/trigger-type/${triggerType}`);
  return res.data?.data;
};

// 최근 워크플로우 조회
export const getRecentWorkflows = async (limit = 10) => {
  const res = await api.get('/workflows/recent', { params: { limit } });
  return res.data?.data;
};

// 활성 워크플로우 조회
export const getActiveWorkflows = async () => {
  const res = await api.get('/workflows/active');
  return res.data?.data;
};
