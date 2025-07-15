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
                    <label>할인율</label>
                    <BaseForm
                      v-model.number="item.discountRate"
                      type="select"
                      :options="discountRateOptions"
                      @update:model-value="() => recalculateItem(index)"
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
                  <label for="discountRate">할인율</label>
                  <BaseForm
                    id="discountRate"
                    v-model="globalDiscountRate"
                    type="select"
                    :options="discountRateOptions"
                    @change="applyGlobalDiscount"
                  />
                </div>
                <div class="items-discount-amount-group">
                  <label>할인금액</label>
                  <BaseForm type="number" :model-value="globalDiscountAmount" readonly />
                </div>
              </div>
            </div>

            <div v-for="method in methods" :key="method.key" class="items-method-row">
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
  import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import { getStaff } from '@/features/staffs/api/staffs.js';
  import { getAvailableSessionPasses } from '@/features/membership/api/membership.js';

  const toastRef = ref(null);
  const emit = defineEmits(['close', 'submit']);
  const staffOptions = ref([]);

  const props = defineProps({
    initialItems: Array,
    initialMemo: String,
    initialDate: String,
    initialTime: String,
    initialPayments: Array,
    initialDiscountRate: Number,
    initialCustomer: String,
    customerId: Number, // ✅ 정기권 조회용 customerId 전달
  });

  const selectedItems = ref([]);
  const date = ref(props.initialDate || new Date().toISOString().substring(0, 10));
  const time = ref(props.initialTime || new Date().toTimeString().substring(0, 5));
  const memo = ref(props.initialMemo || '');
  const globalDiscountRate = ref(props.initialDiscountRate || 0);
  const customer = ref(props.initialCustomer || '');

  const discountRates = [0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50];
  const discountRateOptions = discountRates.map(rate => ({ value: rate, text: `${rate}%` }));

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

  const updateSessionPasses = async () => {
    if (!props.customerId) return;

    let allSessionPasses = [];
    try {
      allSessionPasses = await getAvailableSessionPasses(props.customerId);
    } catch (e) {
      console.warn('[정기권 조회 실패]', e);
    }

    selectedItems.value = selectedItems.value.map(item => {
      const matchedPasses = allSessionPasses
        .filter(pass => pass.secondaryItemName === item.name)
        .map(pass => ({
          value: pass.customerSessionPassId,
          text: `${pass.sessionPassName} (${pass.remainingCount}회/${pass.totalCount}회, ${pass.expirationDate})`,
        }));

      return {
        ...item,
        availableSessionPasses: [
          { value: '', text: '횟수권 선택' },
          ...(matchedPasses.length ? matchedPasses : [{ value: '', text: '사용 불가' }]),
        ],
      };
    });
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

  const submit = () => {
    const payments = selectedMethods.value.map(key => ({
      method: key,
      amount: paymentAmounts.value[key] || 0,
    }));

    emit('submit', {
      date: date.value,
      time: time.value,
      items: selectedItems.value,
      memo: memo.value,
      payments,
      discountRate: globalDiscountRate.value,
      finalAmount: finalTotalPrice.value,
    });

    emit('close');
  };

  const formatPrice = val => (val || 0).toLocaleString('ko-KR') + '원';

  const handleKeydown = event => {
    if (event.key === 'Escape') emit('close');
  };

  onMounted(async () => {
    await fetchStaffs();
    selectedItems.value = (props.initialItems || []).map(item => ({
      name: item.item || '',
      price: item.salesTotal || 0,
      quantity: item.quantity || 1,
      discountRate: Number(item.discountRate) || 0,
      discountAmount: item.discount || 0,
      finalPrice: item.netSales || 0,
      manager: staffOptions.value.find(opt => opt.text === item.staff)?.value || '',
      deduction: '',
      couponCode: '',
      couponInfo: '',
      availableSessionPasses: [], // 초기화
    }));
    await updateSessionPasses();
  });

  onMounted(() => window.addEventListener('keydown', handleKeydown));
  onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown));
</script>
