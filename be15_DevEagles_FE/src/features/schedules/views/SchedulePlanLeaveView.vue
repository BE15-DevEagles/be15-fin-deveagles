<template>
  <div class="schedule-wrapper">
    <div class="page-header">
      <h1 class="font-screen-title">{{ currentTabTitle }}</h1>
    </div>

    <!-- 탭 메뉴 -->
    <div class="tab-bar">
      <BaseButton
        v-for="tab in tabs"
        :key="tab.value"
        type="primary"
        :outline="true"
        class="base-button"
        :class="{ active: tab.value === currentTab }"
        @click="currentTab = tab.value"
      >
        {{ tab.label }}
      </BaseButton>
    </div>

    <!-- 상단 버튼 + 필터 바 -->
    <div class="top-bar-flex">
      <div class="left-bar">
        <BaseButton
          type="error"
          :disabled="selectedIds.length === 0"
          @click="showConfirmModal = true"
        >
          {{ currentTab === 'plan' ? '일정 삭제' : '휴무 삭제' }}
        </BaseButton>
      </div>

      <div class="right-bar">
        <BaseForm
          v-model="selectedType"
          type="select"
          :options="typeOptions"
          style="width: 160px"
        />

        <BaseForm
          v-model="selectedStaff"
          type="select"
          :options="staffOptions"
          style="width: 160px"
        />
        <PrimeDatePicker
          v-model="selectedDateRange"
          selection-mode="range"
          :show-button-bar="true"
          :clearable="true"
          :show-icon="true"
          style="width: 280px"
          @hide="onDateRangeHide"
        />
        <BaseButton type="primary" class="fix-button-height" @click="isRegistModalOpen = true">
          {{ currentTab === 'plan' ? '일정 등록' : '휴무 등록' }}
        </BaseButton>

        <BaseConfirm
          v-model="showConfirmModal"
          title="삭제 확인"
          confirm-text="삭제"
          cancel-text="취소"
          confirm-type="error"
          icon-type="warning"
          :z-index="3000"
          @confirm="handleConfirmedDelete"
        >
          선택한 일정을 삭제하시겠습니까?<br />
          삭제된 일정은 복구할 수 없습니다.
        </BaseConfirm>
      </div>
    </div>

    <!-- 테이블 -->
    <div class="base-table-wrapper">
      <BaseTable
        :columns="columns"
        :data="pagedItems"
        :row-key="row => `${row.type}_${row.id}`"
        :striped="true"
        :hover="true"
        @row-click="handleRowClick"
      >
        <template #header-select>
          <input type="checkbox" :checked="allSelected" @change="toggleAll" />
        </template>
        <template #cell-select="{ item }">
          <input
            v-model="selectedIds"
            type="checkbox"
            :value="`${item.type}_${item.id}`"
            @click.stop
          />
        </template>

        <template v-if="currentTab === 'leave'" #cell-date="{ item, value }">
          <div class="text-left">
            <template v-if="item.type === 'regular_leave'">정기 휴무</template>
            <template v-else-if="value">{{ formatDate(value) }}</template>
            <template v-else>-</template>
          </div>
        </template>
        <template v-if="currentTab === 'plan'" #cell-start="{ item }">
          <div class="text-left">
            <template v-if="item.type === 'regular_plan'">
              {{ formatRegularPlan(item.repeatDescription, item.startTime, item.endTime) }}
            </template>
            <template v-else>
              {{ formatDateWithTime(item.start, item.end) }}
            </template>
          </div>
        </template>
      </BaseTable>
    </div>

    <BasePagination
      :current-page="currentPage"
      :total-pages="totalPages"
      :total-items="totalItems"
      :items-per-page="itemsPerPage"
      @page-change="handlePageChange"
    />

    <!-- 상세 모달 -->
    <component
      :is="modalComponent"
      v-if="modalComponent && isModalOpen"
      :id="selectedItem?.id"
      :model-value="isModalOpen"
      :type="selectedItem?.type"
      @update:model-value="closeModal"
      @closed="fetchList"
    />

    <!-- 등록 모달 -->
    <ScheduleRegistModal
      v-if="isRegistModalOpen"
      v-model="isRegistModalOpen"
      :default-tab="currentTab"
      @closed="fetchList"
    />
  </div>

  <BaseToast ref="toast" />
</template>

<script setup>
  import { ref, computed, onMounted, watch } from 'vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BasePagination from '@/components/common/Pagination.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import PlanDetailModal from '@/features/schedules/components/PlanDetailModal.vue';
  import LeaveDetailModal from '@/features/schedules/components/LeaveDetailModal.vue';
  import ScheduleRegistModal from '@/features/schedules/components/ScheduleRegistModal.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import BaseConfirm from '@/components/common/BaseConfirm.vue';
  import {
    fetchPlanList,
    fetchLeaveList,
    getStaffList,
    deletePlans,
    deleteLeaves,
    fetchScheduleDetail,
  } from '@/features/schedules/api/schedules.js';

  const tabs = [
    { label: '일정', value: 'plan' },
    { label: '휴무', value: 'leave' },
  ];
  const currentTab = ref('plan');
  const currentPage = ref(1);
  const totalPages = ref(1);
  const itemsPerPage = 10;
  const totalItems = ref(0);
  const selectedType = ref('');
  const selectedStaff = ref('');
  const selectedIds = ref([]);
  const selectedDateRange = ref([]);
  const selectedModalType = ref('');
  const isModalOpen = ref(false);
  const isRegistModalOpen = ref(false);
  const selectedItem = ref(null);
  const toast = ref(null);
  const showConfirmModal = ref(false);

  const planData = ref([]);
  const leaveData = ref([]);
  const staffList = ref([]);

  const modalComponent = computed(() => {
    if (!selectedModalType.value) return null;
    return selectedModalType.value === 'plan'
      ? PlanDetailModal
      : selectedModalType.value === 'leave'
        ? LeaveDetailModal
        : null;
  });

  const onDateRangeHide = () => {
    if (
      selectedDateRange.value?.length !== 2 ||
      !selectedDateRange.value[0] ||
      !selectedDateRange.value[1]
    ) {
      selectedDateRange.value = [];
    }
  };

  const handleConfirmedDelete = async () => {
    const deleteRequests = selectedIds.value
      .map(val => {
        const parts = val.split('_');
        const id = Number(parts.pop());
        const type = parts.join('_');
        return type && !isNaN(id) ? { id, type } : null;
      })
      .filter(Boolean);

    try {
      if (currentTab.value === 'plan') {
        await deletePlans(deleteRequests);
      } else {
        await deleteLeaves(deleteRequests);
      }

      selectedIds.value = [];

      currentPage.value = 1;
      await fetchList();

      toast.value?.success('삭제가 완료되었습니다.');
    } catch (error) {
      console.error('삭제 실패:', error);
      toast.value?.error('삭제 중 오류가 발생했습니다.');
    }
  };

  const handlePageChange = page => {
    currentPage.value = page;
    fetchList();
  };

  const loadStaffList = async () => {
    const list = await getStaffList();
    staffList.value = list;
  };

  const staffOptions = computed(() => [
    { text: '담당자', value: '' },
    ...staffList.value.map(staff => ({
      text: staff.staffName,
      value: staff.staffId,
    })),
  ]);

  const typeOptions = computed(() =>
    currentTab.value === 'plan'
      ? [
          { text: '일정 종류', value: '' },
          { text: '단기', value: 'plan' },
          { text: '정기', value: 'regular_plan' },
        ]
      : [
          { text: '휴무 종류', value: '' },
          { text: '단기', value: 'leave' },
          { text: '정기', value: 'regular_leave' },
        ]
  );

  const formatDateWithTime = (startStr, endStr) => {
    const start = new Date(startStr);
    const end = new Date(endStr);
    if (isNaN(start) || isNaN(end)) return '-';

    const formatFull = date => {
      const yyyy = date.getFullYear();
      const mm = String(date.getMonth() + 1).padStart(2, '0');
      const dd = String(date.getDate()).padStart(2, '0');
      const day = ['일', '월', '화', '수', '목', '금', '토'][date.getDay()];
      const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
      return `${yyyy}.${mm}.${dd} (${day}) ${time}`;
    };

    const formatTimeOnly = date => {
      return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    };

    const isSameDay = start.toDateString() === end.toDateString();

    return isSameDay
      ? `${formatFull(start)} ~ ${formatTimeOnly(end)}`
      : `${formatFull(start)} ~ ${formatFull(end)}`;
  };

  const formatRegularPlan = (repeat, startTime, endTime) => {
    const s = startTime?.slice(0, 5) ?? '';
    const e = endTime?.slice(0, 5) ?? '';
    return `${repeat} ${s} ~ ${e}`;
  };

  const toKoreanWeekday = rule => {
    if (!rule) return '';

    const weekMap = {
      MON: '월',
      TUE: '화',
      WED: '수',
      THU: '목',
      FRI: '금',
      SAT: '토',
      SUN: '일',
    };

    return rule.replace(/\b(MON|TUE|WED|THU|FRI|SAT|SUN)(요일)?\b/g, (_, eng) => {
      return weekMap[eng] || eng;
    });
  };

  const fromDate = computed(() => selectedDateRange.value?.[0]);
  const toDate = computed(() => selectedDateRange.value?.[1]);

  const fetchList = async () => {
    try {
      function formatDateToYMD(date) {
        const yyyy = date.getFullYear();
        const mm = String(date.getMonth() + 1).padStart(2, '0');
        const dd = String(date.getDate()).padStart(2, '0');
        return `${yyyy}-${mm}-${dd}`;
      }

      const from = fromDate.value ? formatDateToYMD(fromDate.value) : null;
      const to = toDate.value ? formatDateToYMD(toDate.value) : null;

      const params = {
        page: currentPage.value - 1,
        size: itemsPerPage,
        from,
        to,
        staffId: selectedStaff.value ? Number(selectedStaff.value) : null,
        ...(currentTab.value === 'plan'
          ? selectedType.value
            ? { planType: selectedType.value }
            : {}
          : selectedType.value
            ? { leaveType: selectedType.value }
            : {}),
      };

      if (currentTab.value === 'plan') {
        const result = await fetchPlanList(params);
        const content = result.content ?? [];
        totalItems.value = result.pagination?.totalItems ?? 0;
        leaveData.value = [];
        planData.value = content.map(item => {
          const isRegular = item.planType === 'regular_plan';

          return {
            id: item.id,
            staffId: item.staffId,
            staff: item.staffName,
            staffName: item.staffName,
            title: item.planTitle,
            type: isRegular ? 'regular_plan' : 'plan',
            period: isRegular ? '정기' : '단기',
            date: isRegular ? null : item.startAt?.split('T')[0],
            start: item.startAt,
            end: item.endAt,
            startTime: item.startTime,
            endTime: item.endTime,
            repeatDescription: toKoreanWeekday(item.repeatRule || '반복 안함'),
          };
        });

        totalPages.value = result.pagination?.totalPages ?? 1;
        currentPage.value = (result.pagination?.currentPage ?? 0) + 1;
      } else {
        planData.value = [];
        const result = await fetchLeaveList(params);
        const content = result.content ?? [];
        totalItems.value = result.pagination?.totalItems ?? 0;

        leaveData.value = content.map(item => ({
          id: item.id,
          staffId: item.staffId,
          staff: item.staffName,
          title: item.leaveTitle,
          type: item.leaveType,
          typeLabel: item.leaveType === 'regular_leave' ? '정기' : '단기',
          date: item.leaveDate ?? '반복 휴무',
          repeatDescription: toKoreanWeekday(item.repeatRule || '반복 안함'),
        }));

        totalPages.value = result.pagination?.totalPages ?? 1;
        currentPage.value = (result.pagination?.currentPage ?? 0) + 1;
      }
    } catch (error) {
      console.error('일정/휴무 목록 조회 실패:', error);
    }
  };

  onMounted(async () => {
    const today = new Date();
    const sevenDaysLater = new Date();
    sevenDaysLater.setDate(today.getDate() + 6);

    selectedDateRange.value = [today, sevenDaysLater];

    await loadStaffList();
    await fetchList();
  });

  watch(
    [
      currentTab,
      selectedType,
      selectedStaff,
      () => selectedDateRange.value?.[0],
      () => selectedDateRange.value?.[1],
    ],
    () => {
      currentPage.value = 1;
      fetchList();
    }
  );

  watch(
    () => currentTab.value,
    () => {
      selectedType.value = '';
      selectedStaff.value = '';
      selectedDateRange.value = [];
      selectedIds.value = [];
      currentPage.value = 1;

      const today = new Date();
      const sevenDaysLater = new Date();
      sevenDaysLater.setDate(today.getDate() + 6);
      selectedDateRange.value = [today, sevenDaysLater];

      fetchList();
    }
  );

  const pagedItems = computed(() =>
    currentTab.value === 'plan' ? planData.value : leaveData.value
  );

  const allSelected = computed(
    () =>
      pagedItems.value.length > 0 &&
      pagedItems.value.every(item => selectedIds.value.includes(`${item.type}_${item.id}`))
  );

  const toggleAll = e => {
    if (e.target.checked) {
      selectedIds.value = pagedItems.value.map(item => `${item.type}_${item.id}`);
    } else {
      selectedIds.value = [];
    }
  };

  const handleRowClick = async (row, event) => {
    if (!row || typeof row !== 'object') {
      return;
    }
    if (event?.target?.type === 'checkbox') return;

    const { type, id } = row;

    try {
      const { data } = await fetchScheduleDetail(type, id);
      const resData = data.data;

      if (type === 'regular_plan') {
        let repeat = '';
        let date = '';

        if (resData.weeklyPlan) {
          repeat = 'weekly';
          date = resData.weeklyPlan;
        } else if (resData.monthlyPlan) {
          repeat = 'monthly';
          date = String(resData.monthlyPlan);
        }

        selectedItem.value = {
          id,
          type,
          title: resData.title ?? '',
          staff: resData.staffName ?? '',
          memo: resData.memo ?? '',
          repeat,
          date,
          startTime: resData.startTime ?? '',
          endTime: resData.endTime ?? '',
        };
      } else if (type === 'plan') {
        selectedItem.value = {
          id,
          type,
          title: resData.title ?? '',
          staff: resData.staffName ?? '',
          memo: resData.memo ?? '',
          start: resData.startAt ?? '',
          end: resData.endAt ?? '',
          startTime: '',
          endTime: '',
        };
      } else if (type === 'leave') {
        selectedItem.value = {
          id,
          type,
          title: resData.leaveTitle ?? '',
          staff: resData.staffName ?? '',
          memo: resData.memo ?? '',
          start: resData.leaveDate ?? '',
        };
      } else if (type === 'regular_leave') {
        selectedItem.value = {
          id,
          type,
          title: resData.leaveTitle ?? '',
          staff: resData.staffName ?? '',
          memo: resData.memo ?? '',
          start: resData.repeatRule ?? '',
        };
      }

      selectedModalType.value = type.includes('plan') ? 'plan' : 'leave';
      isModalOpen.value = true;
    } catch (e) {
      console.error('상세 조회 실패:', e);
      toast.value?.error('상세 정보를 불러오는 데 실패했습니다.');
    }
  };

  const closeModal = () => {
    isModalOpen.value = false;
    selectedItem.value = null;
  };

  const formatDate = dateStr => {
    const date = new Date(dateStr);
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    const day = ['일', '월', '화', '수', '목', '금', '토'][date.getDay()];
    return `${yyyy}.${mm}.${dd} (${day})`;
  };

  const currentTabTitle = computed(() => (currentTab.value === 'plan' ? '일정 목록' : '휴무 목록'));

  const columns = computed(() =>
    currentTab.value === 'plan'
      ? [
          { key: 'select', title: '', width: '40px' },
          { key: 'staff', title: '직원 이름', width: '120px' },
          { key: 'title', title: '일정 제목', width: '160px' },
          { key: 'period', title: '정기/단기', width: '120px' },
          { key: 'repeatDescription', title: '반복 설정', width: '180px' },
          { key: 'start', title: '날짜', width: '240px' },
        ]
      : [
          { key: 'select', title: '', width: '40px' },
          { key: 'staff', title: '직원 이름', width: '120px' },
          { key: 'title', title: '휴무 제목', width: '160px' },
          { key: 'typeLabel', title: '정기/단기', width: '120px' },
          { key: 'repeatDescription', title: '반복 설정', width: '180px' },
          { key: 'date', title: '날짜', width: '180px' },
        ]
  );
</script>

<style scoped>
  .btn-outline.btn-primary {
    background: transparent;
    border: 1px solid var(--color-primary-main);
    color: var(--color-primary-main);
  }

  .btn-outline.btn-primary:hover {
    background: var(--color-primary-50);
  }

  .btn-outline.btn-primary:active {
    background: var(--color-primary-100);
  }

  .right-bar > * {
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
  .schedule-wrapper {
    padding: 24px;
  }

  .page-header {
    margin-bottom: 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .top-bar-flex {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 16px;
  }

  .left-bar {
    flex-shrink: 0;
  }

  .tab-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
  }

  .right-bar {
    display: flex;
    justify-content: flex-end;
    gap: 16px;
    flex-wrap: wrap;
  }

  .base-table-wrapper {
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    padding: 24px;
  }

  .fix-button-height {
    height: 40px;
    padding: 0 16px !important;
    font-size: 14px;
    line-height: 1 !important;
  }

  .tab-bar :deep(.base-button.active) {
    background-color: var(--color-primary-main);
    color: white;
    border-color: var(--color-primary-main);
  }
</style>
