<template>
  <div class="leave-wrapper">
    <div class="page-header">
      <h1 class="font-screen-title">휴무 목록</h1>
    </div>

    <!-- 상단 버튼 + 필터 바 -->
    <div class="top-bar-flex">
      <!-- 왼쪽: 휴무 삭제 -->
      <div class="left-bar">
        <BaseButton type="error" :disabled="selectedIds.length === 0" @click="deleteSelected">
          휴무 삭제
        </BaseButton>
      </div>

      <!-- 오른쪽: 필터 + 등록 -->
      <div class="right-bar">
        <BaseForm
          v-model="selectedType"
          type="select"
          :options="[
            { text: '전체', value: '' },
            { text: '단기', value: 'leave' },
            { text: '정기', value: 'regular_leave' },
          ]"
          placeholder="휴무 종류"
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

        <!-- ✅ 휴무 등록 버튼 클릭 시 모달 오픈 + leave 탭으로 -->
        <BaseButton type="primary" class="fix-button-height" @click="openLeaveModal">
          휴무 등록
        </BaseButton>
      </div>
    </div>

    <!-- 테이블 -->
    <div class="base-table-wrapper">
      <BaseTable
        :columns="columns"
        :data="pagedLeaves"
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

        <template #cell-date="{ value }">
          <div class="text-left">{{ formatDate(value) }}</div>
        </template>
      </BaseTable>
    </div>

    <BasePagination
      :current-page="currentPage"
      :total-pages="totalPages"
      :total-items="filteredLeaves.length"
      :items-per-page="itemsPerPage"
      @page-change="handlePageChange"
    />

    <!-- 상세 모달 -->
    <LeaveDetailModal
      v-if="isModalOpen"
      :model-value="isModalOpen"
      :reservation="selectedLeave"
      @update:model-value="closeModal"
    />

    <!-- ✅ 등록 모달 -->
    <ScheduleRegistModal
      v-if="isRegistModalOpen"
      v-model="isRegistModalOpen"
      :default-tab="'leave'"
    />
  </div>
</template>

<script setup>
  import { ref, computed } from 'vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BasePagination from '@/components/common/Pagination.vue';
  import LeaveDetailModal from '@/features/schedules/components/LeaveDetailModal.vue';
  import ScheduleRegistModal from '@/features/schedules/components/ScheduleRegistModal.vue'; // ✅ 모달 import

  const selectedType = ref('');
  const selectedStaff = ref('');
  const selectedIds = ref([]);

  const currentPage = ref(1);
  const itemsPerPage = 10;

  const columns = [
    { key: 'select', title: '', width: '40px' },
    { key: 'staff', title: '직원 이름', width: '120px' },
    { key: 'title', title: '휴무 제목', width: '160px' },
    { key: 'typeLabel', title: '정기/단기', width: '120px' },
    { key: 'date', title: '날짜', width: '180px' },
  ];

  const leaves = ref([
    { id: 1, staff: '김이글', title: '휴무', type: 'leave', date: '2025-06-08' },
    { id: 2, staff: '박이글', title: '정기 휴무', type: 'regular_leave', date: '2025-06-12' },
    { id: 3, staff: '김이글', title: '병가', type: 'leave', date: '2025-06-08' },
  ]);

  for (let i = 4; i <= 15; i++) {
    leaves.value.push({
      id: i,
      staff: i % 2 === 0 ? '김이글' : '박이글',
      title: i % 3 === 0 ? '정기 휴무' : '휴무',
      type: i % 3 === 0 ? 'regular_leave' : 'leave',
      date: `2025-06-${String(i).padStart(2, '0')}`,
    });
  }

  const filteredLeaves = computed(() =>
    leaves.value
      .map(item => ({
        ...item,
        typeLabel: item.type === 'regular_leave' ? '정기' : '단기',
        start: item.date,
      }))
      .filter(
        item =>
          (!selectedType.value || item.type === selectedType.value) &&
          (!selectedStaff.value || item.staff === selectedStaff.value)
      )
  );

  const pagedLeaves = computed(() => {
    const start = (currentPage.value - 1) * itemsPerPage;
    return filteredLeaves.value.slice(start, start + itemsPerPage);
  });

  const totalPages = computed(() => Math.ceil(filteredLeaves.value.length / itemsPerPage));

  const handlePageChange = page => {
    currentPage.value = page;
  };

  const formatDate = dateStr => {
    const date = new Date(dateStr);
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    const day = ['일', '월', '화', '수', '목', '금', '토'][date.getDay()];
    return `${yyyy}.${mm}.${dd} (${day})`;
  };

  const allSelected = computed(
    () =>
      pagedLeaves.value.length > 0 &&
      pagedLeaves.value.every(item => selectedIds.value.includes(item.id))
  );

  const toggleAll = e => {
    if (e.target.checked) {
      const idsToAdd = pagedLeaves.value.map(item => item.id);
      selectedIds.value = [...new Set([...selectedIds.value, ...idsToAdd])];
    } else {
      selectedIds.value = selectedIds.value.filter(
        id => !pagedLeaves.value.some(item => item.id === id)
      );
    }
  };

  const deleteSelected = () => {
    leaves.value = leaves.value.filter(item => !selectedIds.value.includes(item.id));
    selectedIds.value = [];
  };

  // 상세 모달
  const isModalOpen = ref(false);
  const selectedLeave = ref(null);

  const handleRowClick = (row, event) => {
    if (event?.target?.type === 'checkbox') return;
    selectedLeave.value = row;
    isModalOpen.value = true;
  };

  const closeModal = () => {
    isModalOpen.value = false;
    selectedLeave.value = null;
  };

  // ✅ 등록 모달 상태
  const isRegistModalOpen = ref(false);

  const openLeaveModal = () => {
    isRegistModalOpen.value = true;
  };
</script>

<style scoped>
  .fix-button-height {
    height: 40px;
    padding: 0 16px !important;
    font-size: 14px;
    line-height: 1 !important;
  }

  .leave-wrapper {
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
</style>
