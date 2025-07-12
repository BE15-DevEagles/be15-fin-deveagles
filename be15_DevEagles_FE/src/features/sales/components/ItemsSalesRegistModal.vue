<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="modal-panel">
      <div class="top-bar">
        <div class="info">
          <h2 class="title">상품 매출 등록</h2>
        </div>
        <div class="actions">
          <BaseButton class="close-button" @click="emit('close')">&times;</BaseButton>
        </div>
      </div>

      <div class="divider"></div>

      <div class="register-body">
        <!-- 좌측 -->
        <div class="form-left scrollable-left">
          <div class="datetime-row">
            <div class="date-time-group">
              <PrimeDatePicker v-model="date" class="date-picker" />
              <PrimeDatePicker
                v-model="time"
                time-only
                show-time
                hour-format="24"
                class="time-picker"
              />
            </div>
            <BaseButton class="select-button" @click="openProductModal">상품 선택</BaseButton>
          </div>

          <div
            class="receipt-box"
            :class="{ clickable: selectedProducts.length === 0 }"
            @click="selectedProducts.length === 0 && openProductModal()"
          >
            <template v-if="selectedProducts.length === 0">
              <p class="placeholder">영역을 클릭해<br />판매할 상품을 선택해주세요.</p>
            </template>
            <template v-else>
              <div
                v-for="(product, index) in selectedProducts"
                :key="index"
                class="selected-detail"
              >
                <div class="product-header">
                  <p>
                    <strong>{{ product.name }}</strong> ({{ formatPrice(product.price) }})
                  </p>
                  <BaseButton class="x-button" @click.stop="removeProduct(index)">×</BaseButton>
                </div>

                <div class="grid-container">
                  <div class="input-group">
                    <label>수량</label>
                    <BaseForm
                      v-model.number="product.quantity"
                      type="number"
                      min="1"
                      @input="recalculateProduct(index)"
                    />
                  </div>
                  <div class="input-group">
                    <label>정가</label>
                    <BaseForm
                      v-model.number="product.price"
                      type="number"
                      @input="recalculateProduct(index)"
                    />
                  </div>
                  <div class="input-group">
                    <label>정기권 차감</label>
                    <BaseForm
                      v-model="product.deduction"
                      type="select"
                      :options="deductionOptions"
                    />
                  </div>
                  <div class="input-group">
                    <label>쿠폰번호입력</label>
                    <BaseForm v-model="product.couponCode" type="text" />
                  </div>
                  <div class="input-group">
                    <label>쿠폰정보</label>
                    <BaseForm v-model="product.couponInfo" type="text" />
                  </div>
                  <div class="input-group">
                    <label>담당자</label>
                    <BaseForm v-model="product.manager" type="text" />
                  </div>
                  <div class="input-group">
                    <label>할인/추가</label>
                    <BaseForm
                      v-model.number="product.discountRate"
                      type="select"
                      :options="discountRateOptions"
                      @change="recalculateProduct(index)"
                    />
                  </div>
                  <div class="input-group">
                    <label>할인금액</label>
                    <BaseForm v-model.number="product.discountAmount" type="number" readonly />
                  </div>
                  <div class="input-group">
                    <label>최종금액</label>
                    <BaseForm v-model.number="product.finalPrice" type="number" readonly />
                  </div>
                </div>
              </div>
            </template>
          </div>

          <label class="memo-label">메모</label>
          <BaseForm v-model="memo" type="textarea" placeholder="메모 입력" />
        </div>

        <!-- 우측 -->
        <div class="form-right">
          <div class="search-row">
            <BaseForm v-model="customerSearch" type="text" placeholder="고객명 또는 연락처 검색" />
          </div>

          <div class="total-price-section">
            <label>최종 결제 금액</label>
            <div class="total-display">
              <div class="price-box">{{ formatPrice(finalTotalPrice) }}</div>
            </div>
          </div>

          <div class="discount-row">
            <div class="discount-rate-group">
              <label for="discountRate">할인율</label>
              <BaseForm
                id="discountRate"
                v-model="globalDiscountRate"
                type="select"
                :options="discountRateOptions"
                @change="applyGlobalDiscount"
              />
            </div>

            <div class="discount-amount-group">
              <label>할인금액</label>
              <BaseForm type="number" :model-value="globalDiscountAmount" readonly />
            </div>
          </div>

          <div v-for="method in methods" :key="method.key" class="method-group">
            <div class="checkbox-label">
              <BaseForm
                :id="`payment-${method.key}`"
                v-model="selectedMethods"
                type="checkbox"
                :options="[{ value: method.key, text: method.label }]"
              />
            </div>
            <BaseForm
              v-model.number="paymentAmounts[method.key]"
              type="number"
              class="method-price"
              placeholder="금액"
              :disabled="!selectedMethods.includes(method.key)"
            />
          </div>

          <div class="footer-buttons">
            <BaseButton class="cancel-button" outline @click="emit('close')">닫기</BaseButton>
            <BaseButton class="submit-button" @click="submit">등록</BaseButton>
          </div>
        </div>
      </div>
    </div>

    <ItemsSelectModal
      v-if="showProductModal"
      @close="showProductModal = false"
      @apply="applySelectedProducts"
    />

    <!-- ✅ Toast 컴포넌트 추가 -->
    <BaseToast ref="toastRef" />
  </div>
</template>

<script setup>
  import { ref, computed, watch, onMounted } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import ItemsSelectModal from '@/features/sales/components/ItemsSelectModal.vue';
  import BaseToast from '@/components/common/BaseToast.vue'; // ✅ 추가
  import '@/features/sales/styles/SalesItemsModal.css';

  const emit = defineEmits(['close', 'submit']);
  const toastRef = ref(null); // ✅ Toast ref 등록

  const date = ref(new Date().toISOString().substring(0, 10));
  const props = defineProps({
    selectedCustomer: { type: Object, default: null }, // 선택된 고객 정보
  });

  const time = ref(new Date().toTimeString().substring(0, 5));
  const selectedProducts = ref([]);
  const memo = ref('');
  const showProductModal = ref(false);

  const selectedMethods = ref([]);
  const paymentAmounts = ref({});

  const customerSearch = ref('');
  const discountRates = [0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50];
  const discountRateOptions = discountRates.map(rate => ({ value: rate, text: `${rate}%` }));

  const deductionOptions = [
    { value: '', text: '정기권 선택' },
    { value: '정기권 차감', text: '정기권 차감' },
    { value: '차감 없음', text: '차감 없음' },
  ];

  const globalDiscountRate = ref(0);

  const methods = ref([
    { key: 'prepaid', label: '선불권' },
    { key: 'card', label: '카드 결제' },
    { key: 'cash', label: '현금 결제' },
    { key: 'naver', label: '네이버페이' },
    { key: 'local', label: '지역화폐' },
  ]);

  const totalPrice = computed(() =>
    selectedProducts.value.reduce((sum, item) => sum + (item.finalPrice || 0), 0)
  );

  const globalDiscountAmount = computed(() => {
    const raw = totalPrice.value;
    const rate = globalDiscountRate.value;
    return Math.floor((raw * rate) / 100);
  });

  const finalTotalPrice = computed(() => {
    return totalPrice.value - globalDiscountAmount.value;
  });

  const openProductModal = () => {
    showProductModal.value = true;
  };

  const applySelectedProducts = products => {
    selectedProducts.value = products.map(p => {
      const price = p.price || 0;
      const quantity = 1;
      const rate = 0;
      const totalPrice = price * quantity;
      const discountAmount = Math.floor((totalPrice * rate) / 100);
      const finalPrice = totalPrice - discountAmount;

      return {
        ...p,
        quantity,
        deduction: '',
        manager: '',
        discountRate: rate,
        discountAmount,
        finalPrice,
        couponCode: '',
        couponInfo: '',
      };
    });
  };

  const applyGlobalDiscount = () => {
    // 전역 할인율은 총합에만 적용됨
  };

  const recalculateProduct = index => {
    const product = selectedProducts.value[index];
    const unitPrice = product.price || 0;
    const quantity = product.quantity || 1;
    const rate = product.discountRate || 0;
    const totalPrice = unitPrice * quantity;
    const discountAmount = Math.floor((totalPrice * rate) / 100);

    product.discountAmount = discountAmount;
    product.finalPrice = totalPrice - discountAmount;
  };

  const removeProduct = index => {
    selectedProducts.value.splice(index, 1);
  };

  const submit = () => {
    const payments = selectedMethods.value.map(key => ({
      method: key,
      amount: paymentAmounts.value[key] || 0,
    }));

    emit('submit', {
      date: date.value,
      time: time.value,
      products: selectedProducts.value,
      memo: memo.value,
      payments,
      discountRate: globalDiscountRate.value,
      finalAmount: finalTotalPrice.value,
    });

    // ✅ 토스트 메시지 출력
    toastRef.value?.success('상품 매출이 등록되었습니다.');

    emit('close');
  };

  const formatPrice = val => val?.toLocaleString('ko-KR') + '원';
</script>

  // 선택된 고객 정보가 있으면 검색 필드에 자동 입력
  watch(() => props.selectedCustomer, (newCustomer) => {
    if (newCustomer) {
      customerSearch.value = `${newCustomer.customerName} (${newCustomer.phoneNumber})`;
    }
  }, { immediate: true });
