<script setup>
  import { ref, onMounted, watch, nextTick } from 'vue';
  import messagesAPI from '@/features/messages/api/message.js';
  import couponsAPI from '@/features/coupons/api/coupons.js';
  import customersAPI from '@/features/customer/api/customers.js';
  import GradesAPI from '@/features/customer/api/grades.js';
  import TagsAPI from '@/features/customer/api/tags.js';
  import TemplatesAPI from '@/features/messages/api/templates.js';

  import MessageItem from '../components/MessageItem.vue';
  import MessageStats from '../components/MessageStats.vue';
  import MessageSendModal from '../components/modal/MessageSendModal.vue';
  import SendConfirmModal from '../components/modal/SendConfirmModal.vue';
  import ReservationSendModal from '../components/modal/ReservationSendModal.vue';
  import TemplateSelectDrawer from '@/features/messages/components/drawer/TemplateSelectDrawer.vue';
  import CustomerSelectDrawer from '@/features/messages/components/drawer/CustomerSelectDrawer.vue';
  import MessageDetailModal from '../components/modal/MessageDetailModal.vue';
  import EditReservationModal from '../components/modal/EditReservationModal.vue';

  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import Pagination from '@/components/common/Pagination.vue';
  import BaseCard from '@/components/common/BaseCard.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import BaseModal from '@/components/common/BaseModal.vue';

  const messages = ref([]);
  const allMessages = ref([]);
  const selectedCustomers = ref([]);
  const selectedMessage = ref(null);
  const messageToEdit = ref(null);
  const messageToSend = ref({
    content: '',
    link: '',
    coupon: null,
    grades: [],
    tags: [],
    customers: [],
  });

  const availableCoupons = ref([]);
  const allCustomers = ref([]);
  const customerGrades = ref([]);
  const tags = ref([]);

  const templateList = ref({
    content: [],
    pagination: { currentPage: 0, totalPages: 1, totalItems: 0 },
  });

  const showSendModal = ref(false);
  const showSendConfirm = ref(false);
  const showReserveModal = ref(false);
  const showTemplateDrawer = ref(false);
  const showCustomerDrawer = ref(false);
  const showDeleteConfirm = ref(false);
  const showDetailModal = ref(false);
  const showEditModal = ref(false);

  const toast = ref(null);
  const triggerElement = ref(null);

  const totalElements = ref(0);
  const totalPages = ref(0);
  const currentPage = ref(1);
  const itemsPerPage = 10;
  const isLoading = ref(false);
  const statusFilter = ref('all');

  const columns = [
    { key: 'title', title: '제목', width: '18%', headerClass: 'text-center' },
    { key: 'content', title: '내용', width: '35%', headerClass: 'text-center' },
    { key: 'receiver', title: '수신자', width: '12%', headerClass: 'text-center' },
    { key: 'status', title: '상태', width: '10%', headerClass: 'text-center' },
    { key: 'date', title: '날짜', width: '15%', headerClass: 'text-center' },
    { key: 'actions', title: '관리', width: '10%', headerClass: 'text-center' },
  ];

  async function loadMessages() {
    try {
      isLoading.value = true;
      const params = { page: currentPage.value - 1, size: itemsPerPage };
      if (statusFilter.value !== 'all') params.status = statusFilter.value;
      const {
        content,
        totalElements: total,
        totalPages: pages,
      } = await messagesAPI.fetchMessages(params);
      messages.value = content;
      totalElements.value = total;
      totalPages.value = pages;
    } catch (e) {
      await nextTick();
      toast.value?.error('메시지 조회 실패');
    } finally {
      isLoading.value = false;
    }
  }

  async function loadAllMessagesForStats() {
    try {
      let page = 0;
      let finished = false;
      const size = 100;
      const tempAll = [];
      while (!finished) {
        const { content, totalPages } = await messagesAPI.fetchMessages({ page, size });
        tempAll.push(...content);
        page++;
        if (page >= totalPages) finished = true;
      }
      allMessages.value = tempAll;
    } catch (e) {
      console.error('통계 메시지 로드 실패', e);
    }
  }

  function handleDelete(msg, event) {
    selectedMessage.value = msg;
    triggerElement.value = event.currentTarget;
    showDeleteConfirm.value = true;
  }
  function cancelDelete() {
    showDeleteConfirm.value = false;
  }
  function confirmDelete() {
    messages.value = messages.value.filter(m => m.id !== selectedMessage.value.id);
    showDeleteConfirm.value = false;
  }

  function handleSendRequest(payload, type) {
    messageToSend.value = {
      content: payload.messageContent,
      link: payload.link,
      coupon: payload.coupon,
      grades: payload.grades,
      tags: payload.tags,
      customerIds: payload.customerIds,
      messageKind: payload.messageKind,
      messageType: payload.messageType,
      messageSendingType: payload.messageSendingType,
    };

    nextTick(() => {
      if (type === 'IMMEDIATE') showSendConfirm.value = true;
      else if (type === 'RESERVATION') showReserveModal.value = true;
    });
  }
  function handleSendConfirm() {
    const payload = {
      ...messageToSend.value,
      messageContent: messageToSend.value.content, // ✅ 이 줄을 반드시 추가!
    };

    messagesAPI
      .sendMessage(payload)
      .then(() => {
        toast.value?.success('메시지 발송 성공');
        showSendConfirm.value = false;
        loadMessages();
        loadAllMessagesForStats();
      })
      .catch(() => toast.value?.error('메시지 발송 실패'));
  }
  function handleReserveConfirm(payload) {
    messagesAPI
      .sendMessage(payload)
      .then(() => {
        toast.value?.success('예약 메시지 성공');
        showReserveModal.value = false;
        loadMessages();
        loadAllMessagesForStats();
      })
      .catch(() => toast.value?.error('예약 메시지 실패'));
  }

  function handleOpenTemplateDrawer() {
    loadTemplatesForDrawer(1);
    showTemplateDrawer.value = true;
  }
  function handleOpenCustomerDrawer() {
    showCustomerDrawer.value = true;
  }
  function handleTemplateSelect(template) {
    messageToSend.value = {
      ...messageToSend.value,
      content: template.templateContent,
      templateId: template.templateId,
      grades: template.grades || [],
      tags: template.tags || [],
      link: template.link || '',
      coupon: template.coupon || null,
    };
    showTemplateDrawer.value = false;
  }
  function handleCustomerSelect(customers) {
    selectedCustomers.value = customers;
    showCustomerDrawer.value = false;
  }
  function handleShowDetail(msg) {
    selectedMessage.value = msg;
    showDetailModal.value = true;
  }
  function handleEditMessage(msg) {
    messageToEdit.value = null;
    nextTick(() => {
      messageToEdit.value = msg;
      showEditModal.value = true;
    });
  }
  function handleEditConfirm(updated) {
    const idx = messages.value.findIndex(m => m.id === updated.id);
    if (idx !== -1) messages.value[idx] = { ...messages.value[idx], ...updated };
    showEditModal.value = false;
    toast.value?.success('수정 완료');
  }
  function handlePageChange(page) {
    currentPage.value = page;
  }

  async function loadTemplatesForDrawer(page = 1) {
    try {
      const res = await TemplatesAPI.getTemplates({ page: page - 1, size: 10 });
      templateList.value = res.data;
    } catch (err) {
      console.error('🔥 템플릿 페이지 로딩 실패:', err);
      toast.value?.error('템플릿 불러오기 실패');
    }
  }

  watch([currentPage, statusFilter], loadMessages);

  onMounted(async () => {
    await Promise.allSettled([
      couponsAPI
        .getCoupons({ page: 0, size: 100 })
        .then(res => (availableCoupons.value = res.content)),
      customersAPI.getCustomersByShop().then(list => {
        allCustomers.value = list.map(c => ({
          id: c.customerId,
          name: c.customerName,
          phone: c.phoneNumber,
        }));
      }),
      GradesAPI.getGradesByShop().then(res => (customerGrades.value = res)),
      TagsAPI.getTagsByShop().then(res => (tags.value = res)),
    ]);
    await loadTemplatesForDrawer(1);
    await loadMessages();
    await nextTick();
    await loadAllMessagesForStats();
  });
</script>

<template>
  <div class="message-list-view">
    <div class="message-list-header">
      <h2 class="font-section-title text-dark">보낸 메시지 목록</h2>
    </div>

    <MessageStats :messages="allMessages" />

    <div class="message-filter-row with-button">
      <div class="filter-control">
        <label class="filter-label" for="message-status">메시지 상태</label>
        <select id="message-status" v-model="statusFilter" class="filter-select short">
          <option value="all">전체</option>
          <option value="sent">발송 완료</option>
          <option value="reserved">예약 문자</option>
        </select>
      </div>
      <BaseButton class="icon-button" type="primary" size="sm" @click="showSendModal = true">
        메시지 보내기
      </BaseButton>
    </div>

    <BaseCard>
      <template v-if="isLoading">
        <div class="text-center py-10">📡 메시지를 불러오는 중입니다...</div>
      </template>
      <BaseTable v-else :columns="columns" :data="messages">
        <template #body>
          <MessageItem
            v-for="msg in messages"
            :key="msg.id"
            :message="msg"
            @delete="handleDelete"
            @show-detail="handleShowDetail"
            @edit="handleEditMessage"
          />
        </template>
      </BaseTable>
    </BaseCard>

    <Pagination
      v-if="totalPages > 1"
      :current-page="currentPage"
      :total-pages="totalPages"
      :total-items="totalElements"
      :items-per-page="itemsPerPage"
      @page-change="handlePageChange"
    />

    <BaseModal
      :model-value="showDeleteConfirm"
      @update:model-value="val => (showDeleteConfirm = val)"
    >
      <template #title>메시지 삭제</template>
      <div class="modal-body">정말 삭제하시겠습니까?</div>
      <template #footer>
        <BaseButton type="primary" @click="cancelDelete">취소</BaseButton>
        <BaseButton type="error" @click="confirmDelete">삭제</BaseButton>
      </template>
    </BaseModal>

    <Teleport to="body">
      <TemplateSelectDrawer
        v-if="showTemplateDrawer"
        v-model="showTemplateDrawer"
        :templates="templateList.content"
        :pagination="templateList.pagination"
        @select="handleTemplateSelect"
        @page-change="loadTemplatesForDrawer"
      />
    </Teleport>

    <Teleport to="body">
      <CustomerSelectDrawer
        v-if="showCustomerDrawer"
        v-model="showCustomerDrawer"
        :customers="allCustomers"
        :page-size="20"
        @select="handleCustomerSelect"
      />
    </Teleport>

    <MessageSendModal
      :model-value="showSendModal"
      :template-content="messageToSend"
      :customers="selectedCustomers"
      :available-coupons="availableCoupons"
      :grades="customerGrades"
      :tags="tags"
      @update:model-value="val => (showSendModal = val)"
      @request-send="payload => handleSendRequest(payload, 'IMMEDIATE')"
      @request-reserve="payload => handleSendRequest(payload, 'RESERVATION')"
      @open-template="handleOpenTemplateDrawer"
      @open-customer="handleOpenCustomerDrawer"
    />

    <ReservationSendModal
      :model-value="showReserveModal"
      :message-content="messageToSend"
      :customers="selectedCustomers"
      @update:model-value="val => (showReserveModal = val)"
      @confirm="handleReserveConfirm"
    />

    <SendConfirmModal
      :model-value="showSendConfirm"
      :message-content="messageToSend"
      :customers="selectedCustomers"
      @update:model-value="val => (showSendConfirm = val)"
      @confirm="handleSendConfirm"
    />

    <MessageDetailModal
      :model-value="showDetailModal"
      :message="selectedMessage"
      @update:model-value="val => (showDetailModal = val)"
    />

    <EditReservationModal
      v-if="showEditModal && messageToEdit"
      :model-value="showEditModal"
      :message="messageToEdit"
      @update:model-value="val => (showEditModal = val)"
      @confirm="handleEditConfirm"
    />

    <BaseToast ref="toast" />
  </div>
</template>

<style scoped>
  .message-list-view {
    padding: 24px;
    max-width: 1200px;
    margin: 0 auto;
  }
  .message-list-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 1.5rem;
  }
  .message-filter-row.with-button {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 16px;
  }
  .filter-control {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .filter-label {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-gray-600);
    white-space: nowrap;
  }
  .filter-select {
    padding: 6px 10px;
    font-size: 14px;
    border: 1px solid var(--color-gray-300);
    border-radius: 6px;
    background-color: var(--color-neutral-white);
    max-width: 100px;
  }
</style>

<style>
  table thead th {
    text-align: center !important;
  }
</style>
