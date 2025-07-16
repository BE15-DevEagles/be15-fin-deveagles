<script setup>
  import { ref, onMounted } from 'vue';
  import TemplatesAPI from '@/features/messages/api/templates.js';
  import { PlusIcon } from 'lucide-vue-next';

  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseCard from '@/components/common/BaseCard.vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import Pagination from '@/components/common/Pagination.vue';
  import BaseToast from '@/components/common/BaseToast.vue';

  import TemplateCreateModal from '@/features/messages/components/modal/TemplateCreateModal.vue';
  import TemplateEditModal from '@/features/messages/components/modal/TemplateEditModal.vue';
  import TemplateDeleteModal from '@/features/messages/components/modal/TemplateDeleteModal.vue';
  import TemplateDetailModal from '@/features/messages/components/modal/TemplateDetailModal.vue';
  import EditIcon from '@/components/icons/EditIcon.vue';
  import TrashIcon from '@/components/icons/TrashIcon.vue';

  const templates = ref([]);
  const currentPage = ref(1);
  const itemsPerPage = 10;
  const totalItems = ref(0);
  const totalPages = ref(0);
  const loading = ref(false);

  const typeLabelMap = {
    advertising: '광고',
    announcement: '안내',
    etc: '기타',
  };

  const showCreateModal = ref(false);
  const showEditModal = ref(false);
  const showDeleteModal = ref(false);
  const showDetailModal = ref(false);

  const editTarget = ref(null);
  const deleteTarget = ref(null);
  const detailTarget = ref(null);
  const toast = ref(null);

  const tableColumns = [
    { key: 'templateName', title: '템플릿명', width: '20%', headerClass: 'text-center' },
    { key: 'templateType', title: '유형', width: '10%', headerClass: 'text-center' },
    { key: 'templateContent', title: '내용', width: '40%', headerClass: 'text-center' },
    { key: 'createdAt', title: '등록일자', width: '20%', headerClass: 'text-center' },
    { key: 'actions', title: '관리', width: '10%', headerClass: 'text-center' },
  ];

  async function fetchTemplates() {
    loading.value = true;
    try {
      const params = {
        page: currentPage.value - 1,
        size: itemsPerPage,
      };
      const result = await TemplatesAPI.getTemplates(params);
      templates.value = result.data.content;
      totalPages.value = result.data.pagination.totalPages;
      totalItems.value = result.data.pagination.totalItems;
      currentPage.value = result.data.pagination.currentPage + 1; // ← 0-based → 1-based
    } catch (e) {
      toast.value?.error('템플릿 목록 조회에 실패했습니다.');
    } finally {
      loading.value = false;
    }
  }
  onMounted(() => {
    fetchTemplates();
  });

  function onPageChange(page) {
    currentPage.value = page;
    fetchTemplates();
  }

  function openCreateModal() {
    showCreateModal.value = true;
  }
  function openEditModal(template) {
    editTarget.value = { ...template };
    showEditModal.value = true;
  }
  function openDeleteModal(template) {
    deleteTarget.value = template;
    showDeleteModal.value = true;
  }
  function openDetailModal(template) {
    detailTarget.value = template;
    showDetailModal.value = true;
  }
</script>

<template>
  <div class="template-list-view">
    <div class="template-list-header">
      <h2 class="font-section-title text-dark">템플릿 보관함</h2>
      <BaseButton type="primary" size="sm" @click="openCreateModal">
        <PlusIcon class="icon" /> 템플릿 등록
      </BaseButton>
    </div>

    <BaseCard>
      <BaseTable :columns="tableColumns" :data="templates" :loading="loading" hover>
        <template #cell-templateName="{ item }">
          <div class="text-center">{{ item.templateName }}</div>
        </template>

        <template #cell-templateType="{ item }">
          <div class="text-center">
            <span class="badge">{{ typeLabelMap[item.templateType] || '기타' }}</span>
          </div>
        </template>

        <template #cell-templateContent="{ item }">
          <div class="text-center clickable" @click="openDetailModal(item)">
            <div class="truncate">{{ item.templateContent }}</div>
          </div>
        </template>

        <template #cell-createdAt="{ item }">
          <div class="text-center">
            {{ item.createdAt ? item.createdAt.slice(0, 10) : '-' }}
          </div>
        </template>

        <template #cell-actions="{ item }">
          <div class="action-cell text-center">
            <BaseButton size="xs" icon aria-label="수정" @click="openEditModal(item)">
              <EditIcon class="icon" />
            </BaseButton>
            <BaseButton size="xs" icon aria-label="삭제" @click="openDeleteModal(item)">
              <TrashIcon class="icon" />
            </BaseButton>
          </div>
        </template>

        <template #empty>
          <div class="empty-state">
            <p class="text-gray-500">등록된 템플릿이 없습니다.</p>
            <BaseButton type="primary" @click="openCreateModal"> 템플릿 생성하기 </BaseButton>
          </div>
        </template>
      </BaseTable>
    </BaseCard>

    <Pagination
      v-if="totalPages > 1"
      :current-page="currentPage"
      :total-pages="totalPages"
      :total-items="totalItems"
      :items-per-page="itemsPerPage"
      @page-change="onPageChange"
    />

    <TemplateCreateModal v-model="showCreateModal" @success="fetchTemplates" />
    <TemplateEditModal
      v-if="editTarget"
      v-model="showEditModal"
      :template="editTarget"
      @success="fetchTemplates"
    />
    <TemplateDeleteModal
      v-model="showDeleteModal"
      :template="deleteTarget"
      @deleted="fetchTemplates"
    />
    <TemplateDetailModal v-model="showDetailModal" :template="detailTarget" />

    <BaseToast ref="toast" />
  </div>
</template>

<style scoped>
  .template-list-view {
    padding: 24px;
    max-width: 1200px;
    margin: 0 auto;
  }
  .template-list-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 1.5rem;
  }
  .truncate {
    max-width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .clickable {
    cursor: pointer;
    color: var(--color-primary-main);
  }
  .action-cell {
    display: flex;
    justify-content: center;
    gap: 0.5rem;
  }
  .badge {
    display: inline-block;
    background-color: var(--color-gray-100);
    border-radius: 4px;
    padding: 2px 8px;
    font-size: 12px;
    color: var(--color-text-secondary);
  }
</style>
