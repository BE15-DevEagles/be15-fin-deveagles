<template>
  <div class="staff-settlement-page">
    <h2 class="page-title">직원 결산</h2>

    <!-- 탭 + 설정 버튼 -->
    <div class="tab-row">
      <div class="tab-list">
        <BaseTab v-model="activeTab" :tabs="tabs" />
      </div>
      <div class="tab-action-button">
        <BaseButton v-if="activeTab !== '목표매출'" @click="openIncentivePopup">
          직원별 인센티브 설정
        </BaseButton>
        <BaseButton v-else @click="openTargetPopup"> 목표매출 설정 </BaseButton>
      </div>
    </div>

    <!-- 조회 필터 -->
    <div class="filter-row">
      <div class="radio-wrapper">
        <BaseForm
          v-model="searchMode"
          type="radio"
          :options="[
            { text: '월별 조회', value: 'MONTH' },
            { text: '기간별 조회', value: 'PERIOD' },
          ]"
        />
      </div>
      <div class="date-picker">
        <PrimeDatePicker
          v-if="searchMode === 'MONTH'"
          v-model="selectedMonth"
          view="month"
          selection-mode="single"
          placeholder="월 선택"
        />
        <PrimeDatePicker
          v-else
          v-model="selectedRange"
          view="date"
          selection-mode="range"
          placeholder="기간 선택"
        />
      </div>
      <input v-model="staffNameFilter" placeholder="직원 이름 검색" class="name-filter-input" />
      <div :class="['incentive-guide', { goal: activeTab === '목표매출' }]">
        <template v-if="activeTab === '목표매출'">
          ※ <span>일할 목표 매출</span>은 월 목표 매출을 월 일수로 나누어 계산되며, 조회 기간에 맞춰
          합산됩니다.
        </template>
        <template v-else> ※ 괄호 안의 <span>파란 숫자</span>는 인센티브 금액입니다. </template>
      </div>
    </div>
    <!-- 테이블 -->
    <div class="table-wrapper">
      <div v-if="loading" class="table-loading-overlay">
        <BaseLoading text="정산 내역을 불러오는 중입니다..." />
      </div>
      <BaseTable
        v-if="!loading && currentData.length > 0"
        :columns="currentColumns"
        :data="currentData"
        :row-class="getRowClass"
        :scroll="{ y: '600px' }"
        :pagination="false"
        :sticky-header="true"
      >
        <!-- 🎯 결산/상세결산 공통 SLOT -->
        <!-- 카드 -->
        <template v-if="activeTab !== '목표매출'" #cell-CARD="{ item }">
          {{ formatCurrency(item?.CARD ?? 0) }}
          <div class="incentive-amount">({{ formatCurrency(item?.CARD_INCENTIVE ?? 0) }})</div>
        </template>

        <!-- 현금 -->
        <template v-if="activeTab !== '목표매출'" #cell-CASH="{ item }">
          {{ formatCurrency(item?.CASH ?? 0) }}
          <div class="incentive-amount">({{ formatCurrency(item?.CASH_INCENTIVE ?? 0) }})</div>
        </template>

        <!-- 네이버페이 -->
        <template v-if="activeTab !== '목표매출'" #cell-NAVER_PAY="{ item }">
          {{ formatCurrency(item?.NAVER_PAY ?? 0) }}
          <div class="incentive-amount">({{ formatCurrency(item?.NAVER_PAY_INCENTIVE ?? 0) }})</div>
        </template>

        <!-- 지역화폐 -->
        <template v-if="activeTab !== '목표매출'" #cell-LOCAL="{ item }">
          {{ formatCurrency(item?.LOCAL ?? 0) }}
          <div class="incentive-amount">({{ formatCurrency(item?.LOCAL_INCENTIVE ?? 0) }})</div>
        </template>

        <!-- 할인 -->
        <template v-if="activeTab !== '목표매출'" #cell-DISCOUNT="{ item }">
          {{ formatCurrency(item?.DISCOUNT ?? 0) }}
        </template>

        <!-- 쿠폰 -->
        <template v-if="activeTab !== '목표매출'" #cell-COUPON="{ item }">
          {{ formatCurrency(item?.COUPON ?? 0) }}
        </template>

        <!-- 선불권 -->
        <template v-if="activeTab !== '목표매출'" #cell-PREPAID="{ item }">
          {{ formatCurrency(item?.PREPAID ?? 0) }}
        </template>

        <!-- 총 공제 -->
        <template v-if="activeTab !== '목표매출'" #cell-totalDeductions="{ item }">
          {{ formatCurrency(item?.totalDeductions ?? 0) }}
        </template>

        <!-- 최종 실매출 -->
        <template v-if="activeTab !== '목표매출'" #cell-finalSales="{ item }">
          {{ formatCurrency(item?.finalSales ?? 0) }}
        </template>

        <!-- 총 실매출 -->
        <template v-if="activeTab !== '목표매출'" #cell-totalSales="{ item }">
          {{ formatCurrency(item?.totalSales ?? 0) }}
        </template>

        <!-- 📌 목표매출 전용 SLOT -->
        <template v-if="activeTab === '목표매출'" #cell-rate="{ item }">
          <span :class="{ 'highlight-rate': item?.rate >= 100 }">
            {{ item?.rate?.toFixed(1) }}%
          </span>
        </template>

        <template v-if="activeTab === '목표매출'" #cell-actual="{ item }">
          {{ formatCurrency(item?.actual ?? 0) }}
        </template>

        <template v-if="activeTab === '목표매출'" #cell-target="{ item }">
          {{ formatCurrency(item?.target ?? 0) }}
        </template>
      </BaseTable>
      <div v-else-if="!loading && activeTab === '목표매출'" class="custom-empty-box">
        <p>등록된 목표 매출이 없습니다. 목표 매출을 설정해 주세요 😆</p>
        <button class="copy-button">최근 목표 복사하기</button>
      </div>
    </div>
  </div>
  <BaseSlidePanel
    v-if="showIncentiveModal"
    :title="'인센티브 설정'"
    @close="showIncentiveModal = false"
  >
    <IncentiveSettingModal
      v-if="incentiveData"
      ref="incentiveModalRef"
      v-model:selected-staff-id="selectedStaffId"
      :incentive-data="incentiveData"
      :toast="toastRef"
      @saved="handleSaved"
    />
    <template #footer>
      <div class="footer-btn-row">
        <BaseButton type="primary" @click="incentiveModalRef?.handleSave?.()">저장하기</BaseButton>
      </div>
    </template>
  </BaseSlidePanel>

  <BaseSlidePanel
    v-if="showTargetSalesModal"
    :key="'target-slide'"
    title="목표매출 설정"
    @close="showTargetSalesModal = false"
  >
    <TargetSalesSettingModal
      ref="targetModalRef"
      :search-mode="searchMode"
      :selected-month="selectedMonth"
      :selected-range="selectedRange"
      :toast="toastRef"
      @saved="handleSaved"
    />
    <template #footer>
      <div class="footer-btn-row">
        <BaseButton type="primary" @click="targetModalRef?.handleSave?.()">저장하기</BaseButton>
      </div>
    </template>
  </BaseSlidePanel>
  <BaseToast ref="toastRef" />
</template>

<script setup>
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import BaseLoading from '@/components/common/BaseLoading.vue';
  import BaseTab from '@/components/common/BaseTab.vue';
  import IncentiveSettingModal from '@/features/staffsales/components/IncentiveSettingModal.vue';
  import BaseSlidePanel from '@/features/staffsales/components/BaseSlidePanel.vue';
  import TargetSalesSettingModal from '@/features/staffsales/components/TargetSalesSettingModal.vue';
  import {
    getIncentives,
    getStaffDetailSales,
    getStaffSales,
    getStaffTargetSales,
  } from '@/features/staffsales/api/staffsales.js';
  import BaseToast from '@/components/common/BaseToast.vue';
  import { useStaffSales } from '@/features/staffsales/composables/useStaffSales.js';
  import { useFlattenSales } from '@/features/staffsales/composables/useFlattenSales.js';

  const toastRef = ref();
  const activeTab = ref('직원별 결산');
  const tabs = ['직원별 결산', '직원별 상세결산', '목표매출'];
  const searchMode = ref('MONTH');
  const selectedMonth = ref(new Date());
  const selectedRange = ref([]);
  const showIncentiveModal = ref(false);
  const showTargetSalesModal = ref(false);
  const loading = ref(false);
  const staffSalesApiData = ref(null);
  const staffNameFilter = ref('');
  const incentiveData = ref(null);
  const incentiveModalRef = ref();
  const targetModalRef = ref();
  const selectedStaffId = ref(null);

  const { categoryLabelMap, formatCurrency, getFormattedDates } = useStaffSales();

  const { flattenStaffSalesList, flattenDetailData, flattenTargetSales } = useFlattenSales({
    categoryLabelMap,
    staffNameFilter,
  });

  const baseColumns = [
    { title: '직원 이름', key: 'name' },
    { title: '상품 구분', key: 'category' },
    // 매출 항목
    { title: '카드', key: 'CARD' },
    { title: '현금', key: 'CASH' },
    { title: '네이버페이', key: 'NAVER_PAY' },
    { title: '지역화폐', key: 'LOCAL' },
    { title: '실매출액', key: 'totalSales' },
    // 공제 항목
    { title: '할인', key: 'DISCOUNT' },
    { title: '쿠폰', key: 'COUPON' },
    { title: '선불권', key: 'PREPAID' },
    // 합계
    { title: '총 공제', key: 'totalDeductions' },
    { title: '총영업액', key: 'finalSales' },
  ];

  const detailColumns = [
    { title: '직원 이름', key: 'name' },
    { title: '상품 구분', key: 'category' },
    { title: '1차', key: 'primary' },
    { title: '2차', key: 'secondary' },
    // 매출 항목
    { title: '카드', key: 'CARD' },
    { title: '현금', key: 'CASH' },
    { title: '네이버페이', key: 'NAVER_PAY' },
    { title: '지역화폐', key: 'LOCAL' },
    { title: '실매출액', key: 'totalSales' },
    // 공제 항목
    { title: '할인', key: 'DISCOUNT' },
    { title: '쿠폰', key: 'COUPON' },
    { title: '선불권', key: 'PREPAID' },
    // 합계
    { title: '총 공제', key: 'totalDeductions' },
    { title: '총영업액', key: 'finalSales' },
  ];

  const targetColumns = [
    { title: '직원 이름', key: 'name' },
    { title: '분류', key: 'category' },
    { title: '목표', key: 'target' },
    { title: '총영업액', key: 'actual' },
    { title: '달성률', key: 'rate' },
  ];

  const currentColumns = computed(() => {
    if (activeTab.value === '목표매출') return targetColumns;
    if (activeTab.value === '직원별 상세결산') return detailColumns;
    return baseColumns; // 직원별 결산
  });

  const currentData = computed(() => {
    if (!staffSalesApiData.value) return [];
    switch (activeTab.value) {
      case '직원별 상세결산':
        return flattenDetailData(staffSalesApiData.value);
      case '목표매출':
        return flattenTargetSales(staffSalesApiData.value.staffSalesList);
      default:
        return flattenStaffSalesList(staffSalesApiData.value);
    }
  });

  const fetchStaffSales = async () => {
    loading.value = true;
    try {
      staffSalesApiData.value = null;
      const { startDate, endDate } = getFormattedDates(
        searchMode.value,
        selectedMonth.value,
        selectedRange.value
      );

      const payload = {
        searchMode: searchMode.value,
        startDate,
      };

      if (endDate) payload.endDate = endDate;

      let data;

      if (activeTab.value === '직원별 상세결산') data = await getStaffDetailSales(payload);
      else if (activeTab.value === '목표매출') data = await getStaffTargetSales(payload);
      else data = await getStaffSales(payload);

      staffSalesApiData.value = data.data.data;
    } catch (err) {
      toastRef.value?.error?.('직원 결산 조회에 실패했습니다.');
      console.error(`직원 결산 조회 실패`, err);
      staffSalesApiData.value = { staffSalesList: [] };
    } finally {
      loading.value = false;
    }
  };

  watch([searchMode, selectedMonth, selectedRange, activeTab], () => {
    const isMonthValid = searchMode.value === 'MONTH';
    const isPeriodValid =
      searchMode.value === 'PERIOD' &&
      Array.isArray(selectedRange.value) &&
      selectedRange.value.length === 2 &&
      selectedRange.value[0] &&
      selectedRange.value[1];

    if (isMonthValid || isPeriodValid) {
      fetchStaffSales();
    }
  });

  const openIncentivePopup = async () => {
    try {
      const data = await getIncentives();
      incentiveData.value = data.data.data;
      showIncentiveModal.value = true;
    } catch (err) {
      toastRef.value?.error?.('인센티브 조회에 실패했습니다.');
      console.error(`인센티브 조회 실패 ${err}`);
    }
  };

  const handleSaved = updatedData => {
    const list = updatedData?.data?.data?.incentiveList ?? [];
    incentiveData.value = updatedData?.data?.data;

    if (selectedStaffId.value) {
      const exists = list.some(i => i.staffId === selectedStaffId.value);
      if (!exists) selectedStaffId.value = null;
    }
    fetchStaffSales();
  };

  const openTargetPopup = () => {
    showTargetSalesModal.value = true;
  };

  const getRowClass = row => {
    if (row.name === '총계' && row.category === '') return 'summary-row';
    if (row.category === '총계') return 'staff-summary-row';
    return '';
  };
  const handleKeydown = e => {
    const isTyping = ['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName);
    if (isTyping) return;

    if (e.key === 'Escape') {
      if (showTargetSalesModal.value) showTargetSalesModal.value = false;
      else if (showIncentiveModal.value) showIncentiveModal.value = false;
    }

    if (e.key === 'Enter') {
      if (showTargetSalesModal.value) {
        targetModalRef.value?.handleSave?.();
      } else if (showIncentiveModal.value) {
        incentiveModalRef.value?.handleSave?.();
      }
    }
  };

  onMounted(() => {
    window.addEventListener('keydown', handleKeydown);
  });
  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown);
  });

  onMounted(() => {
    const { startDate, endDate } = getFormattedDates();
    if ((searchMode.value === 'PERIOD' && startDate && endDate) || searchMode.value === 'MONTH') {
      fetchStaffSales();
    }
  });
</script>

<style scoped>
  .page-title {
    font-size: 24px;
    font-weight: bold;
    margin-bottom: 16px;
  }
  .tab-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
  .table-wrapper {
    background-color: #fff;
    border-radius: 12px;
    box-shadow:
      0 0 0 1px #e5e7eb,
      0 1px 3px rgba(0, 0, 0, 0.05);
    padding: 24px;
  }
  .radio-wrapper {
    display: flex;
    align-items: center;
    height: 40px;
    font-size: 13px;
  }
  .radio-wrapper label {
    font-size: 13px;
    margin: 0;
  }
  .radio-wrapper :deep(.form-group) {
    display: flex;
    align-items: center;
    gap: 16px;
    margin: 0;
    padding: 0;
  }
  .radio-wrapper :deep(.radio-group) {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 16px;
    min-height: 100%;
  }
  .radio-wrapper :deep(.radio-group .radio) {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .radio-wrapper :deep(.radio-group label),
  .radio-wrapper :deep(.radio-group input[type='radio']) {
    font-size: 13px;
  }
  :deep(.summary-row) {
    font-weight: bold;
    background-color: #eaeaea;
  }
  :deep(.staff-summary-row) {
    font-weight: bold;
    background-color: #f6f6f6;
  }
  .staff-settlement-page {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .incentive-amount {
    font-size: 12px;
    color: #3f51b5;
  }
  .filter-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 1rem;
  }
  .name-filter-input {
    height: 100%;
    padding: 12px;
    font-size: 14px;
    border: 1px solid var(--surface-border, #cbd5e1);
    border-radius: 6px;
    line-height: 1.5;
    box-sizing: border-box;
    transition:
      border-color 0.2s ease,
      box-shadow 0.2s ease;
  }
  .name-filter-input:focus {
    outline: none;
    border-color: #1a2331;
    box-shadow: 0 0 0 0.125rem rgba(0, 0, 0, 0.1);
  }
  .incentive-guide {
    font-size: 13px;
    color: var(--color-gray-600);
  }

  .incentive-guide span {
    font-weight: 500;
  }

  .incentive-guide.goal span {
    color: var(--color-gray-600); /* 목표매출 안내문에는 텍스트색과 동일 */
  }

  .incentive-guide:not(.goal) span {
    color: #3f51b5; /* 기존 인센티브용 파란색 유지 */
  }

  .custom-empty-box {
    text-align: center;
    padding: 40px 0;
  }
  .custom-empty-box p {
    margin: 4px 0;
    color: var(--color-gray-400);
  }
  .custom-empty-box .copy-button {
    margin-top: 16px;
    padding: 10px 20px;
    background-color: var(--color-success-300); /* 민트색 */
    color: var(--color-neutral-white);
    border: none;
    border-radius: 8px;
    font-weight: 500;
    cursor: pointer;
    transition: background-color 0.2s;
  }
  .custom-empty-box .copy-button:hover {
    background-color: var(--color-success-400);
  }
  .highlight-rate {
    color: var(--color-success-500);
    font-weight: 700;
    animation: blink 1s ease-in-out infinite alternate;
  }
  @keyframes blink {
    from {
      opacity: 1;
    }
    to {
      opacity: 0.6;
    }
  }
  :deep(.sticky-header thead th) {
    top: -24px !important;
    z-index: 20;
  }
</style>
