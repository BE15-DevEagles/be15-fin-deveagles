<template>
  <div class="items-overlay" @click.self="emit('close')">
    <div class="items-modal-panel">
      <div class="items-top-bar">
        <div class="info"><h2 class="items-title">상품 매출 등록</h2></div>
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
            <BaseButton class="select-button" @click="openProductModal">상품 선택</BaseButton>
          </div>

          <div
            class="items-box"
            :class="{ clickable: selectedProducts.length === 0 }"
            @click="selectedProducts.length === 0 && openProductModal()"
          >
            <template v-if="selectedProducts.length === 0">
              <p class="items-placeholder">영역을 클릭해<br />판매할 상품을 선택해주세요.</p>
            </template>
            <template v-else>
              <div
                v-for="(product, index) in selectedProducts"
                :key="index"
                class="items-selected-detail"
              >
                <div class="items-product-header">
                  <p>
                    <strong>{{ product.name }}</strong> ({{ formatPrice(product.price) }})
                  </p>
                  <Button class="items-x-button" @click.stop="removeProduct(index)">×</Button>
                </div>

                <div class="items-grid-container">
                  <div class="items-input-group">
                    <label>수량</label>
                    <BaseForm
                      v-model.number="product.quantity"
                      type="number"
                      min="1"
                      @input="recalculateProduct(index)"
                    />
                  </div>
                  <div class="items-input-group">
                    <label>정가</label>
                    <BaseForm
                      v-model.number="product.price"
                      type="number"
                      @input="recalculateProduct(index)"
                    />
                  </div>
                  <div class="items-input-group">
                    <label>횟수권 차감</label>
                    <BaseForm
                      v-model="product.deduction"
                      type="select"
                      :options="product.availableSessionPasses"
                      @update:model-value="() => recalculateProduct(index)"
                    />
                  </div>
                  <div class="items-input-group">
                    <label>쿠폰번호입력</label>
                    <BaseForm v-model="product.couponCode" type="text" />
                  </div>
                  <div class="items-input-group">
                    <label>쿠폰정보</label>
                    <BaseForm v-model="product.couponInfo" type="text" />
                  </div>
                  <div class="mem-input-group">
                    <label>담당자</label>
                    <BaseForm
                      v-model.number="product.manager"
                      type="select"
                      :options="staffOptions"
                      placeholder="담당자 선택"
                    />
                  </div>
                  <div class="items-input-group">
                    <label>할인/추가</label>
                    <BaseForm
                      v-model.number="product.discountRate"
                      type="select"
                      :options="discountRateOptions"
                      @update:model-value="() => recalculateProduct(index)"
                    />
                  </div>
                  <div class="items-input-group">
                    <label>할인금액</label>
                    <BaseForm v-model.number="product.discountAmount" type="number" readonly />
                  </div>
                  <div class="items-input-group">
                    <label>최종금액</label>
                    <BaseForm v-model.number="product.finalPrice" type="number" readonly />
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
            <div class="search-row" style="position: relative">
              <BaseForm
                v-model="searchKeyword"
                type="text"
                class="items-customer-search"
                placeholder="고객명 또는 연락처 검색"
                @input="handleInput"
              />
              <ul v-if="showDropdown" class="autocomplete-dropdown">
                <li
                  v-for="(customer, index) in searchResults"
                  :key="customer.customer_id || index"
                  @click="handleCustomerSelect(customer)"
                >
                  <div>
                    <strong>{{ customer.customer_name }}</strong>
                    <span style="margin-left: 8px; color: #888">{{ customer.phone_number }}</span>
                  </div>
                </li>
              </ul>
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

            <!-- 결제 수단 렌더링 -->
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
              <BaseButton class="submit-button" @click="submit">등록</BaseButton>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ItemsSelectModal
      v-if="showProductModal"
      @close="showProductModal = false"
      @apply="applySelectedProducts"
    />

    <BaseToast ref="toastRef" />
  </div>
</template>

<script setup>
  import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import ItemsSelectModal from '@/features/sales/components/ItemsSelectModal.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import customersAPI from '@/features/customer/api/customers.js';
  import { getAvailableSessionPasses } from '@/features/membership/api/membership.js';
  import '@/features/sales/styles/SalesItemsModal.css';
  import { getStaff } from '@/features/staffs/api/staffs.js';
  import { getCustomerPrepaidPasses } from '@/features/membership/api/membership.js';
  import { registerItemSale } from '@/features/sales/api/sales.js';
  import { useAuthStore } from '@/store/auth';
  import couponsAPI from '@/features/coupons/api/coupons.js'; // 경로는 실제 위치에 맞게 조정

  const selectedPrepaidPassId = ref(null);
  const customerPrepaidPassOptions = ref([]);
  const prepaidTotalAmount = ref(0);
  const emit = defineEmits(['close', 'submit']);
  const toastRef = ref(null);
  const staffOptions = ref([]);
  const date = ref(new Date().toISOString().substring(0, 10));
  const time = ref(new Date().toTimeString().substring(0, 5));
  const selectedProducts = ref([]);
  const memo = ref('');
  const showProductModal = ref(false);
  const authStore = useAuthStore();
  const selectedMethods = ref([]);
  const paymentAmounts = ref({});
  const globalDiscountRate = ref(0);
  const discountRates = [0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50];
  const discountRateOptions = discountRates.map(rate => ({ value: rate, text: `${rate}%` }));
  const methods = ref([
    { key: 'prepaid', label: '선불권' },
    { key: 'card', label: '카드 결제' },
    { key: 'cash', label: '현금 결제' },
    { key: 'naver', label: '네이버페이' },
    { key: 'local', label: '지역화폐' },
  ]);
  const totalPaymentAmount = computed(() =>
    selectedMethods.value.reduce((sum, key) => {
      return sum + (paymentAmounts.value[key] || 0);
    }, 0)
  );

  const totalPrice = computed(() =>
    selectedProducts.value.reduce((sum, item) => sum + (item.finalPrice || 0), 0)
  );
  const globalDiscountAmount = computed(() =>
    Math.floor((totalPrice.value * globalDiscountRate.value) / 100)
  );
  const finalTotalPrice = computed(() => totalPrice.value - globalDiscountAmount.value);

  const openProductModal = () => (showProductModal.value = true);

  // 🔍 고객 검색 관련
  const customerId = ref(null);
  const searchKeyword = ref('');
  const searchResults = ref([]);
  const showDropdown = ref(false);
  const cachedCustomers = ref([]);
  const lastFetchTime = ref(0);
  const CACHE_DURATION = 5 * 60 * 1000;

  const isSearching = ref(false);
  const searchTimeout = ref(null);

  const fetchCustomers = async () => {
    const now = Date.now();
    if (cachedCustomers.value.length > 0 && now - lastFetchTime.value < CACHE_DURATION) {
      return cachedCustomers.value;
    }

    try {
      const customers = await customersAPI.getCustomersByShop();
      cachedCustomers.value = customers;
      lastFetchTime.value = now;
      return customers;
    } catch (e) {
      console.warn('[SalesItemsModal] 고객 목록 조회 실패', e);
      return [];
    }
  };

  const sessionPassTotal = computed(() =>
    selectedProducts.value
      .filter(p => !!p.deduction)
      .reduce((sum, p) => sum + (p.finalPrice || 0), 0)
  );

  const fetchCustomerSuggestions = async keyword => {
    if (!keyword || !keyword.trim()) {
      searchResults.value = [];
      showDropdown.value = false;
      return;
    }

    if (isSearching.value) return;
    isSearching.value = true;

    try {
      let results = [];

      try {
        results = await customersAPI.searchByKeyword(keyword);
      } catch (e) {
        console.warn('[SalesItemsModal] 키워드 검색 실패', e);
      }

      if (!results.length) {
        try {
          const strings = await customersAPI.autocomplete(keyword);
          results = strings.map((s, i) => ({
            customer_id: `auto-${i}`,
            customer_name: s,
            phone_number: '',
          }));
        } catch (e) {
          console.warn('[SalesItemsModal] 자동완성 실패', e);
        }
      }

      if (!results.length) {
        const customers = await fetchCustomers();
        const lower = keyword.toLowerCase();
        results = customers.filter(
          c =>
            (c.customer_name || '').toLowerCase().includes(lower) ||
            (c.phone_number || '').includes(lower)
        );
      }

      searchResults.value = results;
      showDropdown.value = results.length > 0;
    } catch (e) {
      console.warn('[SalesItemsModal] 검색 실패', e);
      searchResults.value = [];
      showDropdown.value = false;
    } finally {
      isSearching.value = false;
    }
  };

  const handleCustomerSelect = async customer => {
    searchKeyword.value = `${customer.customer_name} (${customer.phone_number})`;
    customerId.value = customer.customer_id;
    showDropdown.value = false;

    updateSessionPassesForProducts();
    await fetchPrepaidTotalAmount(); // ← 고객 선택 시 선불권 총액 조회
  };

  const handleInput = () => {
    showDropdown.value = true;
    if (searchTimeout.value) clearTimeout(searchTimeout.value);
    searchTimeout.value = setTimeout(() => {
      fetchCustomerSuggestions(searchKeyword.value);
    }, 300);
  };

  const applySelectedProducts = async products => {
    const existingIds = selectedProducts.value.map(p => p.id);
    const newProducts = [];

    let allSessionPasses = [];
    if (customerId.value) {
      try {
        allSessionPasses = await getAvailableSessionPasses(customerId.value);
      } catch (e) {
        console.warn('[SalesItemsModal] 전체 session pass 조회 실패', e);
      }
    }

    for (const p of products) {
      if (existingIds.includes(p.id)) continue;

      const price = p.price || 0;
      const quantity = 1;
      const total = price * quantity;
      const rate = 0;
      const discountAmount = 0;
      const finalPrice = total - discountAmount;

      const availableSessionPasses = !customerId.value
        ? [{ value: '', text: '고객을 먼저 선택해주세요' }]
        : [
            { value: '', text: '횟수권 선택' },
            ...allSessionPasses
              .filter(pass => String(pass.secondaryItemId) === String(p.id))
              .map(pass => ({
                value: pass.customerSessionPassId,
                text: `${pass.sessionPassName} (${pass.remainingCount}회/${pass.totalCount}회, ${pass.expirationDate})`,
              })),
          ];

      newProducts.push({
        ...p,
        quantity,
        price,
        deduction: '',
        availableSessionPasses,
        manager: '',
        discountRate: rate,
        discountAmount,
        finalPrice,
        couponCode: '',
        couponInfo: '',
        couponId: null,
        couponDiscountRate: 0,
      });
    }

    selectedProducts.value = [...selectedProducts.value, ...newProducts];
  };

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

  onMounted(() => {
    window.addEventListener('keydown', handleKeydown);
    fetchCustomers();
    fetchStaffs();
  });
  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown);
  });

  const handleKeydown = event => {
    if (event.key === 'Escape') emit('close');
  };

  watch([selectedMethods, finalTotalPrice], ([methods, total]) => {
    if (methods.length === 1) paymentAmounts.value[methods[0]] = total;
    Object.keys(paymentAmounts.value).forEach(key => {
      if (!methods.includes(key)) delete paymentAmounts.value[key];
    });
  });

  const applyGlobalDiscount = () => {};

  const recalculateProduct = index => {
    const product = selectedProducts.value[index];
    const quantity = product.quantity || 1;
    const price = product.price || 0;

    const baseTotal = price * quantity;

    const sessionPassApplied = !!product.deduction;
    const couponDiscountRate = product.couponDiscountRate || 0;
    const discountRate = sessionPassApplied ? 0 : product.discountRate || 0;

    const couponDiscountAmount = Math.floor(baseTotal * (couponDiscountRate / 100));
    const normalDiscountAmount = Math.floor(
      (baseTotal - couponDiscountAmount) * (discountRate / 100)
    );
    const finalDiscount = couponDiscountAmount + normalDiscountAmount;

    const finalPrice = sessionPassApplied ? 0 : baseTotal - finalDiscount;

    selectedProducts.value[index] = {
      ...product,
      discountAmount: finalDiscount,
      finalPrice,
    };
  };

  const removeProduct = index => selectedProducts.value.splice(index, 1);

  const submit = async () => {
    if (!customerId.value) {
      toastRef.value?.error('고객을 선택해주세요.');
      return;
    }

    if (totalPaymentAmount.value !== finalTotalPrice.value) {
      toastRef.value?.error(
        `결제 수단의 합계가 최종 금액과 일치하지 않습니다.\n합계: ${formatPrice(totalPaymentAmount.value)} / 최종 금액: ${formatPrice(finalTotalPrice.value)}`
      );
      return;
    }

    const PaymentsMethodEnum = {
      card: 'CARD',
      cash: 'CASH',
      naver: 'NAVER_PAY',
      local: 'LOCAL',
      prepaid: 'PREPAID_PASS',
      session_pass: 'SESSION_PASS',
    };

    for (const product of selectedProducts.value) {
      const salesDateStr = `${date.value}T${time.value}:00`;
      const payments = [];

      if (product.deduction) {
        payments.push({
          paymentsMethod: PaymentsMethodEnum['session_pass'],
          amount: product.finalPrice || product.price * product.quantity, // 0일 경우 대체
          customerSessionPassId: product.deduction,
          usedCount: product.quantity || 1,
        });
      }

      // 공통 결제 수단 (prepaid 제외)
      selectedMethods.value.forEach(method => {
        if (method === 'session_pass' || method === 'prepaid') return;

        payments.push({
          paymentsMethod: PaymentsMethodEnum[method],
          amount: paymentAmounts.value[method] || 0,
        });
      });

      // 선불권 결제
      if (selectedMethods.value.includes('prepaid')) {
        if (!selectedPrepaidPassId.value) {
          toastRef.value?.error('사용할 선불권을 선택해주세요.');
          return;
        }
        payments.push({
          paymentsMethod: PaymentsMethodEnum['prepaid'],
          amount: paymentAmounts.value['prepaid'] || 0,
          customerPrepaidPassId: selectedPrepaidPassId.value,
        });
      }

      const payload = {
        secondaryItemId: product.id,
        customerId: customerId.value,
        staffId: product.manager,
        shopId: Number(authStore.shopId),
        reservationId: null,
        discountRate: product.discountRate || 0,
        couponId: product.couponId || null, // 추후 쿠폰 처리 필요
        quantity: product.quantity,
        retailPrice: product.price,
        discountAmount: product.discountAmount || 0,
        totalAmount: product.finalPrice,
        salesMemo: memo.value,
        salesDate: salesDateStr,
        payments,
      };

      try {
        await registerItemSale(payload);
      } catch (e) {
        toastRef.value?.error('상품 매출 등록 중 오류가 발생했습니다.');
        console.error('[SalesItemsModal] 상품 매출 등록 실패', e);
        return;
      }
    }

    toastRef.value?.success('모든 상품 매출이 등록되었습니다.');
    emit('submit');
    emit('close');
  };

  const updateSessionPassesForProducts = async () => {
    if (!customerId.value) return;

    let allSessionPasses = [];
    try {
      allSessionPasses = await getAvailableSessionPasses(customerId.value);
    } catch (e) {
      console.warn('[SalesItemsModal] session pass 재조회 실패', e);
    }

    selectedProducts.value = selectedProducts.value.map(product => {
      const filteredPasses = allSessionPasses
        .filter(pass => String(pass.secondaryItemId) === String(product.id))
        .map(pass => ({
          value: pass.customerSessionPassId,
          text: `${pass.sessionPassName} (${pass.remainingCount}회/${pass.totalCount}회, ${pass.expirationDate})`,
        }));

      const availableSessionPasses = [
        { value: '', text: '횟수권 선택' },
        ...(filteredPasses.length ? filteredPasses : [{ value: '', text: '해당 상품 사용 불가' }]),
      ];

      return {
        ...product,
        availableSessionPasses,
      };
    });
  };

  const fetchPrepaidTotalAmount = async () => {
    try {
      const list = await getCustomerPrepaidPasses(customerId.value);
      prepaidTotalAmount.value = list.reduce((sum, item) => sum + (item.remainingAmount || 0), 0);
      customerPrepaidPassOptions.value = list.map(pass => ({
        value: pass.customerPrepaidPassId,
        text: `${pass.prepaidPassName} (${pass.remainingAmount.toLocaleString()}원, ${pass.expirationDate})`,
      }));

      if (list.length === 1) {
        selectedPrepaidPassId.value = list[0].customerPrepaidPassId;
      }
    } catch (e) {
      console.warn('[SalesItemsModal] 선불권 총합 조회 실패', e);
      prepaidTotalAmount.value = 0;
      customerPrepaidPassOptions.value = [];
      selectedPrepaidPassId.value = null;
    }
  };
  watch(sessionPassTotal, val => {
    if (val > 0) {
      paymentAmounts.value['session_pass'] = val;
      if (!selectedMethods.value.includes('session_pass')) {
        selectedMethods.value.push('session_pass');
      }
    } else {
      delete paymentAmounts.value['session_pass'];
      selectedMethods.value = selectedMethods.value.filter(m => m !== 'session_pass');
    }
  });

  watch(
    () => selectedProducts.value.map(p => p.couponCode),
    async (newCodes, oldCodes) => {
      for (let i = 0; i < newCodes.length; i++) {
        const code = newCodes[i];
        const old = oldCodes?.[i];

        if (code && code !== old) {
          try {
            const couponInfo = await couponsAPI.getCouponByCode(code);
            selectedProducts.value[i].couponInfo =
              `${couponInfo.couponTitle} (${couponInfo.discountRate}%)`;
            selectedProducts.value[i].couponId = couponInfo.couponId;
            selectedProducts.value[i].couponDiscountRate = couponInfo.discountRate;

            recalculateProduct(i); //
          } catch (e) {
            console.warn('쿠폰 조회 실패:', e);
            selectedProducts.value[i].couponInfo = '유효하지 않은 쿠폰입니다';
            selectedProducts.value[i].couponId = null;
            selectedProducts.value[i].couponDiscountRate = 0;

            recalculateProduct(i); //
          }
        }
      }
    },
    { deep: true }
  );

  const formatPrice = val => val?.toLocaleString('ko-KR') + '원';
</script>
