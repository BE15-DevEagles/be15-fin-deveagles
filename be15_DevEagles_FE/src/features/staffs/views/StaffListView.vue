<template>
  <div class="staff-container">
    <!-- 상단 헤더 -->
    <div class="staff-header">
      <BaseButton @click="goToCreate">+ 직원 추가</BaseButton>
      <div class="search-area">
        <input
          v-model="searchText"
          placeholder="이름 검색"
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <BaseButton @click="resetFilters">초기화</BaseButton>

        <label class="checkbox-label">
          <input v-model="onlyActive" type="checkbox" />
          재직자만 보기
        </label>
      </div>
    </div>

    <!-- 테이블 -->
    <div class="table-wrapper">
      <div v-if="loading" class="table-loading-overlay">
        <BaseLoading text="직원 목록을 조회하는 중입니다..." />
      </div>
      <BaseTable
        v-if="!loading"
        :columns="columns"
        :data="staffList"
        :hover="true"
        :striped="true"
        :row-key="'staffId'"
        @click-row="goToDetail"
      >
        <!-- 이름 셀 커스터마이징 -->
        <template #cell-staffName="{ item, value }">
          <div class="name-cell" @click="goToDetail(item)">
            <div class="color-box" :style="{ backgroundColor: item.colorCode }"></div>
            {{ value }}&nbsp;
            <p style="color: #888888">({{ item.loginId }})</p>
          </div>
        </template>
        <!-- 재직 상태 커스터마이징 -->
        <template #cell-isActive="{ item }">
          <BaseBadge
            :type="item.working ? 'success' : 'error'"
            :text="item.working ? '재직' : '퇴직'"
            dot
          />
        </template>
      </BaseTable>
    </div>

    <!-- 페이지네이션 -->
    <Pagination
      :current-page="page"
      :total-pages="totalPages"
      :total-items="totalCount"
      :items-per-page="limit"
      @page-change="handlePageChange"
    />
  </div>
  <BaseToast ref="toastRef" />
</template>

<script setup>
  import { ref, computed, onMounted, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import BaseTable from '@/components/common/BaseTable.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import Pagination from '@/components/common/Pagination.vue';
  import { getStaff } from '@/features/staffs/api/staffs.js';
  import BaseToast from '@/components/common/BaseToast.vue';
  import BaseBadge from '@/components/common/BaseBadge.vue';
  import BaseLoading from '@/components/common/BaseLoading.vue';

  const router = useRouter();
  const toastRef = ref();
  const staffList = ref([]);

  const page = ref(1);
  const limit = ref(10);
  const totalCount = ref(0);
  const onlyActive = ref(false);
  const loading = ref(false);

  const columns = [
    { key: 'staffName', title: '이름', width: '200px' },
    { key: 'phoneNumber', title: '연락처', width: '180px' },
    { key: 'grade', title: '직급', width: '150px' },
    { key: 'isActive', title: '재직 상태', width: '120px' },
  ];

  const totalPages = computed(() => Math.ceil(totalCount.value / limit.value));

  const searchText = ref('');
  let typingTimer = null;
  const debounceDelay = 300;

  // 검색
  watch(searchText, () => {
    if (typingTimer) clearTimeout(typingTimer);
    typingTimer = setTimeout(() => {
      page.value = 1;
      fetchStaff();
    }, debounceDelay);
  });

  // 엔터 키로 검색
  const handleSearch = () => {
    if (typingTimer) clearTimeout(typingTimer);
    page.value = 1;
    fetchStaff();
  };

  const fetchStaff = async () => {
    loading.value = true;
    try {
      const res = await getStaff({
        page: page.value,
        size: limit.value,
        keyword: searchText.value || null,
        isActive: onlyActive.value,
      });

      staffList.value = res.data.data.staffList;
      totalCount.value = res.data.data.pagination.totalItems;
      loading.value = false;
    } catch (err) {
      toastRef.value?.error?.('직원 목록 조회에 실패했습니다.');
    }
  };

  const handlePageChange = newPage => {
    page.value = newPage;
    fetchStaff();
  };

  const resetFilters = () => {
    searchText.value = '';
    onlyActive.value = false;
    page.value = 1;
    fetchStaff();
  };

  const goToCreate = () => {
    router.push({ name: 'StaffRegist' });
  };

  const goToDetail = staff => {
    router.push({ name: 'StaffDetail', params: { staffId: staff.staffId } });
  };

  onMounted(() => {
    fetchStaff();
  });

  watch(onlyActive, () => {
    page.value = 1;
    fetchStaff();
  });
</script>

<style scoped>
  .staff-container {
    padding: 20px;
  }

  .staff-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .search-area {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  .search-input {
    padding: 8px;
    border: 1px solid #ccc;
    border-radius: 6px;
  }

  .checkbox-label {
    font-size: 14px;
    color: #555;
  }

  .color-box {
    width: 12px;
    height: 12px;
    display: inline-block;
    margin-right: 8px;
    border-radius: 2px;
  }

  .name-cell {
    display: flex;
    align-items: center;
    cursor: pointer;
  }

  .table-wrapper {
    background-color: #fff;
    padding: 1.5rem;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    margin-top: 1.5rem;
  }
</style>
