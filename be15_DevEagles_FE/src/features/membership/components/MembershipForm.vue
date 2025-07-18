<template>
  <div class="membership-page-wrapper">
    <!-- 헤더 -->
    <div class="header-row">
      <h1 class="page-title">회원권 관리 페이지</h1>
      <BaseButton class="register-button" @click="modalVisible = true">회원권 관리</BaseButton>
    </div>

    <!-- 상단 탭 -->
    <div class="tab-filter-row">
      <div class="tab-group">
        <div class="tab" :class="{ active: activeTab === '전체' }" @click="activeTab = '전체'">
          전체
        </div>
        <div
          class="tab"
          :class="{ active: activeTab === '만료예정' }"
          @click="activeTab = '만료예정'"
        >
          만료(예정)
        </div>
      </div>
    </div>

    <!-- 하위 탭 (만료 예정만) -->
    <div v-if="activeTab === '만료예정'" class="sub-tab-row">
      <div class="tab-group">
        <div class="tab" :class="{ active: expiryType === '선불' }" @click="expiryType = '선불'">
          선불권
        </div>
        <div
          class="tab"
          :class="{ active: expiryType === '횟수권' }"
          @click="expiryType = '횟수권'"
        >
          횟수권
        </div>
      </div>
    </div>

    <!-- 검색/필터 -->
    <div class="search-bar">
      <BaseForm
        v-model="searchKeyword"
        type="text"
        placeholder="고객명, 전화번호 검색"
        style="width: 300px"
        @keydown.enter="applySearch"
      />
      <BaseButton
        class="filter-button"
        style="transform: translateY(-8px)"
        @click="toggleFilterModal"
      >
        필터
      </BaseButton>
    </div>

    <!-- 필터 뱃지 -->
    <div v-if="filterState" class="filter-badges">
      <!-- 전체 탭 전용 뱃지 -->
      <template v-if="activeTab === '전체'">
        <span v-if="filterState.min || filterState.max" class="badge">
          잔여:
          {{
            filterState.min && filterState.max
              ? `${filterState.min} ~ ${filterState.max}`
              : filterState.min
                ? `${filterState.min}~`
                : `~${filterState.max}`
          }}
          <button @click="clearFilter">×</button>
        </span>

        <span v-if="filterState.startDate && filterState.endDate" class="badge">
          만료(예정)일: {{ formatDate(filterState.startDate) }} ~
          {{ formatDate(filterState.endDate) }}
          <button @click="clearFilter">×</button>
        </span>
      </template>

      <!-- 만료(예정) 탭 전용 뱃지 -->
      <template v-else-if="activeTab === '만료예정'">
        <!-- 선불권 -->
        <span
          v-if="
            expiryType === '선불' &&
            (filterState.minRemainingAmount || filterState.maxRemainingAmount)
          "
          class="badge"
        >
          잔여:
          {{
            filterState.minRemainingAmount && filterState.maxRemainingAmount
              ? `${filterState.minRemainingAmount} ~ ${filterState.maxRemainingAmount}`
              : filterState.minRemainingAmount
                ? `${filterState.minRemainingAmount}~`
                : `~${filterState.maxRemainingAmount}`
          }}
          <button @click="clearFilter">×</button>
        </span>

        <!-- 횟수권 -->
        <span
          v-if="
            expiryType === '횟수권' &&
            (filterState.minRemainingCount || filterState.maxRemainingCount)
          "
          class="badge"
        >
          잔여:
          {{
            filterState.minRemainingCount && filterState.maxRemainingCount
              ? `${filterState.minRemainingCount} ~ ${filterState.maxRemainingCount}`
              : filterState.minRemainingCount
                ? `${filterState.minRemainingCount}~`
                : `~${filterState.maxRemainingCount}`
          }}
          <button @click="clearFilter">×</button>
        </span>

        <!-- 날짜 필터 뱃지: startDate 또는 endDate 한쪽만 있어도 출력 -->
        <span v-if="filterState.startDate || filterState.endDate" class="badge">
          만료(예정)일:
          {{ formatDate(filterState.startDate) }} ~
          {{ formatDate(filterState.endDate) }}
          <button @click="clearFilter">×</button>
        </span>
      </template>
    </div>

    <!-- 테이블 -->
    <MembershipTableForm
      v-if="activeTab === '전체'"
      :search-keyword="searchKeyword"
      :filter-state="filterState"
    />
    <MembershipExpiryTableForm
      v-else
      :type="expiryType"
      :search-keyword="searchKeyword"
      :filter="filterState"
    />

    <!-- 필터 모달 -->
    <MembershipFilterModal
      v-if="activeTab === '전체' && showFilterModal"
      v-model="showFilterModal"
      @apply="handleFilterApply"
    />
    <MembershipExpiryFilterModal
      v-if="activeTab === '만료예정' && showFilterModal"
      v-model="showFilterModal"
      :type="expiryType"
      @apply="handleExpiryFilterApply"
    />

    <!-- 상세 모달 -->
    <div v-if="modalVisible" class="overlay" @click.self="modalVisible = false">
      <div class="modal-panel">
        <MembershipDetailModal @close="modalVisible = false" />
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, onMounted, watch } from 'vue';
  import MembershipFilterModal from '@/features/membership/components/MembershipFilterModal.vue';
  import MembershipExpiryFilterModal from '@/features/membership/components/MembershipExpiryFilterModal.vue';
  import MembershipTableForm from '@/features/membership/components/MembershipTableForm.vue';
  import MembershipExpiryTableForm from '@/features/membership/components/MembershipExpiryTableForm.vue';
  import MembershipDetailModal from '@/features/membership/components/MembershipDetailModal.vue';

  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';

  const activeTab = ref('전체');
  const expiryType = ref('선불');
  const showFilterModal = ref(false);
  const modalVisible = ref(false);
  const searchKeyword = ref('');
  const filterState = ref(null);

  const toggleFilterModal = () => {
    showFilterModal.value = !showFilterModal.value;
  };

  const handleFilterApply = filters => {
    filterState.value = filters;
    showFilterModal.value = false;
  };

  const handleExpiryFilterApply = filters => {
    filterState.value = filters; // ✅ type은 넣지 않음
    showFilterModal.value = false;
  };

  const clearFilter = () => {
    filterState.value = null;
  };

  const applySearch = () => {
    // optional: 검색 키워드 처리
  };

  const formatDate = (date, endOfDay = false) => {
    if (!date) return null;

    const d = new Date(date);
    if (endOfDay) {
      d.setHours(23, 59, 59, 999);
    } else {
      d.setHours(0, 0, 0, 0);
    }

    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  };

  // ✅ 초기 만료 필터 설정 함수
  const initializeDefaultExpiryFilter = () => {
    const today = new Date();
    const twoWeeksLater = new Date();
    twoWeeksLater.setDate(today.getDate() + 14);

    filterState.value = {
      minRemainingAmount: null,
      maxRemainingAmount: null,
      minRemainingCount: null,
      maxRemainingCount: null,
      startDate: today,
      endDate: twoWeeksLater,
    };
  };

  // ✅ 탭 변경 시 기본 필터 적용
  watch(
    activeTab,
    newVal => {
      if (newVal === '만료예정') {
        if (filterState.value == null) {
          initializeDefaultExpiryFilter();
        }
      } else if (newVal === '전체') {
        filterState.value = null; // 전체 탭 오면 필터 초기화
      }
    },
    { immediate: true }
  );

  // ✅ 최초 진입 시 기본 필터 적용
  onMounted(() => {
    if (activeTab.value === '만료예정' && filterState.value == null) {
      initializeDefaultExpiryFilter();
    }
  });
</script>

<style scoped>
  .membership-page-wrapper {
    padding: 2rem;
  }
  .header-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
  }
  .tab-filter-row,
  .sub-tab-row {
    display: flex;
    margin-bottom: 1rem;
    font-weight: bold;
  }
  .tab-group {
    display: flex;
    width: 220px;
    border: 1px solid #364f6b;
    border-radius: 6px;
    overflow: hidden;
  }
  .tab {
    flex: 1;
    text-align: center;
    padding: 0.5rem 0;
    font-size: 14px;
    cursor: pointer;
    background-color: white;
    color: #364f6b;
    font-weight: bold;
    border-right: 1px solid #364f6b;
  }
  .tab:last-child {
    border-right: none;
  }
  .tab.active {
    background-color: #364f6b;
    color: white;
    font-weight: bold;
  }
  .search-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 1rem;
  }
  .filter-badges {
    display: flex;
    gap: 8px;
    margin-bottom: 1rem;
    flex-wrap: wrap;
  }
  .badge {
    background-color: #ff6b91;
    color: white;
    padding: 4px 10px;
    border-radius: 20px;
    font-size: 13px;
    display: flex;
    align-items: center;
  }
  .badge button {
    background: none;
    border: none;
    color: white;
    font-size: 14px;
    margin-left: 6px;
    cursor: pointer;
  }
  .overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100vh;
    background: rgba(0, 0, 0, 0.3);
    z-index: 1000;
  }
  .modal-panel {
    position: fixed;
    top: 0;
    left: 240px;
    width: calc(100% - 240px);
    height: 100vh;
    background: white;
    display: flex;
    flex-direction: column;
    padding: 24px;
    overflow-y: auto;
  }
</style>
