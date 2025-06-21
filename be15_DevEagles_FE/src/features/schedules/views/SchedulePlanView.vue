<template>
  <div class="schedule-wrapper">
    <div class="page-header">
      <h1 class="font-screen-title">일정 목록</h1>
    </div>

    <!-- 상단 버튼 + 필터 바 -->
    <div class="top-bar-flex">
      <div class="left-bar">
        <BaseButton type="error" :disabled="selectedIds.length === 0" @click="deleteSelected">
          일정 삭제
        </BaseButton>
      </div>

      <div class="right-bar">
        <BaseForm
          v-model="selectedType"
          type="select"
          :options="[
            { text: '전체', value: '' },
            { text: '단기', value: 'plan' },
            { text: '정기', value: 'regular_plan' },
          ]"
          placeholder="일정 종류"
          style="width: 160px"
        />
        <BaseForm
          v-model="selectedStaff"
          type="select"
          :options="[
            { text: '전체', value: '' },
            { text: '김이글', value: '김이글' },
            { text: '박이글', value: '박이글' },
          ]"
          placeholder="직원 이름"
          style="width: 160px"
        />
        <BaseButton type="primary" class="fix-button-height" @click="isRegistModalOpen = true">
          일정 등록
        </BaseButton>
      </div>
    </div>

    <!-- 테이블 -->
    <div class="base-table-wrapper">
      <BaseTable
        :columns="columns"
        :data="pagedSchedules"
        row-key="id"
        :striped="true"
        :hover="true"
        @row-click="handleRowClick"
      >
        <template #header-select>
          <input type="checkbox" :checked="allSelected" @change="toggleAll" />
        </template>

        <template #cell-select="{ item }">
          <input v-model="selectedIds" type="checkbox" :value="item.id" @click.stop />
        </template>
      </BaseTable>
    </div>

    <BasePagination
      :current-page="currentPage"
      :total-pages="totalPages"
      :total-items="filteredSchedules.length"
      :items-per-page="itemsPerPage"
      @page-change="handlePageChange"
    />

    <!-- 일정 상세 모달 -->
    <PlanDetailModal
      v-if="isModalOpen"
      :model-value="isModalOpen"
      :reservation="selectedSchedule"
      @update:model-value="closeModal"
    />

    <!-- 일정 등록 모달 -->
    <ScheduleRegistModal
      v-if="isRegistModalOpen"
      :model-value="isRegistModalOpen"
      :default-tab="'plan'"
      @update:model-value="isRegistModalOpen = false"
    />
  </div>
</template>

<script setup>
  import { ref, computed } from 'vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BasePagination from '@/components/common/Pagination.vue';
  import PlanDetailModal from '@/features/schedules/components/PlanDetailModal.vue';
  import ScheduleRegistModal from '@/features/schedules/components/ScheduleRegistModal.vue'; // 등록 모달

  const selectedType = ref('');
  const selectedStaff = ref('');
  const selectedIds = ref([]);

  const currentPage = ref(1);
  const itemsPerPage = 10;

  const columns = [
    { key: 'select', title: '', width: '40px' },
    { key: 'staff', title: '직원 이름', width: '120px' },
    { key: 'title', title: '일정 제목', width: '160px' },
    { key: 'period', title: '정기/단기', width: '120px' },
    { key: 'start', title: '시작 날짜', width: '160px' },
    { key: 'end', title: '마감 날짜', width: '160px' },
  ];

  const schedules = ref([
    {
      id: 1,
      staff: '김이글',
      title: '워크숍',
      type: 'regular_plan',
      period: '정기',
      date: '2025-06-17',
      startTime: '오후 01:00',
      endTime: '오후 04:00',
      start: '2025-06-17 오후 01:00',
      end: '2025-06-17 오후 04:00',
      duration: '03:00',
      timeRange: '오후 01:00 - 오후 04:00',
      repeatDescription: '반복 안함',
      memo: '외부 미팅 장소',
    },
    {
      id: 2,
      staff: '박이글',
      title: '세미나',
      type: 'plan',
      period: '단기',
      date: '2025-06-08',
      startTime: '오후 02:00',
      endTime: '오후 04:00',
      timeRange: '오후 02:00 - 오후 04:00',
      start: '2025-06-17 오후 02:00',
      end: '2025-06-17 오후 04:00',
      duration: '02:00',
      repeatDescription: '반복 안함',
      memo: '신제품 발표',
    },
  ]);

  for (let i = 3; i <= 15; i++) {
    const isRegular = i % 3 !== 0;
    const type = isRegular ? 'regular_plan' : 'plan';
    const period = type === 'regular_plan' ? '정기' : '단기';
    const staff = i % 2 === 0 ? '김이글' : '박이글';
    const title = isRegular ? '워크숍' : '세미나';
    const date = `2025-06-${String(i).padStart(2, '0')}`;

    schedules.value.push({
      id: i,
      staff,
      title,
      type,
      period,
      date,
      timeRange: '오후 01:00 - 오후 03:00',
      startTime: '오후 01:00',
      endTime: '오후 03:00',
      start: `${date} 오후 01:00`,
      end: `${date} 오후 03:00`,
      duration: '02:00',
      repeatDescription: isRegular ? '매주 화요일 반복' : '반복 안함',
      memo: `${staff} 일정 ${title}`,
    });
  }

  const filteredSchedules = computed(() =>
    schedules.value.filter(
      item =>
        (!selectedType.value || item.type === selectedType.value) &&
        (!selectedStaff.value || item.staff === selectedStaff.value)
    )
  );

  const pagedSchedules = computed(() => {
    const start = (currentPage.value - 1) * itemsPerPage;
    return filteredSchedules.value.slice(start, start + itemsPerPage);
  });

  const totalPages = computed(() => Math.ceil(filteredSchedules.value.length / itemsPerPage));

  const handlePageChange = page => {
    currentPage.value = page;
  };

  const allSelected = computed(
    () =>
      pagedSchedules.value.length > 0 &&
      pagedSchedules.value.every(item => selectedIds.value.includes(item.id))
  );

  const toggleAll = e => {
    if (e.target.checked) {
      const idsToAdd = pagedSchedules.value.map(item => item.id);
      selectedIds.value = [...new Set([...selectedIds.value, ...idsToAdd])];
    } else {
      selectedIds.value = selectedIds.value.filter(
        id => !pagedSchedules.value.some(item => item.id === id)
      );
    }
  };

  const deleteSelected = () => {
    schedules.value = schedules.value.filter(item => !selectedIds.value.includes(item.id));
    selectedIds.value = [];
  };

  const isModalOpen = ref(false);
  const selectedSchedule = ref(null);

  const handleRowClick = (row, event) => {
    if (event?.target?.type === 'checkbox') return;
    selectedSchedule.value = row;
    isModalOpen.value = true;
  };

  const closeModal = () => {
    isModalOpen.value = false;
    selectedSchedule.value = null;
  };

  const isRegistModalOpen = ref(false);
</script>

<style scoped>
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
</style>
