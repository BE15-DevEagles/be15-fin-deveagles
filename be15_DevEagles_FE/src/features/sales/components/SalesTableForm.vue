<template>
  <div class="base-table-wrapper">
    <!-- 매출 테이블 -->
    <BaseTable
      :columns="columns"
      :data="displaySales"
      :loading="loading"
      striped
      @row-click="openModal"
    />

    <!-- 페이지네이션 -->
    <BasePagination
      v-if="pagination.totalPages > 1"
      :current-page="pagination.currentPage"
      :total-pages="pagination.totalPages"
      :total-items="pagination.totalItems"
      :items-per-page="pageSize"
      @page-change="handlePageChange"
    />

    <!-- 상품 상세 모달 -->
    <ItemSalesDetailModal
      v-if="detailModalVisible && selectedSalesItem?.salesType === 'ITEMS'"
      :sales-item="selectedSalesItem"
      @close="closeModal"
    />

    <!-- 회원권 상세 모달 -->
    <MembershipSalesDetailModal
      v-if="detailModalVisible && selectedSalesItem?.salesType === 'MEMBERSHIP'"
      :sales-item="selectedSalesItem"
      @close="closeModal"
    />

    <!-- 환불 상세 모달 -->
    <RefundDetailModal
      v-if="detailModalVisible && selectedSalesItem?.salesType === 'REFUND'"
      :sales-item="selectedSalesItem"
      @close="closeModal"
    />
  </div>
</template>

<script setup>
  import { ref, watch } from 'vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import BasePagination from '@/components/common/Pagination.vue';
  import ItemSalesDetailModal from '@/features/sales/components/ItemSalesDetailModal.vue';
  import MembershipSalesDetailModal from '@/features/sales/components/MembershipSalesDetailModal.vue';
  import RefundDetailModal from '@/features/sales/components/RefundDetailModal.vue';
  import { getSalesList } from '@/features/sales/api/sales.js';

  const props = defineProps({
    customerKeyword: String,
    filterState: Object,
  });

  const displaySales = ref([]);
  const originalSales = ref([]);
  const loading = ref(false);
  const detailModalVisible = ref(false);
  const selectedSalesItem = ref(null);
  const pageSize = 10;

  const pagination = ref({
    currentPage: 1,
    totalPages: 1,
    totalItems: 0,
  });

  const mapPaymentMethodToKorean = method => {
    switch (method?.toLowerCase()) {
      case 'cash':
        return '현금';
      case 'card':
        return '카드';
      case 'naver_pay':
        return '네이버페이';
      case 'session_pass':
        return '횟수권';
      case 'prepaid_pass':
        return '선불권';
      case 'local':
        return '지역화폐';
      default:
        return method ?? '-';
    }
  };

  const fetchSalesList = async () => {
    loading.value = true;
    try {
      const filters = {
        startDate: props.filterState?.startDate,
        endDate: props.filterState?.endDate,
        staffName: props.filterState?.staff,
        saleTypes: props.filterState?.types
          ?.map(type => {
            if (type === '상품') return 'ITEMS';
            if (type === '선불액·정기권') return 'MEMBERSHIP';
            if (type === '환불') return 'REFUND';
            return null;
          })
          .filter(Boolean),
        customerKeyword: props.customerKeyword,
        page: pagination.value.currentPage,
        size: pageSize,
      };

      const result = await getSalesList(filters);
      originalSales.value = result.list;

      displaySales.value = result.list.map(item => ({
        id: item.salesId,
        date: item.salesDate ? item.salesDate.replace('T', ' ').slice(0, 16) : '-',
        type:
          item.salesType === 'ITEMS'
            ? '상품'
            : item.salesType === 'MEMBERSHIP'
              ? '회원권'
              : item.salesType === 'REFUND'
                ? '환불'
                : item.salesType,
        staff: item.staffName || '-',
        customer: item.customerName || '-',
        item:
          item.salesType === 'ITEMS'
            ? item.secondaryItemName || '-'
            : item.salesType === 'MEMBERSHIP'
              ? item.prepaidPassName || item.sessionPassName || '-'
              : '-',
        salesTotal: item.retailPrice ?? '-',
        discount: item.discountAmount ?? '-',
        netSales: item.totalAmount ?? '-',
        paymentMethod: item.payments?.length
          ? item.payments.map(p => mapPaymentMethodToKorean(p.paymentsMethod)).join(', ')
          : '-',
        original: item,
      }));

      pagination.value.totalPages = result.pagination?.totalPages ?? 1;
      pagination.value.totalItems = result.pagination?.totalItems ?? 0;
    } catch (e) {
      console.error('매출 데이터 조회 실패:', e);
    } finally {
      loading.value = false;
    }
  };

  const handlePageChange = newPage => {
    pagination.value.currentPage = newPage;
    fetchSalesList();
  };

  watch(
    () => JSON.stringify({ keyword: props.customerKeyword, filters: props.filterState }),
    () => {
      pagination.value.currentPage = 1;
      fetchSalesList();
    },
    { immediate: true }
  );

  const openModal = item => {
    selectedSalesItem.value = item.original ?? item;
    detailModalVisible.value = true;
  };

  const closeModal = () => {
    detailModalVisible.value = false;
    selectedSalesItem.value = null;
  };

  const columns = [
    { key: 'date', title: '판매일시', width: '160px' },
    { key: 'type', title: '매출 유형', width: '100px' },
    { key: 'staff', title: '담당자', width: '100px' },
    { key: 'customer', title: '고객명', width: '100px' },
    { key: 'item', title: '판매목록', width: '150px' },
    { key: 'salesTotal', title: '총 영업액', width: '100px' },
    { key: 'discount', title: '할인액', width: '100px' },
    { key: 'netSales', title: '실매출액', width: '100px' },
    { key: 'paymentMethod', title: '결제수단', width: '120px' },
  ];

  defineExpose({ fetchSalesList });
</script>

<style>
  .base-table-wrapper {
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    padding: 24px;
    margin-bottom: 24px;
  }
</style>
