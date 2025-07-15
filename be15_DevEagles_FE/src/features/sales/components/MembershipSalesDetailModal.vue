<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="modal-panel">
      <!-- 상단 헤더 -->
      <div class="top-bar">
        <div class="info">
          <h2 class="title">회원권 매출 상세 조회</h2>
        </div>
        <div class="actions">
          <Button class="close-button" @click="emit('close')">&times;</Button>
        </div>
      </div>

      <div class="divider"></div>

      <!-- 양쪽 레이아웃 -->
      <div class="register-body">
        <!-- 좌측 -->
        <div class="form-left scrollable-left">
          <div class="receipt-box">
            <div class="receipt-header">
              <h3 class="receipt-title">결제 내역서</h3>
              <p class="receipt-sub">{{ date }} {{ time }}</p>
            </div>

            <div v-for="(item, index) in selectedMemberships" :key="index" class="sales-row">
              <div class="row-left">
                <strong>{{ item.item }}</strong> / 1개 / {{ item.staff || '담당자 없음' }}
              </div>
              <div class="row-right">
                <strong>{{ formatPrice(props.salesItem.totalAmount) }}</strong>
              </div>
            </div>

            <div class="receipt-row">
              <span>총 영업액</span>
              <span>{{ formatPrice(totalAmount) }}</span>
            </div>
            <div v-for="(pay, idx) in props.salesItem.payments" :key="idx" class="receipt-row">
              <span
                >[결제]
                {{
                  methodLabelMap[pay.paymentsMethod?.toLowerCase()] || `기타(${pay.paymentsMethod})`
                }}</span
              >
              <span>{{ formatPrice(pay.amount) }}</span>
            </div>
            <div class="receipt-row">
              <span>메모</span>
              <span>{{ memo || '비고 없음' }}</span>
            </div>
          </div>
        </div>

        <!-- 우측 -->
        <div class="form-right">
          <!-- 본문 영역 -->
          <div class="form-right-body">
            <div class="search-row">
              <BaseForm
                type="text"
                :model-value="selectedMemberships[0]?.customer || ''"
                label="고객명"
                readonly
              />
            </div>
          </div>

          <!-- 하단 버튼 영역 -->
          <div class="form-right-footer">
            <div class="footer-buttons">
              <BaseButton class="primary" outline @click="emit('close')">닫기</BaseButton>

              <div ref="dropdownRef" class="dropdown-wrapper">
                <BaseButton class="primary" @click="toggleDropdown">매출 수정</BaseButton>
                <div v-if="dropdownVisible" class="sales-dropdown-menu">
                  <BaseButton class="primary" outline @click="handleAction('refund')">
                    매출 환불
                  </BaseButton>
                  <BaseButton class="primary" outline @click="openEditModal">매출 수정</BaseButton>
                  <BaseButton class="primary" outline @click="handleAction('delete')">
                    매출 삭제
                  </BaseButton>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 연결된 모달 -->
      <MembershipSalesDeleteModal
        v-model="showDeleteModal"
        title="매출 삭제"
        animation-class="back-in-left"
        :sales-id="selectedSalesId"
        @confirm="handleDeleteConfirm"
      />
      <MembershipSalesRefundModal
        v-model="showRefundModal"
        title="매출 환불"
        animation-class="back-in-left"
        :sales-id="selectedSalesId"
        @confirm="handleRefundConfirm"
      />
      <MembershipSalesEditModal
        v-if="showEditModal"
        :initial-membership="{
          id: props.salesItem.prepaidPassId ?? props.salesItem.sessionPassId,
          prepaidPassId: props.salesItem.prepaidPassId,
          sessionPassId: props.salesItem.sessionPassId,
          prepaidPassName: props.salesItem.prepaidPassName,
          sessionPassName: props.salesItem.sessionPassName,
          type: props.salesItem.prepaidPassName ? 'PREPAID' : 'SESSION',
          name: props.salesItem.prepaidPassName ?? props.salesItem.sessionPassName,
          retailPrice: props.salesItem.retailPrice,
          discountAmount: props.salesItem.discountAmount,
          totalAmount: props.salesItem.totalAmount,
          chargeAmount: membershipDetail.value?.chargeAmount ?? 0,
          remainAmount: membershipDetail.value?.remainAmount ?? 0,
          totalCount: props.salesItem.totalCount,
          remainCount: props.salesItem.remainCount,
          expireDate: membershipDetail.value?.expirationDate ?? null,
          benefit: membershipDetail.value?.benefit ?? '',
          manager: props.salesItem.staffId,
          customerName: props.salesItem.customerName,
          quantity: 1,
        }"
        :initial-memo="memo"
        :initial-date="date"
        :initial-time="time"
        :initial-payments="props.salesItem.payments"
        @close="handleEditCancel"
        @submit="handleEditConfirm"
      />

      <!-- ✅ Toast 추가 -->
      <BaseToast ref="toastRef" />
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import MembershipSalesDeleteModal from '@/features/sales/components/MembershipSalesDeleteModal.vue';
  import MembershipSalesRefundModal from '@/features/sales/components/MembershipSalesRefundModal.vue';
  import MembershipSalesEditModal from '@/features/sales/components/MembershipSalesEditModal.vue';
  import '@/features/sales/styles/SalesDetailModal.css';
  import {
    getPrepaidPassSalesDetail,
    getSessionPassSalesDetail,
  } from '@/features/sales/api/sales.js';

  const membershipDetail = ref(null);

  onMounted(async () => {
    const isPrepaid = !!props.salesItem.prepaidPassSalesId;

    try {
      let detail = null;

      if (isPrepaid) {
        const { data } = await getPrepaidPassSalesDetail(props.salesItem.prepaidPassSalesId);
        detail = data.data;
      } else {
        const { data } = await getSessionPassSalesDetail(props.salesItem.sessionPassSalesId);
        detail = data.data;
      }

      membershipDetail.value = {
        chargeAmount: detail?.retailPrice ?? 0,
        remainAmount: detail?.remainingAmount ?? 0, // customer_prepaid_pass에서
        expirationDate: detail?.expirationDate ?? null,
        benefit: detail?.bonus ? `${detail.bonus.toLocaleString()}원 보너스` : '',
      };
    } catch (err) {
      console.error('회원권 상세 조회 실패:', err);
    }
  });

  const props = defineProps({ salesItem: Object });
  const emit = defineEmits(['close']);
  const selectedSalesId = ref(null);
  const dropdownVisible = ref(false);
  const showDeleteModal = ref(false);
  const showRefundModal = ref(false);
  const showEditModal = ref(false);
  const toastRef = ref(null);

  const date = ref('');
  const time = ref('');
  const memo = ref('');
  const selectedMethod = ref('card');
  const selectedMemberships = ref([]);

  const dropdownRef = ref(null);

  const methodLabelMap = {
    card: '카드',
    cash: '현금',
    naver_pay: '네이버페이',
    local: '지역화폐',
    transfer: '계좌이체',
  };

  const mapMethodToKey = label => {
    const map = {
      카드: 'card',
      현금: 'cash',
      네이버페이: 'naver_pay',
      지역화폐: 'local',
    };
    return map[label] || 'card';
  };

  watch(
    () => props.salesItem,
    val => {
      if (!val) return;

      const dateParts =
        typeof val.date === 'string'
          ? val.date.split(' ')
          : (val.salesDate || '').replace('T', ' ').split(' ');

      date.value = dateParts[0] ?? '';
      time.value = dateParts[1] ?? '';

      memo.value = val.memo ?? '';
      selectedMethod.value = mapMethodToKey(val.payments?.[0]?.paymentsMethod ?? '');

      selectedMemberships.value = [
        {
          item: val.prepaidPassName ?? val.sessionPassName ?? val.secondaryItemName ?? '알 수 없음',
          staff: val.staffName,
          salesTotal: val.retailPrice,
          discount: val.discountAmount,
          discountRate: val.discountRate ?? val.salesDiscountRate,
          netSales: val.totalAmount,
          customer: val.customerName,
        },
      ];
    },
    { immediate: true }
  );

  onMounted(() => {
    window.addEventListener('keydown', handleKeydown);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown);
  });

  const handleKeydown = event => {
    if (event.key === 'Escape') {
      emit('close');
    }
  };

  const totalAmount = computed(() =>
    selectedMemberships.value.reduce((sum, m) => sum + (m.netSales || 0), 0)
  );

  const toggleDropdown = () => {
    dropdownVisible.value = !dropdownVisible.value;
  };

  const handleClickOutside = e => {
    if (dropdownRef.value && !dropdownRef.value.contains(e.target)) {
      dropdownVisible.value = false;
    }
  };

  onMounted(() => {
    document.addEventListener('click', handleClickOutside);
  });

  onBeforeUnmount(() => {
    document.removeEventListener('click', handleClickOutside);
  });

  const handleAction = type => {
    dropdownVisible.value = false;
    selectedSalesId.value = props.salesItem.salesId;
    switch (type) {
      case 'delete':
        showDeleteModal.value = true;
        break;
      case 'refund':
        showRefundModal.value = true;
        break;
    }
  };

  const openEditModal = () => {
    showEditModal.value = true;
  };

  const handleEditConfirm = () => {
    toastRef.value?.success('회원권 매출이 수정되었습니다.');
    showEditModal.value = false;
  };

  const handleDeleteConfirm = () => {
    toastRef.value?.success('회원권 매출이 삭제되었습니다.');
    showDeleteModal.value = false;
  };

  const handleRefundConfirm = () => {
    toastRef.value?.success('회원권 매출이 환불되었습니다.');
    showRefundModal.value = false;
  };

  const handleEditCancel = () => {
    showEditModal.value = false;
  };
  const formatPrice = val => (val || 0).toLocaleString('ko-KR') + '원';
</script>
