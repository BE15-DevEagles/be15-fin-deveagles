<template>
  <div class="list-container workflow-list-container">
    <!-- Header -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="font-screen-title text-dark">마케팅 자동화</h1>
        <p class="font-body text-gray-500">고객 행동 기반 자동화 워크플로우를 관리하세요</p>
      </div>
      <BaseButton type="primary" @click="createWorkflow"> + 워크플로우 생성 </BaseButton>
    </div>

    <!-- Stats Cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon active">▶</div>
        <div class="stat-content">
          <span class="stat-number font-section-title text-dark">{{ activeWorkflows }}</span>
          <span class="stat-label font-small text-gray-500">활성 워크플로우</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon total">🔄</div>
        <div class="stat-content">
          <span class="stat-number font-section-title text-dark">{{ totalWorkflows }}</span>
          <span class="stat-label font-small text-gray-500">전체 워크플로우</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon trigger">⚡</div>
        <div class="stat-content">
          <span class="stat-number font-section-title text-dark">{{ totalTriggers }}</span>
          <span class="stat-label font-small text-gray-500">이번 달 실행</span>
        </div>
      </div>
    </div>

    <!-- Workflow Table -->
    <BaseCard>
      <BaseTable :columns="tableColumns" :data="paginatedWorkflows" :loading="loading" hover>
        <!-- Workflow Name Column -->
        <template #cell-name="{ item }">
          <div class="workflow-name">
            <h4 class="font-section-inner text-dark">{{ item.title }}</h4>
          </div>
        </template>

        <!-- 설명 Column -->
        <template #cell-description="{ item }">
          <div class="workflow-description">
            <p class="font-small text-gray-500">{{ item.description }}</p>
          </div>
        </template>

        <!-- 작성자 Column -->
        <template #cell-author="{ item }">
          <div class="workflow-author">
            <span class="font-small text-gray-700">
              {{ getStaffName(item.staffId) }}
            </span>
          </div>
        </template>

        <!-- Tags Column -->
        <template #cell-tags="{ item }">
          <div class="workflow-tags">
            <BaseBadge :key="item.triggerType" type="secondary" size="sm">
              {{ item.triggerType }}
            </BaseBadge>
            <BaseBadge :key="item.actionType" type="primary" size="sm">
              {{ item.actionType }}
            </BaseBadge>
          </div>
        </template>

        <!-- Stats Column -->
        <template #cell-stats="{ item }">
          <div class="workflow-stats">
            <span class="font-small text-gray-600">{{ item.executionCount }}회 실행</span>
            <span class="font-xs-semibold text-gray-400">{{
              formatDate(item.lastExecutedAt)
            }}</span>
          </div>
        </template>

        <!-- Active Status Column -->
        <template #cell-isActive="{ item }">
          <label class="toggle-switch enhanced">
            <input v-model="item.isActive" type="checkbox" @change="toggleWorkflowStatus(item)" />
            <span class="slider"></span>
          </label>
        </template>

        <!-- Actions Column -->
        <template #cell-actions="{ item }">
          <div class="action-buttons">
            <div class="tooltip-container">
              <!-- <BaseButton type="ghost" size="sm" class="icon-button" @click="editWorkflow(item)">
                <EditIcon :size="16" color="var(--color-gray-500)" />
              </BaseButton>
              <span class="tooltip tooltip-bottom tooltip-primary">수정</span>
            </div>
            <div class="tooltip-container"> -->

              <BaseButton
                :ref="`deleteBtn-${item.id}`"
                type="ghost"
                size="sm"
                class="icon-button"
                @click="deleteWorkflow(item, $event)"
              >
                <TrashIcon :size="16" color="var(--color-error-300)" />
              </BaseButton>
              <span class="tooltip tooltip-bottom tooltip-primary">삭제</span>
            </div>
          </div>
        </template>

        <!-- Empty State -->
        <template #empty>
          <div class="empty-state enhanced">
            <div class="empty-icon">🔄</div>
            <h3 class="font-section-title text-gray-700">워크플로우가 없습니다</h3>
            <p class="font-body text-gray-500">첫 번째 마케팅 자동화 워크플로우를 생성해보세요</p>
            <BaseButton type="primary" @click="navigateToCreate"> 워크플로우 생성하기 </BaseButton>
          </div>
        </template>
      </BaseTable>
    </BaseCard>

    <!-- Pagination -->
    <BasePagination
      v-if="filteredTotalPages > 1"
      :current-page="currentPage"
      :total-pages="filteredTotalPages"
      :total-items="filteredWorkflows.length"
      :items-per-page="itemsPerPage"
      @page-change="handlePageChange"
    />

    <!-- Delete Confirm Popover -->
    <BasePopover
      v-model="showDeleteConfirm"
      title="워크플로우 삭제"
      :message="deleteConfirmMessage"
      confirm-text="삭제"
      cancel-text="취소"
      confirm-type="error"
      placement="top"
      size="sm"
      :trigger-element="triggerElement"
      @confirm="confirmDelete"
      @cancel="cancelDelete"
    />

    <!-- Toast -->
    <BaseToast ref="toast" />
  </div>
</template>

<script>
  import { ref, computed, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { useListManagement } from '@/composables/useListManagement';
  import { MESSAGES } from '@/constants/messages';
  import { formatDate } from '@/utils/formatters';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseBadge from '@/components/common/BaseBadge.vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import BasePopover from '@/components/common/BasePopover.vue';
  import Pagination from '@/components/common/Pagination.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import BaseCard from '@/components/common/BaseCard.vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import TrashIcon from '@/components/icons/TrashIcon.vue';
  import EditIcon from '@/components/icons/EditIcon.vue';
  import {
    searchWorkflows,
    toggleWorkflowStatus as apiToggleStatus,
    deleteWorkflow as apiDelete,
  } from '@/features/workflows/api/workflows.js';
  import { useAuthStore } from '@/store/auth.js';
  import { useMetadataStore } from '@/store/metadata.js';

  export default {
    name: 'WorkflowList',
    components: {
      BaseButton,
      BaseForm,
      BaseBadge,
      BaseModal,
      BasePopover,
      BasePagination: Pagination,
      BaseToast,
      BaseCard,
      BaseTable,
      TrashIcon,
      EditIcon,
    },
    setup() {
      const router = useRouter();
      const authStore = useAuthStore();
      const metadataStore = useMetadataStore();
      metadataStore.loadMetadata();

      // List management composable
      const {
        items: workflows,
        currentPage,
        loading,
        showDeleteConfirm,
        selectedItem: selectedWorkflow,
        triggerElement,
        totalItems,
        totalPages,
        toggleItemStatus,
        deleteItem,
        confirmDelete,
        cancelDelete,
        handlePageChange,
        showNotImplemented,
      } = useListManagement({
        itemName: MESSAGES.WORKFLOW.ITEM_NAME,
        initialItems: [],
        itemsPerPage: 12,
      });

      // Local state
      const searchQuery = ref('');
      const statusFilter = ref('');
      const typeFilter = ref('');

      // Table columns
      const tableColumns = [
        { key: 'name', title: '워크플로우', width: '300px' },
        { key: 'description', title: '설명', width: '300px' },
        { key: 'author', title: '작성자', width: '150px' },
        { key: 'stats', title: '통계', width: '120px' },
        { key: 'isActive', title: '활성화', width: '100px' },
        { key: 'actions', title: 'Actions', width: '120px' },
      ];

      // Select options
      const statusOptions = [
        { value: '', text: '전체' },
        { value: 'active', text: '활성' },
        { value: 'inactive', text: '비활성' },
      ];

      const typeOptions = [
        { value: '', text: '전체 유형' },
        { value: 'visit-cycle', text: '방문 주기' },
        { value: 'specific-treatment', text: '특정 시술' },
        { value: 'birthday', text: '생일' },
        { value: 'registration-days-after', text: '등록 후 경과' },
        { value: 'visit-milestone', text: '방문 마일스톤' },
        { value: 'amount-milestone', text: '금액 마일스톤' },
      ];

      // Computed
      const activeWorkflows = computed(() => workflows.value.filter(w => w.isActive).length);

      const totalWorkflows = computed(() => workflows.value.length);

      const totalTriggers = computed(() =>
        workflows.value.reduce((sum, w) => sum + (w.executionCount || 0), 0)
      );

      const filteredWorkflows = computed(() => {
        let filtered = workflows.value;

        if (searchQuery.value) {
          filtered = filtered.filter(
            w =>
              w.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
              w.description.toLowerCase().includes(searchQuery.value.toLowerCase())
          );
        }

        if (statusFilter.value) {
          filtered = filtered.filter(w => {
            if (statusFilter.value === 'active') return w.isActive;
            if (statusFilter.value === 'inactive') return !w.isActive;
            return true;
          });
        }

        if (typeFilter.value) {
          filtered = filtered.filter(w => w.triggerType === typeFilter.value);
        }

        return filtered;
      });

      // 페이지네이션을 위한 표시용 워크플로우
      const paginatedWorkflows = computed(() => {
        const start = (currentPage.value - 1) * 12;
        const end = start + 12;
        return filteredWorkflows.value.slice(start, end);
      });

      // 총 페이지 수 계산 (필터링된 결과 기준)
      const filteredTotalPages = computed(() => Math.ceil(filteredWorkflows.value.length / 12));

      const deleteConfirmMessage = computed(() =>
        selectedWorkflow.value ? MESSAGES.WORKFLOW.DELETE_CONFIRM(selectedWorkflow.value.title) : ''
      );

      // Methods
      const createWorkflow = () => router.push('/workflows/create');
      const navigateToCreate = () => router.push('/workflows/create');
      const editWorkflow = workflow => {
        router.push(`/workflows/edit/${workflow.id}`);
      };
      const toggleWorkflowStatus = async workflow => {
        try {
          await apiToggleStatus(workflow.id);
          workflow.isActive = !workflow.isActive;
          toggleItemStatus(workflow);
        } catch (err) {
          console.error('상태 토글 실패', err);
        }
      };
      const deleteWorkflow = (workflow, event) => deleteItem(workflow, event);

      const getStatusText = workflow => {
        return workflow.isActive ? '활성' : '비활성';
      };

      function getStaffName(staffId) {
        const staff = metadataStore.staff.find(s => s.id == staffId);
        return staff ? staff.name : staffId;
      }

      // 필터 변경 시 페이지 리셋
      const resetPageOnFilterChange = () => {
        currentPage.value = 1;
      };

      // 필터 변경 감지
      const handleSearchChange = value => {
        searchQuery.value = value;
        resetPageOnFilterChange();
      };

      const handleStatusFilterChange = value => {
        statusFilter.value = value;
        resetPageOnFilterChange();
      };

      const handleTypeFilterChange = value => {
        typeFilter.value = value;
        resetPageOnFilterChange();
      };

      // 서버에서 워크플로우 목록 가져오기
      const fetchWorkflows = async () => {
        try {
          loading.value = true;
          // authStore.shopId는 searchWorkflows 내부에서 기본값으로 병합됨
          const res = await searchWorkflows({ page: 0, size: 100 });
          const items = res?.content ?? res ?? [];
          workflows.value.splice(0, workflows.value.length, ...items);
        } catch (err) {
          console.error('워크플로우 목록 조회 실패', err);
        } finally {
          loading.value = false;
        }
      };

      onMounted(() => {
        fetchWorkflows();
      });

      // confirmDelete wrapper to call API
      const confirmDeleteWrapper = async () => {
        if (selectedWorkflow.value) {
          try {
            await apiDelete(selectedWorkflow.value.id, authStore.shopId, authStore.userId);
            confirmDelete();
          } catch (err) {
            console.error('삭제 실패', err);
          }
        }
      };

      return {
        // State
        workflows,
        currentPage,
        loading,
        showDeleteConfirm,
        selectedWorkflow,
        triggerElement,
        searchQuery,
        statusFilter,
        typeFilter,

        // Computed
        totalItems,
        totalPages,
        activeWorkflows,
        totalWorkflows,
        totalTriggers,
        filteredWorkflows,
        paginatedWorkflows,
        filteredTotalPages,
        deleteConfirmMessage,

        // Data
        tableColumns,
        statusOptions,
        typeOptions,
        itemsPerPage: 12, // 페이지네이션을 위해 추가

        // Methods
        createWorkflow,
        navigateToCreate,
        editWorkflow,
        toggleWorkflowStatus,
        deleteWorkflow,
        confirmDelete: confirmDeleteWrapper,
        cancelDelete,
        handlePageChange,
        handleSearchChange,
        handleStatusFilterChange,
        handleTypeFilterChange,
        getStatusText,
        formatDate,
        getStaffName,
      };
    },
  };
</script>

<style scoped>
  @import '@/assets/css/list-components.css';

  /* 워크플로우 전용 스타일 */
  .workflow-list-container {
    padding: 24px;
    max-width: 1400px;
    margin: 0 auto;
  }

  .page-header {
    align-items: flex-start;
  }

  .filter-group {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
  }
</style>
