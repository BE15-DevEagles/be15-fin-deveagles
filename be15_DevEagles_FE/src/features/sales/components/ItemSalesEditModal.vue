<template>
  <div class="items-overlay" @click.self="emit('close')">
    <div class="items-modal-panel">
      <div class="items-top-bar">
        <div class="info"><h2 class="items-title">상품 매출 수정</h2></div>
        <div class="actions">
          <button class="items-close-button" @click="emit('close')">&times;</button>
        </div>
      </div>

      <div class="items-divider"></div>

      <div class="items-register-body">
        <!-- 좌측 -->
        <div class="items-form-left">
          <div class="items-datetime-row">
            <div class="items-date-time-group">
              <PrimeDatePicker v-model="date" class="date-picker" />
              <PrimeDatePicker
                v-model="time"
                time-only
                show-time
                hour-format="24"
                class="time-picker"
              />
            </div>
          </div>

          <div class="items-box">
            <template v-if="selectedItems.length === 0">
              <p class="items-placeholder">수정할 상품이 없습니다.</p>
            </template>
            <template v-else>
              <div
                v-for="(item, index) in selectedItems"
                :key="index"
                class="items-selected-detail"
              >
                <div class="items-product-header">
                  <p>
                    <strong>{{ item.name }}</strong> ({{ formatPrice(item.price) }})
                  </p>
                </div>

                <div class="items-grid-container">
                  <div class="items-input-group">
                    <label>수량</label>
                    <BaseForm
                      v-model.number="item.quantity"
                      type="number"
                      min="1"
                      @input="recalculateItem(index)"
                    />
                  </div>
                  <div class="items-input-group">
                    <label>정가</label>
                    <BaseForm
                      v-model.number="item.price"
                      type="number"
                      @input="recalculateItem(index)"
                    />
                  </div>
                  <div class="items-input-group">
                    <label>횟수권 차감</label>
                    <BaseForm
                      v-model="item.deduction"
                      type="select"
                      :options="item.availableSessionPasses"
                      @update:model-value="() => recalculateItem(index)"
                    />
                  </div>
                  <div class="items-input-group">
                    <label>쿠폰번호입력</label>
                    <BaseForm v-model="item.couponCode" type="text" />
                  </div>
                  <div class="items-input-group">
                    <label>쿠폰정보</label>
                    <BaseForm v-model="item.couponInfo" type="text" />
                  </div>
                  <div class="mem-input-group">
                    <label>담당자</label>
                    <BaseForm
                      v-model="item.manager"
                      type="select"
                      :options="staffOptions"
                      placeholder="담당자 선택"
                    />
                  </div>
                  <div class="items-input-group">
                    <label>할인율(%)</label>
                    <BaseForm
                      v-model.number="item.discountRate"
                      type="number"
                      min="0"
                      max="100"
                      step="1"
                      placeholder="할인율 입력"
                      @input="recalculateItem(index)"
                    />
                  </div>
                  <div class="items-input-group">
                    <label>할인금액</label>
                    <BaseForm v-model.number="item.discountAmount" type="number" readonly />
                  </div>
                  <div class="items-input-group">
                    <label>최종금액</label>
                    <BaseForm v-model.number="item.finalPrice" type="number" readonly />
                  </div>
                </div>
              </div>
            </template>
          </div>

          <label class="items-memo-label">메모</label>
          <BaseForm v-model="memo" type="textarea" placeholder="메모 입력" class="items-textarea" />
        </div>

        <!-- 우측 -->
        <div class="items-form-right">
          <div class="items-form-right-body">
            <div class="search-row">
              <BaseForm v-model="customer" type="text" readonly />
            </div>

            <div class="items-total-price-section">
              <label>최종 결제 금액</label>
              <div class="items-total-display">
                <div class="items-price-box">{{ formatPrice(finalTotalPrice) }}</div>
              </div>
            </div>

            <div class="items-discount-row">
              <div class="items-discount-group">
                <div class="items-discount-rate-group">
                  <label for="discountRate">할인율(%)</label>
                  <BaseForm
                    id="discountRate"
                    v-model.number="globalDiscountRate"
                    type="number"
                    min="0"
                    max="100"
                    step="1"
                    placeholder="할인율 입력"
                  />
                </div>
                <div class="items-discount-amount-group">
                  <label>할인금액</label>
                  <BaseForm type="number" :model-value="globalDiscountAmount" readonly />
                </div>
              </div>
            </div>

            <div v-for="method in methods" :key="method.key" class="items-method-row">
              <template v-if="method.key !== 'prepaid'">
                <div class="items-method-inline">
                  <BaseForm
                    v-model="selectedMethods"
                    type="checkbox"
                    :options="[{ value: method.key, text: method.label }]"
                  />
                  <BaseForm
                    v-model.number="paymentAmounts[method.key]"
                    type="number"
                    class="items-method-price"
                    placeholder="금액"
                    :disabled="!selectedMethods.includes(method.key)"
                    :class="{ disabled: !selectedMethods.includes(method.key) }"
                  />
                </div>
              </template>
              <template v-else>
                <div class="items-method-group">
                  <div class="items-method-inline">
                    <div class="items-payment-label">
                      <BaseForm
                        v-model="selectedMethods"
                        type="checkbox"
                        :options="[{ value: method.key, text: '선불권' }]"
                      />
                    </div>
                    <div class="items-prepaid-inline">
                      <div class="items-input-group">
                        <label>사용할 선불권</label>
                        <BaseForm
                          v-model="selectedPrepaidPassId"
                          type="select"
                          :options="customerPrepaidPassOptions"
                          :disabled="!selectedMethods.includes(method.key)"
                        />
                      </div>
                      <div class="items-input-group">
                        <label>사용할 금액</label>
                        <BaseForm
                          v-model.number="paymentAmounts[method.key]"
                          type="number"
                          placeholder="금액 입력"
                          :disabled="!selectedMethods.includes(method.key)"
                          class="items-method-price"
                          :class="{ disabled: !selectedMethods.includes(method.key) }"
                          :max="prepaidTotalAmount"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>

          <div class="items-form-right-footer">
            <div class="items-footer-buttons">
              <BaseButton class="cancel-button" outline @click="emit('close')">닫기</BaseButton>
              <BaseButton class="submit-button" @click="submit">수정</BaseButton>
            </div>
          </div>
        </div>
      </div>
    </div>

    <BaseToast ref="toastRef" />
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import { getStaff } from '@/features/staffs/api/staffs.js';
  import {
    getAvailableSessionPasses,
    getCustomerPrepaidPasses,
  } from '@/features/membership/api/membership.js';
  import { updateItemSales } from '@/features/sales/api/sales.js';
  import { useAuthStore } from '@/store/auth';
  const authStore = useAuthStore();

  const toastRef = ref(null);
  const emit = defineEmits(['close', 'submit']);
  const staffOptions = ref([]);

  const props = defineProps({
    initialData: {
      type: Object,
      default: () => ({}),
    },
  });

  const customerId = computed(() => props.initialData?.customerId);
  const initialSalesId = computed(
    () => props.initialData?.initialSalesId || props.initialData?.salesId
  );

  const selectedItems = ref([]);
  const date = ref(new Date().toISOString().substring(0, 10));
  const time = ref(new Date().toTimeString().substring(0, 5));
  const memo = ref('');
  const globalDiscountRate = ref(0);
  const customer = ref('');
  const selectedPrepaidPassId = ref(null);
  const customerPrepaidPassOptions = ref([]);
  const prepaidTotalAmount = ref(0);

  const PaymentsMethodEnum = {
    card: 'CARD',
    cash: 'CASH',
    naver: 'NAVER_PAY',
    local: 'LOCAL',
    prepaid: 'PREPAID_PASS',
    prepaid_pass: 'PREPAID_PASS',
    session_pass: 'SESSION_PASS',
  };

  const methods = ref([
    { key: 'prepaid', label: '선불권' },
    { key: 'card', label: '카드 결제' },
    { key: 'cash', label: '현금 결제' },
    { key: 'naver', label: '네이버페이' },
    { key: 'local', label: '지역화폐' },
  ]);

  const selectedMethods = ref(
    props.initialPayments?.map(p => p.paymentsMethod?.toLowerCase()) || []
  );

  const paymentAmounts = ref(
    Object.fromEntries(
      (props.initialPayments || []).map(p => [p.paymentsMethod?.toLowerCase(), p.amount])
    )
  );

  const fetchStaffs = async () => {
    try {
      const response = await getStaff({ page: 1, size: 100, isActive: true });
      staffOptions.value = response.data.data.staffList.map(staff => ({
        value: staff.staffId,
        text: staff.staffName,
      }));
    } catch (e) {
      console.error('직원 목록 불러오기 실패:', e);
    }
  };

  const fetchSessionPassesForEdit = async () => {
    if (!customerId.value) return;
    let allSessionPasses = [];
    try {
      allSessionPasses = await getAvailableSessionPasses(customerId.value);
    } catch (e) {
      console.warn('[ItemSalesEditModal] session pass 조회 실패', e);
      return;
    }
    for (const item of selectedItems.value) {
      const filteredPasses = allSessionPasses
        .filter(pass => String(pass.secondaryItemId) === String(item.id))
        .map(pass => ({
          value: pass.customerSessionPassId,
          text: `${pass.sessionPassName} (${pass.remainingCount}회/${pass.totalCount}회, ${pass.expirationDate})`,
        }));
      item.availableSessionPasses = [
        { value: '', text: '횟수권 선택' },
        ...(filteredPasses.length ? filteredPasses : [{ value: '', text: '해당 상품 사용 불가' }]),
      ];
    }
  };

  const fetchPrepaidPassesForEdit = async () => {
    if (!customerId.value) return;

    try {
      const list = await getCustomerPrepaidPasses(customerId.value);

      if (list.length === 0) {
        customerPrepaidPassOptions.value = [{ value: '', text: '사용 가능한 선불권 없음' }];
        selectedPrepaidPassId.value = '';
        prepaidTotalAmount.value = 0;
        return;
      }

      prepaidTotalAmount.value = list.reduce((sum, item) => sum + (item.remainingAmount || 0), 0);
      customerPrepaidPassOptions.value = list.map(pass => ({
        value: pass.customerPrepaidPassId,
        text: `${pass.prepaidPassName} (${pass.remainingAmount.toLocaleString()}원, ${pass.expirationDate})`,
      }));

      if (list.length === 1) {
        selectedPrepaidPassId.value = list[0].customerPrepaidPassId;
      }
    } catch (e) {
      console.warn('[ItemSalesEditModal] 선불권 조회 실패', e);
      customerPrepaidPassOptions.value = [{ value: '', text: '선불권 조회 실패' }];
      selectedPrepaidPassId.value = '';
      prepaidTotalAmount.value = 0;
    }
  };

  const recalculateItem = index => {
    const item = selectedItems.value[index];
    const total = (item.price || 0) * (item.quantity || 1);
    const discount = Math.floor((total * (item.discountRate || 0)) / 100);
    item.discountAmount = discount;
    item.finalPrice = total - discount;
  };

  const totalPrice = computed(() =>
    selectedItems.value.reduce((sum, item) => sum + (item.finalPrice || 0), 0)
  );

  const globalDiscountAmount = computed(() =>
    Math.floor((totalPrice.value * globalDiscountRate.value) / 100)
  );

  const finalTotalPrice = computed(() => totalPrice.value - globalDiscountAmount.value);

  const submit = async () => {
    if (selectedMethods.value.includes('prepaid') && !selectedPrepaidPassId.value) {
      toastRef.value?.error('사용할 선불권을 선택해주세요.');
      return;
    }

    const item = selectedItems.value[0];
    const dateOnly = date.value.split(' ')[0]; // "2025-07-21"
    const timeOnly = time.value.includes(':') ? time.value : `${time.value}:00`; // "11:27:00"
    const salesDateStr = `${dateOnly}T${timeOnly}`;

    const payments = [];

    if (item.deduction) {
      payments.push({
        paymentsMethod: PaymentsMethodEnum['session_pass'],
        amount: item.finalPrice || item.price * item.quantity,
        customerSessionPassId: item.deduction,
        usedCount: item.quantity || 1,
      });
    }

    selectedMethods.value.forEach(method => {
      if (method === 'session_pass') return;

      const paymentsMethod = PaymentsMethodEnum[method];
      if (!paymentsMethod) {
        console.warn(`❌ 잘못된 결제 수단: ${method}`);
        return;
      }

      if (method === 'prepaid' || method === 'prepaid_pass') {
        return;
      }

      payments.push({
        paymentsMethod,
        amount: paymentAmounts.value[method] || 0,
      });
    });

    if (selectedMethods.value.includes('prepaid')) {
      payments.push({
        paymentsMethod: PaymentsMethodEnum['prepaid'],
        amount: paymentAmounts.value['prepaid'] || 0,
        customerPrepaidPassId: selectedPrepaidPassId.value,
      });
    }

    const payload = {
      customerId: Number(customerId.value) || 0,
      staffId: Number(item.manager) || null,
      shopId: Number(authStore.shopId) || 0,
      discountRate: Number(globalDiscountRate.value) || 0,
      retailPrice: selectedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0),
      discountAmount: selectedItems.value.reduce(
        (sum, item) => sum + (item.discountAmount || 0),
        0
      ),
      totalAmount: selectedItems.value.reduce((sum, item) => sum + (item.finalPrice || 0), 0),
      salesMemo: memo.value || '',
      salesDate: salesDateStr,
      payments: payments.filter(p => p.amount > 0),
      items: selectedItems.value.map(item => ({
        secondaryItemId: Number(item.id) || 0,
        quantity: Number(item.quantity) || 1,
        discountRate: Number(item.discountRate) || 0,
        couponId: item.couponId || null,
      })),
    };
    try {
      await updateItemSales(initialSalesId.value, payload);
      alert('상품매출이 수정되었습니다.');
      emit('submit');
      emit('close');
      location.reload();
    } catch (e) {
      console.error('상품 매출 수정 실패:', e);
      console.error('Error response:', e.response?.data);
      const errorMessage = e.response?.data?.message || '상품 매출 수정에 실패했습니다.';
      toastRef.value?.error(errorMessage);
    }
  };

  const formatPrice = val => (val || 0).toLocaleString('ko-KR') + '원';

  const handleKeydown = event => {
    if (event.key === 'Escape') emit('close');
  };

  onMounted(async () => {
    window.addEventListener('keydown', handleKeydown);
    await fetchStaffs();

    if (!props.initialData) {
      toastRef.value?.error('초기 데이터가 없습니다.');
      return;
    }

    // 기본값 세팅
    customer.value = props.initialData.customerName || '';

    if (props.initialData.salesDate) {
      if (props.initialData.salesDate.includes('T')) {
        date.value = props.initialData.salesDate.split('T')[0];
      } else {
        date.value = props.initialData.salesDate;
      }
    } else {
      date.value = new Date().toISOString().substring(0, 10);
    }

    time.value = props.initialData.salesTime || new Date().toTimeString().substring(0, 5);
    memo.value = props.initialData.salesMemo || '';
    globalDiscountRate.value = props.initialData.salesDiscountRate || 0;

    const paymentMethodMap = {
      CARD: 'card',
      CASH: 'cash',
      NAVER_PAY: 'naver',
      LOCAL: 'local',
      PREPAID_PASS: 'prepaid',
      SESSION_PASS: 'session_pass',
    };

    selectedMethods.value = (props.initialData.payments || [])
      .map(p => paymentMethodMap[p.paymentsMethod] || p.paymentsMethod?.toLowerCase())
      .filter(Boolean);

    paymentAmounts.value = Object.fromEntries(
      (props.initialData.payments || [])
        .map(p => [paymentMethodMap[p.paymentsMethod] || p.paymentsMethod?.toLowerCase(), p.amount])
        .filter(([key]) => key)
    );

    if (props.initialData.customerPrepaidPassId) {
      selectedPrepaidPassId.value = props.initialData.customerPrepaidPassId;
    }

    // 상품 항목 세팅
    selectedItems.value = (props.initialData.items || []).map(item => {
      const price = Number(item.secondaryItemPrice) || 0;
      const quantity = Number(item.quantity) || 1;
      const discountRate = Number(item.itemDiscountRate) || 0;
      const discountAmount = Math.floor((price * discountRate) / 100) * quantity;
      const finalPrice = price * quantity - discountAmount;

      return {
        itemSalesId: item.itemSalesId,
        id: Number(item.secondaryItemId) || 0,
        name: item.secondaryItemName || '알 수 없음',
        price,
        quantity,
        discountRate,
        discountAmount,
        finalPrice,
        manager:
          staffOptions.value.find(opt => opt.text === props.initialData.staffName)?.value || '',
        deduction: props.initialData.customerSessionPassId || '',
        couponCode: '',
        couponInfo: '',
        availableSessionPasses: [],
      };
    });

    await fetchSessionPassesForEdit();
    await fetchPrepaidPassesForEdit();
  });

  watch(selectedMethods, newVal => {
    for (const key in paymentAmounts.value) {
      if (!newVal.includes(key)) {
        paymentAmounts.value[key] = 0;
      }
    }
    if (newVal.length === 1) {
      const selected = newVal[0];
      paymentAmounts.value[selected] = finalTotalPrice.value;
    }
  });

  onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown));
</script>
