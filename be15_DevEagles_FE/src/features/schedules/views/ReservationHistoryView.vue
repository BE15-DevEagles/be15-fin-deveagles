<template>
  <div class="history-wrapper">
    <div class="page-header">
      <h1 class="font-screen-title">예약 변경 이력</h1>
    </div>
    <div class="base-table-wrapper">
      <BaseTable
        :columns="columns"
        :data="historyData"
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
    <ReservationDetailModal :id="selectedReservation" v-model="isModalOpen" :readonly="true" />
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, watch } from 'vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import Pagination from '@/components/common/Pagination.vue';
  import ReservationDetailModal from '@/features/schedules/components/ReservationDetailModal.vue';
  import { fetchReservationHistories } from '@/features/schedules/api/schedules.js';
  import dayjs from 'dayjs';

  const columns = [
    { key: 'customerName', title: '고객 이름', width: '120px' },
    { key: 'itemNames', title: '시술', width: '100px' },
    { key: 'staffName', title: '담당자', width: '100px' },
    { key: 'reservationStartAt', title: '예약 날짜', width: '160px' },
    { key: 'historyType', title: '변경 내용', width: '100px' },
    { key: 'historyAt', title: '변경 일시', width: '160px' },
  ];

  const historyData = ref([]);
  const currentPage = ref(1);
  const itemsPerPage = ref(10);
  const totalItems = ref(0);

  const totalPages = computed(() => Math.ceil(totalItems.value / itemsPerPage.value));

  const fetchData = async () => {
    try {
      const res = await fetchReservationHistories({
        page: currentPage.value - 1,
        size: itemsPerPage.value,
      });
      historyData.value = res.content.map(item => ({
        ...item,
        id: item.reservationId,
        customerName: item.customerName ?? '미등록 고객',
        reservationStartAt: item.reservationStartAt
          ? dayjs(item.reservationStartAt).format('YYYY-MM-DD HH:mm:ss')
          : '',
        historyAt: item.historyAt ? dayjs(item.historyAt).format('YYYY-MM-DD HH:mm:ss') : '',
      }));

      totalItems.value = res.pagination.totalItems;
    } catch (err) {
      console.error('예약 변경 이력 조회 실패:', err);
    }
  };

  function handlePageChange(page) {
    currentPage.value = page;
  }

  function handleItemsPerPageChange(count) {
    itemsPerPage.value = count;
    currentPage.value = 1;
  }

  watch([currentPage, itemsPerPage], fetchData);
  onMounted(fetchData);

  const isModalOpen = ref(false);
  const selectedReservation = ref(null);

  function openDetailModal(item) {
    if (!item || !item.id) {
      return;
    }
    selectedReservation.value = item.id;
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
