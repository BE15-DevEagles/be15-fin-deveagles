<template>
  <div class="mem-overlay" @click.self="emit('close')">
    <div class="mem-modal-panel">
      <!-- 상단 헤더 -->
      <div class="mem-top-bar">
        <div class="mem-info">
          <h2 class="mem-title">회원권 매출 수정</h2>
        </div>
        <div class="mem-actions">
          <button class="mem-close-button" @click="emit('close')">&times;</button>
        </div>
      </div>

      <div class="mem-divider"></div>

      <div class="mem-register-body">
        <!-- 좌측 -->
        <div class="mem-form-left scrollable-left">
          <!-- 날짜/시간 -->
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
          </div>

          <!-- 회원권 정보 -->
          <div class="mem-membership-box">
            <div class="mem-selected-detail">
              <div class="mem-product-header">
                <p>
                  <strong>{{ selectedMembership.name }}</strong>
                  ({{ formatPrice(selectedMembership.price) }})
                </p>
              </div>
              <div class="mem-grid-container">
                <div class="mem-input-group">
                  <label>수량</label>
                  <BaseForm v-model.number="selectedMembership.quantity" type="number" />
                </div>
                <div class="mem-input-group">
                  <label>가격</label>
                  <BaseForm v-model.number="selectedMembership.price" type="number" />
                </div>
                <div class="mem-input-group">
                  <label>제공 혜택</label>
                  <BaseForm v-model="selectedMembership.benefit" type="text" />
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
                <div v-if="selectedMembership.type === 'SESSION'" class="mem-input-group">
                  <label>잔여 횟수</label>
                  <BaseForm v-model.number="selectedMembership.remainSESSION" type="number" />
                </div>

                <div class="mem-input-group">
                  <label>유효 기간</label>
                  <PrimeDatePicker v-model="selectedMembership.expirationDate" />
                </div>

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
          </div>

          <!-- 메모 -->
          <label class="mem-memo-label">메모</label>
          <BaseForm v-model="memo" type="textarea" placeholder="메모 입력" class="mem-textarea" />
        </div>

        <!-- 우측 -->
        <div class="mem-form-right">
          <div class="mem-form-right-body">
            <div class="mem-search-row">
              <BaseForm
                type="text"
                class="mem-customer-search"
                :model-value="selectedMembership.customerName"
                readonly
              />
            </div>

            <div class="mem-total-price-section">
              <label>결제 금액</label>
              <div class="mem-total-display">
                <div class="mem-price-box">
                  {{ selectedMembership ? formatPrice(selectedMembership.price) : '가격' }}
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

          <div class="mem-form-right-footer">
            <div class="mem-footer-buttons">
              <BaseButton class="mem-cancel-button" outline @click="emit('close')">닫기</BaseButton>
              <BaseButton class="mem-submit-button" @click="submit">수정</BaseButton>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, onMounted, watch } from 'vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import { getStaff } from '@/features/staffs/api/staffs.js';
  import { getPrepaidPass, getSessionPass } from '@/features/membership/api/membership.js';

  const props = defineProps({
    initialMembership: Object,
    initialDate: String,
    initialTime: String,
    initialMemo: String,
    initialPayments: Array,
  });

  const emit = defineEmits(['close', 'submit']);

  const date = ref(props.initialDate);
  const time = ref(props.initialTime);
  const memo = ref(props.initialMemo);

  const selectedMembership = ref({
    id: props.initialMembership.prepaidPassId ?? props.initialMembership.sessionPassId,
    name: props.initialMembership.prepaidPassName ?? props.initialMembership.sessionPassName,
    type: props.initialMembership.prepaidPassName
      ? 'PREPAID'
      : props.initialMembership.sessionPassName
        ? 'SESSION'
        : '',
    price: props.initialMembership.retailPrice,
    quantity: props.initialMembership.quantity ?? 1,
    benefit: props.initialMembership.benefit ?? '',
    expirationDate: props.initialMembership.expireDate ?? '',
    manager: props.initialMembership.manager ?? '',
    chargeAmount: props.initialMembership.chargeAmount ?? 0,
    remainAmount: props.initialMembership.remainAmount ?? 0,
    totalSESSION: props.initialMembership.totalCount ?? 0,
    remainSESSION: props.initialMembership.remainCount ?? 0,
    customerName: props.initialMembership.customerName,
  });

  const methods = ref([
    { key: 'CARD', label: '카드 결제' },
    { key: 'CASH', label: '현금 결제' },
    { key: 'NAVER_PAY', label: '네이버페이' },
    { key: 'LOCAL', label: '지역화폐' },
  ]);

  const selectedMethods = ref(props.initialPayments?.map(p => p.method) || []);
  const paymentAmounts = ref(
    Object.fromEntries((props.initialPayments || []).map(p => [p.method, p.amount]))
  );

  const staffOptions = ref([]);
  const fetchStaffs = async () => {
    try {
      const res = await getStaff({ page: 1, size: 100, isActive: true });
      staffOptions.value = res.data.data.staffList.map(s => ({
        value: s.staffId,
        text: s.staffName,
      }));
    } catch (e) {
      console.error('직원 목록 불러오기 실패:', e);
    }
  };

  const fetchMembershipDetails = async () => {
    const type = selectedMembership.value.type;
    const id = selectedMembership.value.id;

    try {
      const list = type === 'PREPAID' ? await getPrepaidPass() : await getSessionPass();
      const item = list.find(i =>
        type === 'PREPAID' ? i.prepaid_pass_id === id : i.session_pass_id === id
      );
      if (!item) return;

      const bonus = item.bonus ?? 0;
      const discount = item.discountRate ?? 0;
      const price = item.prepaidPassPrice ?? item.sessionPassPrice ?? 0;
      const discounted = Math.floor(price * (1 - discount / 100));
      const benefit =
        bonus && discount
          ? `(추가제공) ${bonus}, (할인) ${discount}%`
          : bonus
            ? `(추가제공) ${bonus}`
            : discount
              ? `(할인) ${discount}%`
              : '(할인) 0%';

      const expirationDate = calcExpiration(item.expirationPeriod, item.expirationPeriodType);

      selectedMembership.value = {
        ...selectedMembership.value,
        name: item.prepaidPassName || item.sessionPassName,
        benefit,
        price: discounted,
        expirationPeriod: item.expirationPeriod,
        expirationPeriodType: item.expirationPeriodType,
        expirationDate,
        memo: item.prepaidPassMemo || item.sessionPassMemo || '',
        ...(type === 'PREPAID' && {
          chargeAmount: price + bonus,
        }),
        ...(type === 'SESSION' && {
          totalSESSION: item.session ?? 0,
        }),
      };
    } catch (e) {
      console.error('회원권 정보 불러오기 실패:', e);
    }
  };

  const calcExpiration = (period, type) => {
    const now = new Date();
    switch (type) {
      case 'DAY':
        now.setDate(now.getDate() + period);
        break;
      case 'WEEK':
        now.setDate(now.getDate() + period * 7);
        break;
      case 'MONTH':
        now.setMonth(now.getMonth() + period);
        break;
      case 'YEAR':
        now.setFullYear(now.getFullYear() + period);
        break;
    }
    return now.toISOString().substring(0, 10);
  };

  onMounted(() => {
    fetchStaffs();
    fetchMembershipDetails();
  });

  watch([selectedMembership, selectedMethods], ([membership, selected]) => {
    if (!membership) return;
    const total = membership.price || 0;
    if (selected.length === 1) {
      paymentAmounts.value[selected[0]] = total;
    }
    Object.keys(paymentAmounts.value).forEach(key => {
      if (!selected.includes(key)) {
        delete paymentAmounts.value[key];
      }
    });
  });

  const submit = () => {
    const payments = selectedMethods.value.map(key => ({
      method: key,
      amount: paymentAmounts.value[key] || 0,
    }));

    emit('submit', {
      date: date.value,
      time: time.value,
      membership: selectedMembership.value,
      memo: memo.value,
      payments,
    });
    emit('close');
  };

  const formatPrice = val => val?.toLocaleString('ko-KR') + '원';
</script>
