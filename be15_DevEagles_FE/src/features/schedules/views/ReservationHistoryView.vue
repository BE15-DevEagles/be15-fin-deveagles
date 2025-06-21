<template>
  <div class="history-wrapper">
    <div class="page-header">
      <h1 class="font-screen-title">예약 변경 이력</h1>
    </div>
    <div class="base-table-wrapper">
      <BaseTable
        :columns="columns"
        :data="pagedData"
        row-key="id"
        :striped="true"
        :hover="true"
        @row-click="openDetailModal"
      />
    </div>

    <Pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      :total-items="historyData.length"
      :items-per-page="itemsPerPage"
      @page-change="handlePageChange"
      @items-per-page-change="handleItemsPerPageChange"
    />

    <!-- 상세 모달 -->
    <ReservationDetailModal v-model="isModalOpen" :reservation="selectedReservation" />
  </div>
</template>

<script setup>
  import { ref, computed } from 'vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import Pagination from '@/components/common/Pagination.vue';
  import ReservationDetailModal from '@/features/schedules/components/ReservationDetailModal.vue';

  const columns = [
    { key: 'customer', title: '고객 이름', width: '120px' },
    { key: 'service', title: '시술', width: '100px' },
    { key: 'staff', title: '담당자', width: '100px' },
    { key: 'date', title: '예약 날짜', width: '160px' },
    { key: 'change', title: '변경 내용', width: '100px' },
    { key: 'changedAt', title: '변경 일시', width: '160px' },
  ];

  const historyData = [
    {
      id: 1,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '삭제',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 2,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '수정',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 3,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '수정',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 4,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '삭제',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 5,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '수정',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 6,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '수정',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 7,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '삭제',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 8,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '삭제',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 9,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '삭제',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 10,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '수정',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 11,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '수정',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 12,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '삭제',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 13,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '수정',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 14,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '수정',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 15,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '삭제',
      changedAt: '2025.06.08 11:08',
    },
    {
      id: 16,
      customer: '김미글',
      service: '염펌',
      staff: '박미글',
      date: '2025.06.08 14시',
      change: '삭제',
      changedAt: '2025.06.08 11:08',
    },
  ];

  const currentPage = ref(1);
  const itemsPerPage = ref(10);

  const totalPages = computed(() => Math.ceil(historyData.length / itemsPerPage.value));

  const pagedData = computed(() => {
    const start = (currentPage.value - 1) * itemsPerPage.value;
    return historyData.slice(start, start + itemsPerPage.value);
  });

  function handlePageChange(page) {
    currentPage.value = page;
  }

  function handleItemsPerPageChange(count) {
    itemsPerPage.value = count;
    currentPage.value = 1;
  }

  // 모달 관련
  const isModalOpen = ref(false);
  const selectedReservation = ref({});

  function openDetailModal(item) {
    selectedReservation.value = item;
    isModalOpen.value = true;
  }
</script>

<style scoped>
  .history-wrapper {
    padding: 24px;
  }

  .base-table-wrapper {
    background-color: var(--color-neutral-white);
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    padding: 24px;
    box-sizing: border-box;
  }

  .page-header {
    margin-bottom: 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
</style>
