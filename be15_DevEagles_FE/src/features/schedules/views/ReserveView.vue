<template>
  <div class="page-wrapper">
    <h1 class="page-title">
      <img src="@/images/suri/team_logo-cutout.png" class="logo-img" alt="로고" />
      예약 페이지
    </h1>

    <div class="reservation-wrapper">
      <!-- 고객 정보 입력 -->
      <h3 class="section-title">👤 고객 정보를 입력해주세요</h3>
      <div class="input-section">
        <div class="form-group">
          <label for="customer">이름</label>
          <input
            id="customer"
            v-model="form.customer"
            type="text"
            placeholder="이름을 입력해주세요"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label for="phone">전화번호</label>
          <input
            id="phone"
            v-model="phoneInput"
            type="tel"
            inputmode="numeric"
            pattern="[0-9]*"
            placeholder="연락 가능한 번호를 입력해주세요"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label for="memo">메모</label>
          <input
            id="memo"
            v-model="form.memo"
            type="text"
            placeholder="요청 사항이 있다면 적어주세요"
            class="form-input"
          />
        </div>
      </div>

      <div class="main-section">
        <!-- 왼쪽: 날짜 & 시간 -->
        <div class="left-section">
          <h3>📅 날짜와 시간을 선택해 주세요</h3>
          <PrimeDatePicker
            v-model="form.date"
            :inline="true"
            :show-time="false"
            :min-date="new Date()"
            :show-icon="false"
          />
          <h4>오전</h4>
          <div class="time-grid">
            <BaseButton
              v-for="t in times.am"
              :key="t"
              :outline="form.time !== t"
              @click="selectTime(t)"
            >
              {{ t }}
            </BaseButton>
          </div>
          <h4>오후</h4>
          <div class="time-grid">
            <BaseButton
              v-for="t in times.pm"
              :key="'pm-' + t"
              :class="['btn', form.time === t ? 'btn-primary' : 'btn-outline btn-primary']"
              @click="selectTime(t)"
            >
              {{ t }}
            </BaseButton>
          </div>
        </div>

        <!-- 오른쪽: 시술 -->
        <div class="right-section">
          <h3>💇 시술 메뉴를 선택해 주세요</h3>
          <div class="menu-tabs">
            <BaseButton
              v-for="item in primaryItems"
              :key="item.primaryItemId"
              :class="[
                selectedPrimaryId === item.primaryItemId
                  ? 'btn btn-primary'
                  : 'btn btn-outline btn-primary',
              ]"
              @click="selectPrimary(item)"
            >
              {{ item.primaryItemName }}
            </BaseButton>
          </div>
          <div v-if="allSelectedServices.length > 0" class="selected-tags">
            <BaseBadge
              v-for="(svc, idx) in allSelectedServices"
              :key="svc.name + '-' + svc.primaryItemId"
              :type="badgeColorMap[svc.primaryItemId] || 'neutral'"
              pill
              class="selected-tag"
            >
              {{ svc.name }}
              <button class="remove-btn" @click.stop="removeService(svc.name)">×</button>
            </BaseBadge>
          </div>

          <div class="service-box">
            <div
              v-for="item in filteredSecondaryItems"
              :key="item.secondaryItemId"
              class="service-item"
              :class="{ selected: form.services.includes(item.secondaryItemName) }"
              @click="toggleService(item.secondaryItemName)"
            >
              {{ item.secondaryItemName }}
              ( {{ item.secondaryItemPrice.toLocaleString() }}원
              <template v-if="item.timeTaken != null && item.timeTaken !== 0">
                / {{ item.timeTaken }}분 </template
              >)
            </div>
          </div>
          <div class="submit-area">
            <BaseButton type="primary" :disabled="!isValid" @click="submitReservation">
              예약하기
            </BaseButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { reactive, computed, watch, ref, onMounted } from 'vue';
  import { useRoute } from 'vue-router';
  import { useToast } from 'vue-toastification';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import BaseBadge from '@/components/common/BaseBadge.vue';

  // API
  import {
    getActiveSecondaryItems,
    getAllPrimaryItems,
  } from '@/features/schedules/api/schedules.js';

  const toast = useToast();
  const route = useRoute();
  const phoneInput = ref('');
  const badgeColorMap = reactive({});
  // 상품 상태
  const primaryItems = ref([]);
  const selectedPrimaryId = ref(null);
  const allSecondaryItems = ref([]);

  // 탭별 선택 상태
  const selectedServicesMap = reactive({});

  // 예약 form
  const form = reactive({
    designerId: route.params.id,
    customer: '',
    phone: '',
    memo: '',
    date: null,
    time: '',
    menu: '',
    services: [], // 현재 탭에서 보여줄 선택
  });

  const allSelectedServices = computed(() => {
    const result = [];
    for (const pid in selectedServicesMap) {
      selectedServicesMap[pid].forEach(name => {
        result.push({
          name,
          primaryItemId: Number(pid),
        });
      });
    }
    return result;
  });

  // 초기 데이터
  onMounted(async () => {
    try {
      primaryItems.value = await getAllPrimaryItems();
      allSecondaryItems.value = await getActiveSecondaryItems();
      // 예: 색상 타입을 반복해서 할당
      const colorTypes = ['neutral', 'primary', 'secondary', 'success', 'warning', 'error'];
      primaryItems.value.forEach((item, idx) => {
        badgeColorMap[item.primaryItemId] = colorTypes[idx % colorTypes.length];
      });
      if (primaryItems.value.length > 0) {
        selectedPrimaryId.value = primaryItems.value[0].primaryItemId;
        form.menu = primaryItems.value[0].primaryItemName;
      }
    } catch (e) {
      console.error(e);
      toast.error('상품 목록 조회 중 오류가 발생했습니다.');
    }
  });

  // 1차 선택
  const selectPrimary = item => {
    selectedPrimaryId.value = item.primaryItemId;
    form.menu = item.primaryItemName;
    form.services = selectedServicesMap[item.primaryItemId]
      ? [...selectedServicesMap[item.primaryItemId]]
      : [];
  };

  // 2차 목록
  const filteredSecondaryItems = computed(() => {
    if (!selectedPrimaryId.value) return [];
    return allSecondaryItems.value.filter(item => item.primaryItemId === selectedPrimaryId.value);
  });

  // 2차 선택 토글
  const toggleService = serviceName => {
    const primaryId = selectedPrimaryId.value;
    if (!selectedServicesMap[primaryId]) {
      selectedServicesMap[primaryId] = [];
    }
    const arr = selectedServicesMap[primaryId];
    const idx = arr.indexOf(serviceName);
    if (idx === -1) {
      arr.push(serviceName);
    } else {
      arr.splice(idx, 1);
    }
    form.services = [...arr];
  };

  // X 버튼으로 제거
  const removeService = serviceName => {
    // 모든 탭에서 해당 서비스 제거
    for (const key in selectedServicesMap) {
      const arr = selectedServicesMap[key];
      const idx = arr.indexOf(serviceName);
      if (idx !== -1) {
        arr.splice(idx, 1);
      }
    }
    // 현재 탭의 선택 목록도 다시 동기화
    const currentArr = selectedServicesMap[selectedPrimaryId.value];
    form.services = currentArr ? [...currentArr] : [];
  };

  // 시간 선택
  const selectTime = time => {
    form.time = time;
  };

  // 전화번호 입력 포맷팅
  watch(phoneInput, val => {
    let digits = val.replace(/\D/g, '');
    if (val !== digits) toast.warning('숫자만 입력 가능합니다.');
    if (digits.length > 11) {
      digits = digits.slice(0, 11);
      toast.warning('전화번호는 최대 11자리까지 입력 가능합니다.');
    }
    let formatted = '';
    if (digits.length < 4) formatted = digits;
    else if (digits.length < 7) formatted = `${digits.slice(0, 3)}-${digits.slice(3)}`;
    else formatted = `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
    phoneInput.value = formatted;
    form.phone = formatted;
  });

  // 날짜 포맷
  const formatDateOnly = date => {
    const d = new Date(date);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  };

  // 유효성 검사
  const isValid = computed(() => {
    return (
      form.customer.trim() &&
      /^\d{3}-\d{3,4}-\d{4}$/.test(form.phone) &&
      form.date &&
      form.time &&
      form.menu &&
      allSelectedServices.value.length > 0
    );
  });

  // 예약하기
  const submitReservation = () => {
    if (!isValid.value) {
      toast.warning('모든 필수 항목을 입력해주세요.');
      return;
    }
    const payload = {
      ...form,
      services: allSelectedServices.value, // 전체 선택된 서비스 전송
      date: form.date ? formatDateOnly(form.date) : '',
    };
    toast.success('예약 정보가 정상적으로 준비되었습니다.');
    alert('예약 정보:\n' + JSON.stringify(payload, null, 2));
  };

  // 시간대
  const times = {
    am: ['10:00', '10:30', '11:00', '11:30'],
    pm: [
      '12:00',
      '12:30',
      '13:00',
      '13:30',
      '14:00',
      '14:30',
      '15:00',
      '15:30',
      '16:00',
      '16:30',
      '17:00',
      '17:30',
    ],
  };
</script>

<style scoped>
  .btn-outline.btn-primary {
    background: transparent;
    border: 1px solid var(--color-primary-main);
    color: var(--color-primary-main);
  }

  .btn-outline.btn-primary:hover {
    background: var(--color-primary-50);
  }

  .btn-outline.btn-primary:active {
    background: var(--color-primary-100);
  }

  .page-wrapper {
    padding: 32px 40px;
    background-color: var(--color-gray-50);
    min-height: 100vh;
    box-sizing: border-box;
  }

  .page-title {
    font-size: 40px;
    font-weight: bold;
    margin-bottom: 30px;
    color: var(--color-text-primary);
    text-align: left;
    display: flex;
    align-items: center;
    gap: 12px;
    padding-left: 132px;
  }

  .logo-img {
    height: 60px;
    width: auto;
  }
  .selected-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 12px;
  }

  .selected-tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding-right: 8px;
  }

  .selected-tag .remove-btn {
    border: none;
    background: transparent;
    font-size: 14px;
    cursor: pointer;
    line-height: 1;
    color: inherit;
  }

  .selected-tag .remove-btn:hover {
    color: var(--color-error-500);
  }

  .reservation-wrapper {
    background-color: var(--color-neutral-white);
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
    padding: 32px;
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .section-title {
    font-size: 20px;
    font-weight: 600;
    margin: 12px 0 8px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .input-section {
    display: grid;
    grid-template-columns: 1fr 1fr 2fr;
    gap: 8px 16px;
  }

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .form-input {
    padding: 10px;
    border: 1px solid var(--color-gray-300);
    border-radius: 6px;
    font-size: 14px;
    background-color: var(--color-neutral-white);
    color: var(--color-text-primary);
  }

  .form-input:focus {
    outline: none;
    border-color: var(--color-primary-main);
    box-shadow: 0 0 0 2px rgba(54, 79, 107, 0.15);
  }

  .main-section {
    display: flex;
    justify-content: space-between;
    gap: 48px;
  }

  .left-section,
  .right-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .time-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
    margin-top: 8px;
    margin-bottom: 16px;
  }

  .left-section h4 {
    margin-top: 4px;
    margin-bottom: 4px;
  }

  .menu-tabs {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .service-box {
    border: 1px solid var(--color-gray-300);
    padding: 12px;
    border-radius: 8px;
    background-color: var(--color-neutral-white);
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-height: 325px;
    overflow-y: auto;
  }

  .service-item {
    padding: 12px;
    border-radius: 6px;
    cursor: pointer;
    transition: background-color 0.2s ease;
    border: 1px solid transparent;
    background-color: transparent;
    color: var(--color-text-primary);
  }

  .service-item:hover {
    background-color: var(--color-gray-100);
  }

  .service-item.selected {
    background-color: var(--color-primary-main);
    color: var(--color-neutral-white);
    font-weight: bold;
  }

  .submit-area {
    margin-top: auto;
    padding-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
</style>
