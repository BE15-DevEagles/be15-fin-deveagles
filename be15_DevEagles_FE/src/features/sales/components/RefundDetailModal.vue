<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="modal-panel">
      <!-- 상단 헤더 -->
      <div class="top-bar">
        <div class="info">
          <h2 class="title">환불 내역 상세 조회</h2>
        </div>
        <div class="actions">
          <BaseButton class="close-button" @click="emit('close')">&times;</BaseButton>
        </div>
      </div>

      <div class="divider"></div>

      <!-- 양쪽 레이아웃 -->
      <div class="register-body">
        <!-- 좌측: 환불 내역 -->
        <div class="form-left scrollable-left">
          <div class="receipt-box">
            <div class="receipt-header">
              <h3 class="receipt-title">환불 내역서</h3>
              <p class="receipt-sub">{{ date }} {{ time }}</p>
            </div>

            <div v-for="(item, index) in selectedMemberships" :key="index" class="sales-row">
              <div class="row-left">
                <strong>{{ item.item }}</strong> / 1개 / {{ item.staff || '담당자 없음' }}
              </div>
              <div class="row-right">
                {{ formatPrice(item.salesTotal) }}
              </div>
            </div>

            <div class="receipt-row">
              <span>총 환불액</span>
              <span>{{ formatPrice(totalAmount) }}</span>
            </div>
            <div class="receipt-row">
              <span>[환불] {{ paymentLabel }}</span>
              <span>{{ formatPrice(totalAmount) }}</span>
            </div>
            <div class="receipt-row">
              <span>메모</span>
              <span>{{ memo || '비고 없음' }}</span>
            </div>
          </div>
        </div>

        <!-- 우측: 고객명 표시 및 닫기 버튼 -->
        <div class="form-right">
          <!-- 본문 -->
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

          <!-- 푸터 -->
          <div class="form-right-footer">
            <div class="footer-buttons">
              <BaseButton class="primary" outline @click="emit('close')">닫기</BaseButton>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import '@/features/sales/styles/SalesDetailModal.css';

  const props = defineProps({ salesItem: Object });
  const emit = defineEmits(['close']);

  const date = ref('');
  const time = ref('');
  const memo = ref('');
  const selectedMethod = ref('card');
  const selectedMemberships = ref([]);

  // 결제수단 → 코드 키로 매핑
  const mapMethodToKey = label => {
    const map = {
      카드: 'card',
      현금: 'cash',
      네이버페이: 'naver_pay',
      지역화폐: 'local',
      계좌이체: 'transfer',
    };
    return map[label] || 'card';
  };

  // 결제수단 라벨 출력용
  const paymentLabel = computed(() => {
    const method = {
      card: '카드 결제',
      cash: '현금 결제',
      naver_pay: '네이버페이',
      local: '지역화폐',
      transfer: '계좌이체',
    };
    return method[selectedMethod.value] || '결제수단';
  });

  // 금액 포맷팅
  const formatPrice = val => (val || 0).toLocaleString('ko-KR') + '원';

  // 총 환불액 계산
  const totalAmount = computed(() =>
    selectedMemberships.value.reduce((sum, m) => sum + (m.netSales || 0), 0)
  );

  // Escape 키로 닫기
  const handleKeydown = event => {
    if (event.key === 'Escape') {
      emit('close');
    }
  };

  onMounted(() => {
    window.addEventListener('keydown', handleKeydown);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown);
  });

  // salesItem 변경 감지
  watch(
    () => props.salesItem,
    val => {
      if (!val) return;

      // 날짜/시간 가공
      const dateParts = val.salesDate?.replace('T', ' ')?.split(' ') || [];
      date.value = dateParts[0] ?? '';
      time.value = dateParts[1] ?? '';

      // 메모: null 허용 시 fallback
      memo.value = val.salesMemo ?? '';

      // 결제 수단: 첫 번째 결제 방식 기준
      const paymentMethod = val.payments?.[0]?.paymentsMethod?.toLowerCase() || 'card';
      selectedMethod.value = paymentMethod;

      // selectedMemberships 데이터 구성
      selectedMemberships.value =
        Array.isArray(val.items) && val.items.length
          ? val.items.map(item => ({
              item: item.secondaryItemName ?? '알 수 없음',
              staff: val.staffName,
              salesTotal: Number(item.secondaryItemPrice) || 0,
              netSales: Number(item.secondaryItemPrice) || 0, // 환불액 = 원가 기준
              customer: val.customerName,
            }))
          : [
              {
                item: val.prepaidPassName ?? val.sessionPassName ?? '알 수 없음',
                staff: val.staffName,
                salesTotal: val.retailPrice ?? 0,
                netSales: val.totalAmount ?? 0,
                customer: val.customerName,
              },
            ];
    },
    { immediate: true }
  );
</script>
