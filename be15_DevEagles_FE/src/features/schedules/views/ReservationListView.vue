<template>
  <div style="padding: 24px">
    <div class="page-header">
      <h1 class="font-screen-title">예약 목록</h1>
    </div>

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
          :options="[
            { text: '담당자', value: '' },
            { text: '박미글', value: '박미글' },
            { text: '이팀장', value: '이팀장' },
          ]"
          style="width: 160px"
        />
        <BaseForm
          v-model="selectedService"
          type="select"
          :options="[
            { text: '시술 종류', value: '' },
            { text: '커트', value: '커트' },
            { text: '염색', value: '염색' },
            { text: '펌', value: '펌' },
          ]"
          style="width: 160px"
        />
        <BaseForm
          v-model="selectedStatus"
          type="select"
          :options="[
            { text: '예약 상태', value: '' },
            { text: '예약 대기', value: '예약 대기' },
            { text: '예약 확정', value: '예약 확정' },
            { text: '노쇼', value: '노쇼' },
            { text: '고객에 의한 예약 취소', value: '고객에 의한 예약 취소' },
            { text: '가게에 의한 예약 취소', value: '가게에 의한 예약 취소' },
          ]"
          style="width: 160px"
        />
      </div>
      <BaseButton type="primary" @click="openReservationModal">예약 등록</BaseButton>
    </div>

    <div class="base-table-wrapper">
      <BaseTable
        :columns="columns"
        :data="filteredReservations"
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
              'badge-success': value === '예약 확정',
              'badge-warning': value === '예약 대기',
              'badge-error': value.includes('취소') || value === '노쇼',
            }"
          >
            {{ value }}
          </span>
        </template>

        <template #cell-prepaidUsed="{ value }">
          <div class="text-center">{{ value ? '○' : '×' }}</div>
        </template>

        <template #cell-actions="{ item }">
          <div class="action-buttons">
            <BaseButton
              v-if="item.status === '예약 대기'"
              outline
              type="primary"
              size="sm"
              @click="
                e => {
                  e.stopPropagation();
                  confirmWithoutModal(item);
                }
              "
            >
              예약 확정
            </BaseButton>

            <BaseButton
              v-if="item.status === '예약 대기' || item.status === '예약 확정'"
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

    <ReservationDetailModal
      v-if="isDetailOpen"
      v-model="isDetailOpen"
      :reservation="selectedReservation"
      @cancel-reservation="handleCancelFromDetail"
    />
    <Pagination
      :current-page="1"
      :total-pages="3"
      :total-items="30"
      :items-per-page="10"
      @page-change="page => {}"
      @items-per-page-change="count => {}"
    />

    <BaseModal v-model="isModalOpen" :title="modalTitle">
      <template v-if="modalType === 'confirm'">
        <p style="text-align: center; font-size: 16px">해당 예약을 확정하시겠습니까?</p>
      </template>
      <template v-else>
        <p style="text-align: center; font-size: 16px">예약을 어떤 사유로 취소하시겠습니까?</p>
      </template>

      <template #footer>
        <div style="display: flex; gap: 12px; justify-content: flex-end; flex-wrap: wrap">
          <BaseButton v-if="modalType === 'confirm'" type="primary" @click="onConfirm"
            >예</BaseButton
          >
          <template v-else>
            <BaseButton type="error" @click="confirmCancel('가게에 의한 예약 취소')">
              가게에 의한 예약 취소
            </BaseButton>
            <BaseButton type="error" @click="confirmCancel('고객에 의한 예약 취소')">
              고객에 의한 예약 취소
            </BaseButton>
          </template>
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
  import { ref, computed } from 'vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import Pagination from '@/components/common/Pagination.vue';
  import ScheduleRegistModal from '@/features/schedules/components/ScheduleRegistModal.vue';
  import ReservationDetailModal from '@/features/schedules/components/ReservationDetailModal.vue';
  import BaseForm from '@/components/common/BaseForm.vue';

  const searchText = ref('');
  const selectedDate = ref('');
  const selectedStaff = ref('');
  const selectedService = ref('');
  const selectedStatus = ref('');
  const isModalOpen = ref(false);
  const isRegistModalOpen = ref(false);
  const modalType = ref('confirm');
  const modalTitle = ref('');
  const toast = ref(null);
  let selectedReservation = null;

  const reservations = ref([
    {
      id: 1,
      customer: '김미글',
      service: '염색',
      staff: '박미글',
      phone: '010-2222-2221',
      date: '2025-06-08T14:00:00',
      status: '예약 대기',
      prepaidUsed: true,
      duration: '03:00',
    },
    {
      id: 2,
      customer: '이예정',
      service: '커트',
      staff: '이팀장',
      phone: '010-2222-2222',
      date: '2025-06-09T11:00:00',
      status: '예약 확정',
      prepaidUsed: false,
      duration: '03:00',
    },
    {
      id: 3,
      customer: '장현수',
      service: '펌',
      staff: '박미글',
      phone: '010-2222-2223',
      date: '2025-06-10T15:00:00',
      status: '노쇼',
      prepaidUsed: false,
      duration: '03:00',
    },
  ]);

  const columns = [
    { key: 'customer', title: '고객 이름', width: '120px' },
    { key: 'service', title: '시술', width: '100px' },
    { key: 'staff', title: '담당자', width: '100px' },
    { key: 'date', title: '예약 날짜', width: '160px' },
    { key: 'status', title: '예약 상태', width: '140px' },
    { key: 'prepaidUsed', title: '선불권 사용 여부', width: '140px' },
    { key: 'actions', title: '예약 상태 변경', width: '200px' },
  ];

  function openModal(item, type) {
    selectedReservation = item;
    modalType.value = type;
    modalTitle.value = type === 'confirm' ? '예약 확정' : '예약 취소 사유 선택';
    isModalOpen.value = true;
  }

  function confirmWithoutModal(item) {
    item.status = '예약 확정';
    toast.value.success('예약이 확정되었습니다.');
  }

  function confirmCancel(reason) {
    if (!selectedReservation) return;
    selectedReservation.status = reason;
    toast.value.success(`예약이 취소되었습니다.`);
    isModalOpen.value = false;
  }

  const filteredReservations = computed(() => {
    const now = new Date();

    return reservations.value.filter(r => {
      const matchText =
        !searchText.value ||
        r.customer.includes(searchText.value) ||
        (r.phone && r.phone.includes(searchText.value));

      const matchStaff = !selectedStaff.value || r.staff.includes(selectedStaff.value);
      const matchService = !selectedService.value || r.service.includes(selectedService.value);
      const matchStatus = !selectedStatus.value || r.status === selectedStatus.value;

      const reservationDate = new Date(r.date);
      let matchDate = true;

      if (selectedDate.value === 'today') {
        const today = now.toISOString().split('T')[0];
        const resDate = reservationDate.toISOString().split('T')[0];
        matchDate = today === resDate;
      } else if (selectedDate.value === 'thisWeek') {
        const startOfWeek = new Date(now);
        startOfWeek.setDate(now.getDate() - now.getDay());
        startOfWeek.setHours(0, 0, 0, 0);

        const endOfWeek = new Date(startOfWeek);
        endOfWeek.setDate(startOfWeek.getDate() + 6);
        endOfWeek.setHours(23, 59, 59, 999);

        matchDate = reservationDate >= startOfWeek && reservationDate <= endOfWeek;
      } else if (selectedDate.value === 'thisMonth') {
        matchDate =
          reservationDate.getFullYear() === now.getFullYear() &&
          reservationDate.getMonth() === now.getMonth();
      }

      return matchText && matchStaff && matchService && matchStatus && matchDate;
    });
  });

  const isDetailOpen = ref(false);

  function openDetail(item) {
    selectedReservation = item;
    isDetailOpen.value = true;
  }

  function handleCancelFromDetail(reservation) {
    reservation.status = '고객에 의한 예약 취소';
    toast.value.success('예약이 취소되었습니다.');
    isDetailOpen.value = false;
  }

  function onConfirm() {
    if (!selectedReservation) return;
    selectedReservation.status = '예약 확정';
    toast.value.success('예약이 확정되었습니다.');
    isModalOpen.value = false;
  }

  function onCancel() {
    isModalOpen.value = false;
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

  .filter-bar {
    display: flex;
    justify-content: flex-end;
    align-items: flex-start;
    gap: 16px;
    flex-wrap: wrap;
    margin-bottom: 24px;
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
