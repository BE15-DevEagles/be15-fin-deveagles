<template>
  <div class="mem-overlay" @click.self="emit('close')">
    <div class="mem-modal-panel">
      <!-- 상단 헤더 -->
      <div class="mem-top-bar">
        <div class="mem-info">
          <h2 class="mem-title">회원권 매출 등록</h2>
        </div>
        <div class="mem-actions">
          <button class="mem-close-button" @click="emit('close')">&times;</button>
        </div>
      </div>

      <div class="mem-divider"></div>

      <div class="mem-register-body">
        <!-- 좌측 -->
        <div class="mem-form-left scrollable-left">
          <div class="mem-datetime-row">
            <div class="mem-date-time-group">
              <PrimeDatePicker v-model="date" placeholder="날짜 선택" class="date-picker" />
              <PrimeDatePicker
                v-model="time"
                placeholder="시간 선택"
                time-only
                show-time
                hour-format="24"
                class="time-picker"
              />
            </div>
            <BaseButton class="mem-select-button" @click="selectMembership">회원권 선택</BaseButton>
          </div>

          <div
            class="mem-membership-box"
            :class="{ clickable: !selectedMembership }"
            @click="!selectedMembership && selectMembership()"
          >
            <template v-if="!selectedMembership">
              <p class="mem-placeholder">영역을 클릭해<br />판매할 회원권을 선택해주세요.</p>
            </template>
            <template v-else>
              <div class="mem-selected-detail">
                <div class="mem-product-header">
                  <p>
                    <strong>{{ selectedMembership.name }}</strong>
                    ({{ formatPrice(selectedMembership.retailPrice) }})
                  </p>
                  <button class="mem-x-button" @click.stop="clearMembership">×</button>
                </div>
                <div class="mem-grid-container">
                  <div class="mem-input-group">
                    <label>수량</label>
                    <BaseForm v-model.number="selectedMembership.quantity" type="number" min="1" />
                  </div>
                  <div class="mem-input-group">
                    <label>정가</label>
                    <BaseForm v-model.number="selectedMembership.retailPrice" type="number" />
                  </div>
                  <div class="mem-input-group">
                    <label>제공 혜택</label>
                    <BaseForm v-model="selectedMembership.benefit" type="text" />
                  </div>
                  <div class="mem-input-group">
                    <label>가격</label>
                    <BaseForm v-model.number="selectedMembership.discountAmount" type="number" />
                  </div>
                  <div v-if="selectedMembership.type === 'PREPAID'" class="mem-input-group">
                    <label>충전 금액</label>
                    <BaseForm v-model.number="selectedMembership.chargeAmount" type="number" />
                  </div>
                  <div v-if="selectedMembership.type === 'PREPAID'" class="mem-input-group">
                    <label>잔여 금액</label>
                    <BaseForm v-model.number="selectedMembership.remainAmount" type="number" />
                  </div>
                  <div v-if="selectedMembership.type === 'SESSION'" class="mem-input-group">
                    <label>충전 횟수</label>
                    <BaseForm v-model.number="selectedMembership.totalSESSION" type="number" />
                  </div>
                  <div class="mem-input-group">
                    <label>유효 기간</label>
                    <PrimeDatePicker
                      v-if="selectedMembership"
                      v-model="selectedMembership.expirationDate"
                      class="date-picker"
                    />
                  </div>
                  <!-- 담당자 -->
                  <div class="mem-input-group">
                    <label>담당자</label>
                    <BaseForm
                      v-model.number="selectedMembership.manager"
                      type="select"
                      :options="staffOptions"
                      placeholder="담당자 선택"
                    />
                  </div>
                </div>
              </div>
            </template>
          </div>

          <label class="mem-memo-label">메모</label>
          <BaseForm v-model="memo" type="textarea" placeholder="메모 입력" class="mem-textarea" />
        </div>

        <div class="mem-form-right">
          <!-- 우측 바디 -->
          <div class="mem-form-right-body">
            <div class="mem-search-row" style="position: relative">
              <BaseForm
                v-model="searchKeyword"
                type="text"
                class="mem-customer-search"
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

            <div class="mem-total-price-section">
              <label>결제 금액</label>
              <div class="mem-total-display">
                <div class="mem-price-box">
                  {{ selectedMembership ? formatPrice(selectedMembership.discountAmount) : '가격' }}
                </div>
              </div>
            </div>

            <div v-for="method in methods" :key="method.key" class="mem-method-group">
              <div class="mem-checkbox-label">
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
                class="mem-method-price"
                placeholder="금액"
                :disabled="!selectedMethods.includes(method.key)"
                :class="{ disabled: !selectedMethods.includes(method.key) }"
              />
            </div>
          </div>

          <!-- 우측 하단 푸터 -->
          <div class="mem-form-right-footer">
            <div class="mem-footer-buttons">
              <BaseButton class="mem-cancel-button" outline @click="emit('close')">닫기</BaseButton>
              <BaseButton class="mem-submit-button" @click="submit">등록</BaseButton>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <Teleport to="body">
    <MembershipSelectModal
      v-if="showSelectModal"
      v-model="showSelectModal"
      @apply="handleMembershipSelected"
    />
  </Teleport>
</template>

<script setup>
  import { ref, watch, onMounted, onBeforeUnmount } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import MembershipSelectModal from '@/features/sales/components/MembershipSelectModal.vue';
  import '@/features/sales/styles/SalesMembershipModal.css';
  import { getStaff } from '@/features/staffs/api/staffs.js';
  import customersAPI from '@/features/customer/api/customers.js';
  import { registerPrepaidPassSale, registerSessionPassSale } from '@/features/sales/api/sales.js';
  import { useAuthStore } from '@/store/auth';

  const emit = defineEmits(['close', 'submit']);
  const authStore = useAuthStore();
  const date = ref(new Date().toISOString().substring(0, 10));
  const time = ref(new Date().toTimeString().substring(0, 5));
  const selectedMembership = ref(null);
  const memo = ref('');
  const showSelectModal = ref(false);
  const staffOptions = ref([]);
  const selectedMethods = ref([]);
  const paymentAmounts = ref({});

  const searchKeyword = ref('');
  const searchResults = ref([]);
  const showDropdown = ref(false);
  const selectedCustomer = ref(null);

  // 고객 검색 로직
  const cachedCustomers = ref([]);
  const lastFetchTime = ref(0);
  const CACHE_DURATION = 5 * 60 * 1000; // 5분

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
      console.warn('[SalesMembershipModal] 고객 목록 조회 실패', e);
      return [];
    }
  };

  const isSearching = ref(false);
  const searchTimeout = ref(null);

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

      // 1. Elasticsearch 기반 검색
      try {
        results = await customersAPI.searchByKeyword(keyword);
      } catch (e) {
        console.warn('[SalesMembershipModal] 키워드 검색 실패', e);
      }

      // 2. fallback: autocomplete API
      if (!results.length) {
        try {
          const strings = await customersAPI.autocomplete(keyword);
          results = strings.map((s, i) => ({
            customer_id: `auto-${i}`,
            customer_name: s,
            phone_number: '',
          }));
        } catch (e) {
          console.warn('[SalesMembershipModal] 자동완성 실패', e);
        }
      }

      // 3. fallback: 클라이언트 사이드 필터링
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
      console.warn('[SalesMembershipModal] 검색 실패', e);
      searchResults.value = [];
      showDropdown.value = false;
    } finally {
      isSearching.value = false;
    }
  };

  const handleCustomerSelect = customer => {
    selectedCustomer.value = customer;
    searchKeyword.value = customer.customer_name;
    showDropdown.value = false;
  };

  const handleInput = () => {
    showDropdown.value = true;
    if (searchTimeout.value) clearTimeout(searchTimeout.value);
    searchTimeout.value = setTimeout(() => {
      fetchCustomerSuggestions(searchKeyword.value);
    }, 300);
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

  const methods = ref([
    { key: 'CARD', label: '카드 결제' },
    { key: 'CASH', label: '현금 결제' },
    { key: 'NAVER_PAY', label: '네이버페이' },
    { key: 'LOCAL', label: '지역화폐' },
  ]);

  const selectMembership = () => {
    showSelectModal.value = true;
  };

  const handleMembershipSelected = item => {
    const hasBonus = item.bonus != null && !isNaN(item.bonus);
    const hasDiscount = item.discountRate != null && !isNaN(item.discountRate);

    const benefit =
      hasBonus && hasDiscount
        ? `(추가제공) ${item.bonus}, (할인) ${item.discountRate}%`
        : hasBonus
          ? `(추가제공) ${item.bonus}`
          : hasDiscount
            ? `(할인) ${item.discountRate}%`
            : '(할인) 0%';

    const discountedPrice = hasDiscount
      ? Math.floor(
          (item.prepaidPassPrice ?? item.sessionPassPrice ?? 0) * (1 - item.discountRate / 100)
        )
      : (item.prepaidPassPrice ?? item.sessionPassPrice ?? 0);

    const chargeAmount = hasBonus
      ? (item.prepaidPassPrice ?? 0) + item.bonus
      : (item.prepaidPassPrice ?? 0);

    const now = new Date();
    const expirationDate = new Date(now);
    switch (item.expirationPeriodType) {
      case 'DAY':
        expirationDate.setDate(now.getDate() + item.expirationPeriod);
        break;
      case 'WEEK':
        expirationDate.setDate(now.getDate() + item.expirationPeriod * 7);
        break;
      case 'MONTH':
        expirationDate.setMonth(now.getMonth() + item.expirationPeriod);
        break;
      case 'YEAR':
        expirationDate.setFullYear(now.getFullYear() + item.expirationPeriod);
        break;
    }

    const formattedExpiration = expirationDate.toISOString().substring(0, 10);

    selectedMembership.value = {
      id: item.prepaidPassId ?? item.sessionPassId,
      name: item.prepaidPassName ?? item.sessionPassName ?? '이름없음',
      retailPrice: item.prepaidPassPrice ?? item.sessionPassPrice ?? 0,
      discountAmount: discountedPrice,
      benefit,
      expirationPeriod: item.expirationPeriod,
      expirationPeriodType: item.expirationPeriodType,
      expirationDate: formattedExpiration,
      memo: item.prepaidPassMemo ?? item.sessionPassMemo ?? '',
      quantity: 1,
      type: item.type,
      manager: '',
      ...(item.type === 'PREPAID' && {
        chargeAmount,
        remainAmount: chargeAmount,
      }),
      ...(item.type === 'SESSION' && {
        totalSESSION: item.session ?? 0,
        remainSESSION: item.session ?? 0,
      }),
    };

    showSelectModal.value = false;
  };

  const clearMembership = () => {
    selectedMembership.value = null;
  };

  const submit = async () => {
    if (!selectedMembership.value || !selectedCustomer.value) {
      alert('회원권과 고객을 모두 선택해주세요.');
      return;
    }

    const membership = selectedMembership.value;
    const customer = selectedCustomer.value;
    const staffId = membership.manager;
    const totalAmount = selectedMethods.value.reduce((sum, key) => {
      return sum + (paymentAmounts.value[key] || 0);
    }, 0);

    const payments = selectedMethods.value.map(key => ({
      paymentsMethod: key, // 반드시 enum에 맞는 값이어야 함
      amount: paymentAmounts.value[key] || 0,
    }));

    const basePayload = {
      customerId: customer.customer_id,
      staffId,
      shopId: Number(authStore.shopId),
      reservationId: null,
      discountRate: null,
      retailPrice: membership.retailPrice,
      discountAmount: 0,
      totalAmount,
      salesMemo: memo.value,
      salesDate: `${date.value}T${time.value}:00`,
      payments,
    };

    try {
      if (membership.type === 'PREPAID') {
        await registerPrepaidPassSale({
          ...basePayload,
          prepaidPassId: membership.id,
        });
      } else if (membership.type === 'SESSION') {
        await registerSessionPassSale({
          ...basePayload,
          sessionPassId: membership.id,
        });
      }

      emit('submit');
      emit('close');
    } catch (e) {
      console.error('[매출 등록 실패]', e);
      alert('매출 등록 중 오류가 발생했습니다.');
    }
  };

  const handleKeydown = event => {
    if (event.key === 'Escape') {
      emit('close');
    }
  };

  const formatPrice = val => val?.toLocaleString('ko-KR') + '원';

  watch([selectedMembership, selectedMethods], ([membership, selected]) => {
    if (!membership) return;

    const total = membership.discountAmount || 0;

    if (selected.length === 1) {
      paymentAmounts.value[selected[0]] = total;
    }

    Object.keys(paymentAmounts.value).forEach(key => {
      if (!selected.includes(key)) {
        delete paymentAmounts.value[key];
      }
    });
  });

  onMounted(() => {
    fetchStaffs();
    fetchCustomers();
    window.addEventListener('keydown', handleKeydown);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown);
  });
</script>
