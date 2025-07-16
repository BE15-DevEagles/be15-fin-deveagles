<template>
  <div style="padding: 24px">
    <div class="page-header">
      <h1 class="font-screen-title">예약 목록</h1>
    </div>

    <!-- 필터 바 -->
    <div class="filter-bar">
      <div class="filter-fields">
        <BaseForm
          v-model="searchText"
          type="text"
          placeholder="고객명 또는 연락처 검색"
          style="width: 200px"
        />
        <BaseForm
          v-model="selectedDate"
          type="select"
          :options="[
            { text: '날짜', value: '' },
            { text: '오늘', value: 'today' },
            { text: '이번 주', value: 'thisWeek' },
            { text: '이번 달', value: 'thisMonth' },
          ]"
          style="width: 160px"
        />
        <BaseForm
          v-model="selectedStaff"
          type="select"
          :options="staffOptions"
          style="width: 160px"
        />
        <BaseForm
          v-model="selectedStatus"
          type="select"
          :options="[
            { text: '예약 상태', value: '' },
            { text: '예약 대기', value: 'PENDING' },
            { text: '예약 확정', value: 'CONFIRMED' },
            { text: '노쇼', value: 'NO_SHOW' },
            { text: '고객에 의한 예약 취소', value: 'CBC' },
            { text: '가게에 의한 예약 취소', value: 'CBS' },
          ]"
          style="width: 160px"
        />
      </div>
      <BaseButton type="primary" @click="openReservationModal">예약 등록</BaseButton>
    </div>

    <!-- 예약 테이블 -->
    <div class="base-table-wrapper">
      <BaseTable
        :columns="columns"
        :data="reservations"
        :striped="true"
        :hover="true"
        row-key="id"
        @row-click="openDetail"
      >
        <template #cell-date="{ value }">
          <div class="text-center">{{ formatDate(value) }}</div>
        </template>

        <template #cell-status="{ value }">
          <span
            class="badge"
            :class="{
              'badge-success': value === 'CONFIRMED',
              'badge-warning': value === 'PENDING',
              'badge-error': ['NO_SHOW', 'CBC', 'CBS'].includes(value),
            }"
          >
            {{
              value === 'CONFIRMED'
                ? '예약 확정'
                : value === 'PENDING'
                  ? '예약 대기'
                  : value === 'NO_SHOW'
                    ? '노쇼'
                    : value === 'CBC'
                      ? '고객에 의한 예약 취소'
                      : value === 'CBS'
                        ? '가게에 의한 예약 취소'
                        : value
            }}
          </span>
        </template>

        <template #cell-actions="{ item }">
          <div class="action-buttons">
            <BaseButton
              v-if="item.status === 'PENDING'"
              outline
              type="primary"
              size="sm"
              @click="
                e => {
                  e.stopPropagation();
                  openModal(item, 'confirm');
                }
              "
            >
              예약 확정
            </BaseButton>
            <BaseButton
              v-if="['PENDING', 'CONFIRMED'].includes(item.status)"
              outline
              type="error"
              size="sm"
              @click="
                e => {
                  e.stopPropagation();
                  openModal(item, 'cancel');
                }
              "
            >
              예약 취소
            </BaseButton>
          </div>
        </template>
      </BaseTable>
    </div>

    <!-- 상세 모달 -->
    <ReservationDetailModal
      v-if="isDetailOpen"
      :id="selectedReservation"
      v-model="isDetailOpen"
      @cancel-reservation="handleCancelFromDetail"
    />

    <!-- 페이지네이션 -->
    <Pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      :total-items="totalItems"
      :items-per-page="itemsPerPage"
      @page-change="page => (currentPage = page)"
    />

    <!-- 예약 확정 확인 모달 -->
    <BaseConfirm
      v-if="modalType === 'confirm'"
      v-model="isModalOpen"
      title="예약 확정"
      message="해당 예약을 확정하시겠습니까?"
      confirm-text="예"
      cancel-text="아니오"
      confirm-type="primary"
      @confirm="onConfirm"
      @cancel="onCancel"
    />

    <!-- 예약 취소 사유 선택 모달 -->
    <BaseModal v-else v-model="isModalOpen" :title="modalTitle">
      <p style="text-align: center; font-size: 16px">예약을 어떤 사유로 취소하시겠습니까?</p>

      <template #footer>
        <div style="display: flex; gap: 12px; justify-content: flex-end; flex-wrap: wrap">
          <BaseButton type="error" @click="confirmCancel('가게에 의한 예약 취소')">
            가게에 의한 예약 취소
          </BaseButton>
          <BaseButton type="error" @click="confirmCancel('고객에 의한 예약 취소')">
            고객에 의한 예약 취소
          </BaseButton>
          <BaseButton outline @click="onCancel">닫기</BaseButton>
        </div>
      </template>
    </BaseModal>

    <BaseToast ref="toast" />

    <!-- 예약 등록 모달 -->
    <ScheduleRegistModal
      v-if="isRegistModalOpen"
      v-model="isRegistModalOpen"
      :default-tab="'reservation'"
    />
  </div>
</template>

<script setup>
  import { ref, watch, onMounted } from 'vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import Pagination from '@/components/common/Pagination.vue';
  import ScheduleRegistModal from '@/features/schedules/components/ScheduleRegistModal.vue';
  import ReservationDetailModal from '@/features/schedules/components/ReservationDetailModal.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseConfirm from '@/components/common/BaseConfirm.vue';
  import dayjs from 'dayjs';

  import {
    fetchReservationList,
    getStaffList,
    updateReservationStatuses,
  } from '@/features/schedules/api/schedules';

  const isDetailOpen = ref(false);
  const isConfirmDialogOpen = ref(false);
  const confirmTarget = ref(null);
  const searchText = ref('');
  const selectedDate = ref('thisWeek');
  const selectedStatus = ref('');
  const isModalOpen = ref(false);
  const isRegistModalOpen = ref(false);
  const modalType = ref('confirm');
  const modalTitle = ref('');
  const toast = ref(null);
  const selectedReservation = ref(null);
  const reservations = ref([]);
  const totalItems = ref(0);
  const currentPage = ref(1);
  const itemsPerPage = ref(10);
  const totalPages = ref(1);
  const staffOptions = ref([{ text: '담당자', value: '' }]);
  const selectedStaff = ref('');

  const columns = [
    { key: 'customer', title: '고객 이름', width: '120px' },
    { key: 'service', title: '시술', width: '100px' },
    { key: 'staff', title: '담당자', width: '100px' },
    { key: 'date', title: '예약 날짜', width: '160px' },
    { key: 'status', title: '예약 상태', width: '140px' },
    { key: 'actions', title: '예약 상태 변경', width: '200px' },
  ];

  async function confirmReservation() {
    const target = reservations.value.find(r => r.id === selectedReservation.value);
    if (!target) return;

    try {
      await updateReservationStatuses([
        {
          reservationId: target.id,
          reservationStatusName: 'CONFIRMED',
        },
      ]);
      target.status = 'CONFIRMED';
      toast.value.success('예약이 확정되었습니다.');
    } catch (e) {
      toast.value.error('예약 확정에 실패했습니다.');
    } finally {
      isModalOpen.value = false;
      selectedReservation.value = null;
    }
  }

  async function confirmCancel(reason) {
    const status = reason === '고객에 의한 예약 취소' ? 'CBC' : 'CBS';
    const target = reservations.value.find(r => r.id === selectedReservation.value);
    if (!target) return;

    try {
      await updateReservationStatuses([
        {
          reservationId: target.id,
          reservationStatusName: status,
        },
      ]);
      target.status = status;
      toast.value.success('예약이 취소되었습니다.');
    } catch (e) {
      toast.value.error('예약 취소에 실패했습니다.');
    } finally {
      isModalOpen.value = false;
      selectedReservation.value = null;
    }
  }

  function getDateRangeByType(type) {
    const now = dayjs(); // 현재 로컬 시간 (KST)

    if (type === 'today') {
      const today = now.format('YYYY-MM-DD');
      return { from: today, to: today };
    } else if (type === 'thisWeek') {
      const startOfWeek = now.startOf('week');
      const endOfWeek = now.endOf('week');
      return {
        from: startOfWeek.format('YYYY-MM-DD'),
        to: endOfWeek.format('YYYY-MM-DD'),
      };
    } else if (type === 'thisMonth') {
      const startOfMonth = now.startOf('month');
      const endOfMonth = now.endOf('month');
      return {
        from: startOfMonth.format('YYYY-MM-DD'),
        to: endOfMonth.format('YYYY-MM-DD'),
      };
    }
    return { from: null, to: null };
  }

  function getDuration(start, end) {
    const startTime = new Date(start);
    const endTime = new Date(end);
    const diffMs = endTime - startTime;
    const diffMinutes = Math.floor(diffMs / 1000 / 60);
    const hours = Math.floor(diffMinutes / 60);
    const minutes = diffMinutes % 60;

    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
  }

  const fetchReservations = async () => {
    const { from, to } = getDateRangeByType(selectedDate.value);

    const res = await fetchReservationList({
      staffId: selectedStaff.value || null,
      reservationStatusName: selectedStatus.value || null,
      customerKeyword: searchText.value || null,
      from,
      to,
      page: currentPage.value - 1,
      size: itemsPerPage.value,
    });

    reservations.value = res.content.map(item => ({
      id: item.reservationId,
      customer: item.customerName ?? '미등록 고객',
      phone: item.customerPhone,
      service: item.itemNames,
      staff: item.staffName,
      date: item.reservationStartAt,
      status: item.reservationStatusName,
      duration:
        item.reservationEndAt && item.reservationStartAt
          ? getDuration(item.reservationStartAt, item.reservationEndAt)
          : null,
    }));

    totalItems.value = res.pagination.totalItems;
    totalPages.value = res.pagination.totalPages;
  };

  onMounted(async () => {
    await fetchStaffOptions();
    await fetchReservations();
  });

  const fetchStaffOptions = async () => {
    try {
      const res = await getStaffList({ isActive: true });
      staffOptions.value.push(
        ...res.map(staff => ({
          text: staff.staffName,
          value: staff.staffId,
        }))
      );
    } catch (e) {
      console.error('담당자 목록 조회 실패:', e);
    }
  };
  watch(isDetailOpen, val => {
    if (!val) {
      fetchReservations();
    }
  });
  watch(isRegistModalOpen, val => {
    if (!val) {
      fetchReservations();
    }
  });
  watch(currentPage, () => {
    fetchReservations();
  });

  watch([itemsPerPage, searchText, selectedStaff, selectedDate, selectedStatus], () => {
    currentPage.value = 1;
    fetchReservations();
  });

  watch(itemsPerPage, () => {
    currentPage.value = 1;
    fetchReservations();
  });
  watch(
    [
      () => searchText.value,
      () => selectedStaff.value,
      () => selectedDate.value,
      () => selectedStatus.value,
    ],
    () => {
      currentPage.value = 1;
      fetchReservations();
    }
  );
  function openModal(item, type) {
    selectedReservation.value = item.id;
    modalType.value = type;
    modalTitle.value = type === 'confirm' ? '예약 확정' : '예약 취소 사유 선택';
    isModalOpen.value = true;
  }

  function openDetail(item) {
    selectedReservation.value = item.id;
    console.log('🔍 선택된 예약 ID:', selectedReservation.value);
    isDetailOpen.value = true;
  }

  function handleCancelFromDetail(reservation) {
    reservation.status = '고객에 의한 예약 취소';
    toast.value.success('예약이 취소되었습니다.');
    isDetailOpen.value = false;
  }

  function onConfirm() {
    if (modalType.value === 'confirm') {
      confirmReservation();
    }
  }

  function onCancel() {
    isModalOpen.value = false;
    selectedReservation.value = null;
  }

  function openReservationModal() {
    isRegistModalOpen.value = true;
  }

  function formatDate(dateStr) {
    const d = new Date(dateStr);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    const hh = String(d.getHours()).padStart(2, '0');
    return `${yyyy}.${mm}.${dd} ${hh}시`;
  }
</script>

<style scoped>
  .page-header {
    margin-bottom: 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .filter-fields > * {
    min-width: 160px;
    flex-shrink: 0;
    box-sizing: border-box;
  }

  .filter-bar {
    display: flex;
    justify-content: flex-end;
    align-items: flex-start;
    gap: 16px;
    flex-wrap: wrap;
    margin-bottom: 24px;
    width: 100%;
  }

  .filter-fields {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
  }

  .input-search {
    width: 200px;
    padding: 8px;
    border-radius: 6px;
    border: 1px solid var(--color-gray-300);
    background-color: var(--color-neutral-white);
    color: var(--color-text-primary);
  }

  .input-select {
    width: 160px;
    padding: 8px;
    border-radius: 6px;
    border: 1px solid var(--color-gray-300);
    background-color: var(--color-neutral-white);
    color: var(--color-text-primary);
  }

  .badge {
    display: inline-block;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
    white-space: nowrap;
    line-height: 1.2;
  }

  /* 상태별 뱃지 */
  .badge-success {
    background-color: var(--color-success-50);
    color: var(--color-success-600);
  }

  .badge-warning {
    background-color: var(--color-warning-50);
    color: var(--color-warning-400);
  }

  .badge-error {
    background-color: var(--color-error-100);
    color: var(--color-error-300);
  }

  .base-table-wrapper {
    background-color: var(--color-neutral-white);
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    padding: 24px;
    box-sizing: border-box;
  }

  .action-buttons {
    display: flex;
    gap: 8px;
    flex-wrap: nowrap;
    align-items: center;
  }

  .base-table-wrapper :deep(tbody tr) {
    cursor: pointer;
    transition: background-color 0.2s ease;
  }

  .base-table-wrapper :deep(tbody tr:hover) {
    background-color: var(--color-gray-50);
  }
</style>
