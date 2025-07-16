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
              v-for="t in amSlots"
              :key="t"
              :outline="form.time !== t"
              :disabled="bookedTimes.includes(t)"
              @click="selectTime(t)"
            >
              {{ t }}
            </BaseButton>
          </div>

          <h4>오후</h4>
          <div class="time-grid">
            <BaseButton
              v-for="t in pmSlots"
              :key="t"
              :class="['btn', form.time === t ? 'btn-primary' : 'btn-outline btn-primary']"
              :disabled="bookedTimes.includes(t)"
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
                / {{ item.timeTaken }}분
              </template>
              )
            </div>
          </div>
          <div class="submit-area">
            <BaseButton type="primary" :disabled="!isValid" @click="openConfirmModal">
              예약하기
            </BaseButton>
          </div>
        </div>
      </div>
    </div>
  </div>
  <BaseConfirm
    v-model="showConfirm"
    title="예약 확인"
    :message="reservationMessage"
    confirm-text="예약하기"
    cancel-text="취소"
    @confirm="handleConfirmReservation"
  />
</template>

<script setup>
  import { reactive, computed, watch, ref, onMounted } from 'vue';
  import { useRoute } from 'vue-router';
  import { useToast } from 'vue-toastification';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import BaseBadge from '@/components/common/BaseBadge.vue';
  import { useRouter } from 'vue-router';
  import BaseConfirm from '@/components/common/BaseConfirm.vue';

  import {
    getActiveSecondaryItems,
    getAllPrimaryItems,
    fetchStaffBookedTimes,
    fetchReservationSettings,
    createCustomReservation,
  } from '@/features/schedules/api/schedules.js';

  const timeSlots = ref([]);
  const toast = useToast();
  const route = useRoute();
  const phoneInput = ref('');
  const badgeColorMap = reactive({});
  const shopId = route.params.shopId;
  const bookedTimes = ref([]);
  const primaryItems = ref([]);
  const selectedPrimaryId = ref(null);
  const allSecondaryItems = ref([]);
  const router = useRouter();
  const showConfirm = ref(false);
  const selectedServicesMap = reactive({});

  const form = reactive({
    staffId: route.params.staffId,
    customer: '',
    phone: '',
    memo: '',
    date: null,
    time: '',
    menu: '',
    services: [],
  });

  const reservationMessage = computed(() => {
    if (!form.date || !form.time) return '선택된 시간 정보가 없습니다.';
    const d = new Date(form.date);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    const hhmm = form.time;
    return `${yyyy}-${mm}-${dd} ${hhmm} 에 예약하시겠습니까?`;
  });

  const amSlots = computed(() => timeSlots.value.filter(t => Number(t.split(':')[0]) < 12));

  const pmSlots = computed(() => timeSlots.value.filter(t => Number(t.split(':')[0]) >= 12));
  watch(timeSlots, val => {
    console.log('timeSlots 값:', val);
  });
  const generateTimeSlots = (start, end, term, lunchStart, lunchEnd) => {
    const slots = [];
    const toMinutes = t => t.hours * 60 + t.minutes;

    let current = toMinutes(start);
    const endMinutes = toMinutes(end);
    const lunchStartMin = toMinutes(lunchStart);
    const lunchEndMin = toMinutes(lunchEnd);

    while (current <= endMinutes) {
      if (current < lunchStartMin || current >= lunchEndMin) {
        const hours = Math.floor(current / 60);
        const minutes = current % 60;
        const formatted = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
        slots.push(formatted);
      }
      current += term;
    }
    return slots;
  };
  const getDayOfWeek = date => {
    const jsDay = new Date(date).getDay();
    // 서버의 availableDay: 1(월)~7(일)
    return jsDay === 0 ? 7 : jsDay;
  };
  const openConfirmModal = () => {
    if (!isValid.value) {
      toast.warning('모든 필수 항목을 입력해주세요.');
      return;
    }
    showConfirm.value = true;
  };

  // 실제 확인 시 실행되는 함수
  const handleConfirmReservation = async () => {
    try {
      // 예약 등록 로직 실행 (submitReservation 로직 사용)
      const [hour, minute] = form.time.split(':').map(n => parseInt(n, 10));
      const startDateTime = new Date(form.date);
      startDateTime.setHours(hour, minute, 0, 0);

      // 시술 소요시간 계산 (30분 기준, 필요 시 동적으로 변경)
      const unitMinutes = 30;
      const endDateTime = new Date(startDateTime.getTime() + unitMinutes * 60000);

      const selectedIds = allSelectedServices.value
        .map(svc => {
          const item = allSecondaryItems.value.find(i => i.secondaryItemName === svc.name);
          return item ? item.secondaryItemId : null;
        })
        .filter(id => id !== null);

      const reservationStartAt = formatDateTimeLocal(form.date, form.time);
      const reservationEndAt = formatDateTimeLocal(
        form.date,
        `${String(endDateTime.getHours()).padStart(2, '0')}:${String(endDateTime.getMinutes()).padStart(2, '0')}`
      );

      const payload = {
        shopId: Number(shopId),
        staffId: Number(form.staffId),
        customerId: null,
        customerName: form.customer,
        customerPhone: form.phone.replace(/-/g, ''), // 하이픈 제거
        reservationMemo: form.memo,
        reservationStartAt,
        reservationEndAt,
        secondaryItemIds: selectedIds,
      };

      console.log('전송할 payload:', payload);

      const reservationId = await createCustomReservation(payload);
      toast.success('예약이 완료되었습니다! 예약번호: ' + reservationId);

      router.push(`/p/${shopId}`);
    } catch (err) {
      console.error('예약 생성 중 오류:', err);
      toast.error('예약 생성 중 오류가 발생했습니다.');
    }
  };
  onMounted(async () => {
    try {
      const settings = await fetchReservationSettings(shopId);

      const day = getDayOfWeek(new Date());
      const settingForDay = settings.find(s => s.availableDay === day);

      if (!settingForDay) {
        console.warn('해당 요일의 설정을 찾을 수 없습니다.');
        timeSlots.value = [];
        return;
      }

      const parseTime = str => {
        const [h, m] = str.split(':').map(n => parseInt(n, 10));
        return { hours: h, minutes: m };
      };

      const start = parseTime(settingForDay.availableStartTime);
      const end = parseTime(settingForDay.availableEndTime);
      const lunchStart = parseTime(settingForDay.lunchStartTime);
      const lunchEnd = parseTime(settingForDay.lunchEndTime);
      const term = settingForDay.reservationUnitMinutes;

      timeSlots.value = generateTimeSlots(start, end, term, lunchStart, lunchEnd);
    } catch (e) {
      console.error('예약 설정 조회 실패', e);
    }
  });

  const loadBookedTimes = async () => {
    try {
      const res = await fetchStaffBookedTimes(form.staffId, formatDateOnly(form.date));
      bookedTimes.value = Array.isArray(res.bookedTimes) ? res.bookedTimes.map(bt => bt.time) : [];
    } catch (e) {
      console.error('예약된 시간 조회 실패', e);
      bookedTimes.value = [];
    }
  };

  watch(
    () => form.date,
    async newDate => {
      if (!newDate) {
        bookedTimes.value = [];
        form.time = '';
        timeSlots.value = [];
        return;
      }

      await loadBookedTimes();
      form.time = '';

      const settings = await fetchReservationSettings(shopId);
      const day = getDayOfWeek(newDate);
      const settingForDay = settings.find(s => s.availableDay === day);

      if (!settingForDay) {
        timeSlots.value = [];
        return;
      }

      const parseTime = str => {
        const [h, m] = str.split(':').map(n => parseInt(n, 10));
        return { hours: h, minutes: m };
      };

      const generated = generateTimeSlots(
        parseTime(settingForDay.availableStartTime),
        parseTime(settingForDay.availableEndTime),
        settingForDay.reservationUnitMinutes,
        parseTime(settingForDay.lunchStartTime),
        parseTime(settingForDay.lunchEndTime)
      );

      timeSlots.value = generated;
    }
  );

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

  onMounted(async () => {
    try {
      primaryItems.value = await getAllPrimaryItems();
      allSecondaryItems.value = await getActiveSecondaryItems();
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

  const selectPrimary = item => {
    selectedPrimaryId.value = item.primaryItemId;
    form.menu = item.primaryItemName;
    form.services = selectedServicesMap[item.primaryItemId]
      ? [...selectedServicesMap[item.primaryItemId]]
      : [];
  };

  const filteredSecondaryItems = computed(() => {
    if (!selectedPrimaryId.value) return [];
    return allSecondaryItems.value.filter(item => item.primaryItemId === selectedPrimaryId.value);
  });

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

  const removeService = serviceName => {
    for (const key in selectedServicesMap) {
      const arr = selectedServicesMap[key];
      const idx = arr.indexOf(serviceName);
      if (idx !== -1) {
        arr.splice(idx, 1);
      }
    }
    const currentArr = selectedServicesMap[selectedPrimaryId.value];
    form.services = currentArr ? [...currentArr] : [];
  };

  const selectTime = time => {
    if (form.time === time) {
      form.time = '';
    } else {
      form.time = time;
    }
  };

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
  const formatDateTimeLocal = (date, time) => {
    const d = new Date(date);
    const [hour, minute] = time.split(':').map(n => parseInt(n, 10));
    d.setHours(hour, minute, 0, 0);

    // KST 그대로 문자열로 보내기 (ISO X)
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    const HH = String(d.getHours()).padStart(2, '0');
    const MM = String(d.getMinutes()).padStart(2, '0');
    const SS = '00';

    return `${yyyy}-${mm}-${dd}T${HH}:${MM}:${SS}`; // Z 안 붙임
  };
  const formatDateOnly = date => {
    const d = new Date(date);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  };

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

  const submitReservation = async () => {
    if (!isValid.value) {
      toast.warning('모든 필수 항목을 입력해주세요.');
      return;
    }

    try {
      // 1. 선택한 시술들의 총 소요시간 계산 (분 단위)
      const totalDuration = allSelectedServices.value.reduce((sum, svc) => {
        const item = allSecondaryItems.value.find(i => i.secondaryItemName === svc.name);
        return sum + (item && item.timeTaken ? item.timeTaken : 0);
      }, 0);

      // 2. 시작시간을 Date로 변환
      const startDateTime = new Date(form.date);
      const [startHour, startMin] = form.time.split(':').map(n => parseInt(n, 10));
      startDateTime.setHours(startHour, startMin, 0, 0);

      // 3. 종료시간 = 시작시간 + 총 소요시간(분)
      const endDateTime = new Date(startDateTime.getTime() + totalDuration * 60000);

      // 4. LocalDateTime 포맷으로 변환 (KST 그대로)
      const toLocalDateTimeString = d => {
        const yyyy = d.getFullYear();
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        const HH = String(d.getHours()).padStart(2, '0');
        const MM = String(d.getMinutes()).padStart(2, '0');
        const SS = '00';
        return `${yyyy}-${mm}-${dd}T${HH}:${MM}:${SS}`;
      };
      const reservationStartAt = toLocalDateTimeString(startDateTime);
      const reservationEndAt = toLocalDateTimeString(endDateTime);

      // 5. 선택된 secondaryItemIds
      const secondaryItemIds = allSelectedServices.value
        .map(svc => {
          const item = allSecondaryItems.value.find(i => i.secondaryItemName === svc.name);
          return item ? item.secondaryItemId : null;
        })
        .filter(id => id !== null);
      const phoneRaw = form.phone.replace(/-/g, '');
      // 6. payload 구성
      const payload = {
        shopId: Number(shopId),
        staffId: Number(form.staffId),
        customerId: null,
        customerName: form.customer,
        customerPhone: phoneRaw,
        reservationMemo: form.memo,
        reservationStartAt,
        reservationEndAt,
        secondaryItemIds,
      };

      console.log('전송할 payload:', payload);

      // 7. API 호출
      const reservationId = await createCustomReservation(payload);
      toast.success('예약이 완료되었습니다! 예약번호: ' + reservationId);
      console.log('생성된 예약 ID:', reservationId);
    } catch (err) {
      console.error('예약 생성 중 오류:', err);
      toast.error('예약 생성 중 오류가 발생했습니다.');
    }
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

  button:disabled {
    background-color: var(--color-gray-200);
    color: var(--color-gray-500);
    border-color: var(--color-gray-300);
    cursor: not-allowed;
    opacity: 0.6;
  }
</style>
