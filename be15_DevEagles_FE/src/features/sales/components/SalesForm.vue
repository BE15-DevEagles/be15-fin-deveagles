<template>
  <div class="sales-page-wrapper">
    <BaseToast ref="toastRef" />
    <div class="header-row">
      <h1 class="page-title">매출 등록/목록</h1>
      <div class="button-group">
        <BaseButton type="primary" @click="showMembershipModal = true">회원권 판매</BaseButton>
        <BaseButton type="primary" @click="showItemModal = true">상품 판매</BaseButton>
      </div>
    </div>

    <div class="search-bar">
      <BaseForm
        v-model="customerKeyword"
        type="text"
        placeholder="고객명/매출메모 검색"
        style="width: 300px"
        @keydown.enter="applySearch"
      />
      <BaseButton type="primary" style="transform: translateY(-8px)" @click="toggleFilterModal">
        필터
      </BaseButton>
    </div>

    <div v-if="filterState" class="filter-badges">
      <span v-if="filterState.startDate && filterState.endDate" class="badge">
        날짜: {{ formatDate(filterState.startDate) }} ~ {{ formatDate(filterState.endDate) }}
        <button @click="clearField('date')">×</button>
      </span>
      <span v-if="filterState.types?.length" class="badge">
        매출유형: {{ filterState.types.join(', ') }}
        <button @click="clearField('types')">×</button>
      </span>
      <span v-if="filterState.staff" class="badge">
        담당자: {{ filterState.staff }}
        <button @click="clearField('staff')">×</button>
      </span>
    </div>

    <SalesTableForm
      ref="salesTableRef"
      :customer-keyword="customerKeyword"
      :filter-state="filterState"
      :current-page="currentPage"
      :items-per-page="itemsPerPage"
      @update:total-items="totalItems = $event"
    />

    <SalesFilterModal v-if="showFilterModal" v-model="showFilterModal" @apply="handleFilterApply" />

    <div v-if="showMembershipModal" class="overlay" @click.self="showMembershipModal = false">
      <div class="modal-panel">
        <MembershipSalesRegistModal
          @close="showMembershipModal = false"
          @submit="handleMembershipSubmit"
        />
      </div>
    </div>

    <div v-if="showItemModal" class="overlay" @click.self="showItemModal = false">
      <div class="modal-panel">
        <ItemsSalesRegistModal @close="showItemModal = false" @submit="handleItemSubmit" />
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import SalesFilterModal from '@/features/sales/components/SalesFilterModal.vue';
  import SalesTableForm from '@/features/sales/components/SalesTableForm.vue';
  import MembershipSalesRegistModal from '@/features/sales/components/MembershipSalesRegistModal.vue';
  import ItemsSalesRegistModal from '@/features/sales/components/ItemsSalesRegistModal.vue';

  const customerKeyword = ref('');
  const filterState = ref(null);
  const showFilterModal = ref(false);
  const showMembershipModal = ref(false);
  const showItemModal = ref(false);
  const toastRef = ref(null);
  const currentPage = ref(1);
  const itemsPerPage = 10;
  const salesTableRef = ref(null);

  const toggleFilterModal = () => {
    showFilterModal.value = !showFilterModal.value;
  };

  const handleFilterApply = filters => {
    filterState.value = filters;
    currentPage.value = 1;
    showFilterModal.value = false;
  };

  const applySearch = () => {
    currentPage.value = 1;
  };

  const clearField = key => {
    if (!filterState.value) return;
    const newFilter = { ...filterState.value };
    switch (key) {
      case 'date':
        newFilter.startDate = null;
        newFilter.endDate = null;
        break;
      case 'types':
        newFilter.types = [];
        break;
      case 'staff':
        newFilter.staff = null;
        break;
    }
    filterState.value = newFilter;
  };

  const formatDate = date =>
    new Date(date).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });

  const handleMembershipSubmit = () => {
    showMembershipModal.value = false;
    toastRef.value?.success('회원권 매출이 등록되었습니다.');
    salesTableRef.value?.fetchSalesList();
  };

  const handleItemSubmit = () => {
    showItemModal.value = false;
    toastRef.value?.success('상품 매출이 등록되었습니다.');
    salesTableRef.value?.fetchSalesList();
  };
</script>

<style scoped>
  .sales-page-wrapper {
    padding: 2rem;
  }
  .header-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
  }
  .button-group {
    display: flex;
    gap: 8px;
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
