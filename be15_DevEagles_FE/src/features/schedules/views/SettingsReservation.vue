<template>
  <div class="reservation-wrapper">
    <div class="reservation-settings">
      <h1 class="font-screen-title">예약 설정</h1>

      <!-- 점심시간 설정 라디오 -->
      <div class="section">
        <p class="section-title">점심시간 설정</p>
        <div class="radio-horizontal inline-radio">
          <label><input v-model="lunchEnabled" type="radio" :value="true" /> 설정</label>
          <label><input v-model="lunchEnabled" type="radio" :value="false" /> 비설정</label>
        </div>
      </div>

      <div class="section">
        <p class="section-title">예약 가능 시간 & 점심 시간 설정</p>
        <div class="time-grid">
          <div :class="['header-row', lunchEnabled ? 'lunch-on' : 'lunch-off']">
            <div>요일</div>
            <div>예약 시작</div>
            <div>~</div>
            <div>예약 마감</div>
            <template v-if="lunchEnabled">
              <div></div>
              <div>점심 시작</div>
              <div>~</div>
              <div>점심 마감</div>
            </template>
          </div>

          <!-- 요일별 row -->
          <div
            v-for="day in days"
            :key="day"
            :class="['time-row', lunchEnabled ? 'lunch-on' : 'lunch-off']"
          >
            <button
              class="day-button"
              :class="{ active: activeDays.includes(day) }"
              @click="toggleDay(day)"
            >
              {{ day }}
            </button>

            <template v-if="activeDays.includes(day)">
              <!-- 예약 시작/마감 -->
              <PrimeDatePicker
                v-model="availableTimes[day].start"
                :time-only="true"
                hour-format="24"
                :show-button-bar="true"
                :clearable="false"
                :manual-input="false"
                :placeholder="'시간 선택'"
              />
              <div class="time-separator">~</div>
              <PrimeDatePicker
                v-model="availableTimes[day].end"
                :time-only="true"
                hour-format="24"
                :show-button-bar="true"
                :clearable="false"
                :manual-input="false"
                :placeholder="'시간 선택'"
              />

              <!-- 점심 시작/마감 -->
              <template v-if="lunchEnabled">
                <div></div>
                <PrimeDatePicker
                  v-model="lunchTimes[day].start"
                  :time-only="true"
                  hour-format="24"
                  :show-button-bar="true"
                  :clearable="false"
                  :manual-input="false"
                  :placeholder="'시간 선택'"
                />
                <div class="time-separator">~</div>
                <PrimeDatePicker
                  v-model="lunchTimes[day].end"
                  :time-only="true"
                  hour-format="24"
                  :show-button-bar="true"
                  :clearable="false"
                  :manual-input="false"
                  :placeholder="'시간 선택'"
                />
              </template>
            </template>

            <template v-else>
              <div
                class="disabled-text"
                :style="{ gridColumn: lunchEnabled ? 'span 8' : 'span 5' }"
              >
                미사용
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- 예약 간격 -->
      <div class="section">
        <p class="section-title">예약 시간 간격</p>
        <div class="radio-horizontal inline-radio">
          <label><input v-model="interval" type="radio" value="10" /> 10분</label>
          <label><input v-model="interval" type="radio" value="30" /> 30분</label>
        </div>
      </div>

      <!-- 저장 버튼 -->
      <div class="button-group">
        <BaseButton type="primary" @click="saveSettings">설정 저장</BaseButton>
      </div>
    </div>
  </div>

  <BaseConfirm
    v-model="showConfirm"
    title="예약 설정 저장"
    message="현재 내용을 저장하시겠습니까?"
    :confirm-text="'저장'"
    :cancel-text="'취소'"
    :confirm-type="'primary'"
    @confirm="onConfirmSave"
  />

  <BaseToast ref="toastRef" position="top-right" />
</template>

<script setup>
  import { ref, onMounted, watch } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import BaseConfirm from '@/components/common/BaseConfirm.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import {
    fetchMyReservationSettings,
    updateReservationSettings,
  } from '@/features/schedules/api/schedules';

  const days = ['월', '화', '수', '목', '금', '토', '일'];
  const dayIndexToName = { 0: '일', 1: '월', 2: '화', 3: '수', 4: '목', 5: '금', 6: '토', 7: '일' };
  const activeDays = ref([]);
  const interval = ref('');
  const lunchEnabled = ref(false);

  const availableTimes = ref({});
  const lunchTimes = ref({});

  const showConfirm = ref(false);
  const toastRef = ref();

  const toggleDay = day => {
    if (activeDays.value.includes(day)) {
      activeDays.value = activeDays.value.filter(d => d !== day);
    } else {
      activeDays.value.push(day);
    }
  };

  const fetchSettings = async () => {
    try {
      const data = await fetchMyReservationSettings();

      const available = {};
      const lunch = {};
      const active = [];
      let hasLunchTime = false; // 점심시간이 존재하는지 체크

      const parseTime = (timeStr, defaultHour = null) => {
        if (!timeStr && defaultHour !== null) {
          const d = new Date();
          d.setHours(defaultHour, 0, 0, 0);
          return d;
        }
        if (!timeStr) return null;
        const [hh, mm] = timeStr.split(':').map(Number);
        const d = new Date();
        d.setHours(hh, mm, 0, 0);
        return d;
      };

      for (const day of days) {
        available[day] = {
          start: parseTime(null, 9),
          end: parseTime(null, 18),
        };

        lunch[day] = {
          start: null,
          end: null,
        };
      }

      for (const setting of data) {
        const day = dayIndexToName[setting.availableDay];
        if (!day) continue;

        const availableStart = parseTime(setting.availableStartTime?.slice(0, 5), 9);
        const availableEnd = parseTime(setting.availableEndTime?.slice(0, 5), 18);
        available[day] = { start: availableStart, end: availableEnd };

        const lunchStart = parseTime(setting.lunchStartTime?.slice(0, 5));
        const lunchEnd = parseTime(setting.lunchEndTime?.slice(0, 5));
        lunch[day] = { start: lunchStart, end: lunchEnd };

        if (lunchStart && lunchEnd) {
          lunch[day] = { start: lunchStart, end: lunchEnd };
          hasLunchTime = true;
        } else {
          lunch[day] = { start: null, end: null };
        }

        active.push(day);

        if (!interval.value && setting.reservationUnitMinutes) {
          interval.value = setting.reservationUnitMinutes.toString();
        }
      }

      availableTimes.value = available;
      lunchTimes.value = lunch;
      activeDays.value = active;
      lunchEnabled.value = hasLunchTime;
    } catch (err) {
      console.error('예약 설정 조회 실패:', err);
      toastRef.value?.error('예약 설정 정보를 불러오는 데 실패했습니다.');
    }
  };

  const toTimeString = value => {
    if (!value) return null;
    if (typeof value === 'string') return value.length === 5 ? `${value}:00` : value;
    const d = new Date(value);
    return d.toTimeString().slice(0, 8); // HH:mm:ss
  };

  const onConfirmSave = async () => {
    const dayToIndex = { 월: 1, 화: 2, 수: 3, 목: 4, 금: 5, 토: 6, 일: 7 };
    const requestList = [];

    for (const day of days) {
      const isActive = activeDays.value.includes(day);
      const dayIndex = dayToIndex[day];
      if (!isActive) continue;

      const { start, end } = availableTimes.value[day] || {};
      const startFormatted = toTimeString(start);
      const endFormatted = toTimeString(end);

      if (!startFormatted || !endFormatted || startFormatted >= endFormatted) {
        toastRef.value?.error(`${day}요일의 예약 시간이 올바르지 않습니다.`);
        return;
      }

      let lunchStart = null;
      let lunchEnd = null;

      if (lunchEnabled.value) {
        const { start: lStart, end: lEnd } = lunchTimes.value[day] || {};
        lunchStart = toTimeString(lStart);
        lunchEnd = toTimeString(lEnd);

        if (!lunchStart || !lunchEnd || lunchStart >= lunchEnd) {
          toastRef.value?.error(`${day}요일의 점심시간이 올바르지 않습니다.`);
          return;
        }
      }

      requestList.push({
        availableDay: dayIndex,
        availableStartTime: startFormatted,
        availableEndTime: endFormatted,
        lunchStartTime: lunchStart,
        lunchEndTime: lunchEnd,
        reservationTerm: Number(interval.value),
      });
    }

    try {
      await updateReservationSettings(requestList);
      toastRef.value?.success('설정이 정상적으로 저장되었습니다!');
    } catch (err) {
      console.error('예약 설정 저장 실패:', err);
      toastRef.value?.error('설정 저장에 실패했습니다.');
    }
  };

  const saveSettings = () => {
    showConfirm.value = true;
  };

  watch(lunchEnabled, enabled => {
    if (enabled) {
      for (const day of days) {
        const lunch = lunchTimes.value[day] || {};
        if (!lunch.start || !lunch.end) {
          const defaultLunchStart = new Date();
          defaultLunchStart.setHours(12, 0, 0, 0);

          const defaultLunchEnd = new Date();
          defaultLunchEnd.setHours(13, 0, 0, 0);

          lunchTimes.value[day] = {
            start: defaultLunchStart,
            end: defaultLunchEnd,
          };
        }
      }
    }
  });

  onMounted(fetchSettings);
</script>

<style scoped>
  .reservation-settings {
    padding: 24px;
    max-width: 880px;
    margin: 0 auto;
    background: var(--color-neutral-white);
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  }

  .section {
    margin-bottom: 32px;
  }

  .section-title {
    font-weight: bold;
    margin-bottom: 12px;
    font-size: 18px;
  }

  .time-grid {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  /* 점심 ON */
  .header-row.lunch-on,
  .time-row.lunch-on {
    display: grid;
    grid-template-columns: 60px 1fr 24px 1fr 48px 1fr 24px 1fr;
    gap: 8px;
    align-items: center;
  }

  /* 점심 OFF */
  .header-row.lunch-off,
  .time-row.lunch-off {
    display: grid;
    grid-template-columns: 60px 1fr 24px 1fr;
    gap: 8px;
    align-items: center;
  }

  .day-button {
    padding: 6px 12px;
    border: 1px solid var(--color-gray-300);
    border-radius: 6px;
    background: var(--color-neutral-white);
    cursor: pointer;
  }

  .day-button.active {
    background-color: var(--color-primary-main);
    color: white;
    border-color: var(--color-primary-main);
  }

  .time-separator {
    text-align: center;
    font-weight: 500;
    color: #444;
  }

  .disabled-text {
    color: var(--color-gray-500);
    font-size: 14px;
    padding-left: 8px;
  }

  .radio-horizontal {
    display: flex;
    gap: 24px;
    align-items: center;
    flex-wrap: wrap;
  }

  .inline-radio label {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }

  .button-group {
    display: flex;
    justify-content: flex-end;
  }

  .header-row > div {
    text-align: center;
    line-height: 1.6;
  }

  .reservation-wrapper {
    background-color: #f9fafb;
    min-height: 100vh;
    padding: 40px 0;
  }
  :deep(.p-datepicker-input) {
    padding-left: 12px !important;
    padding-right: 60px !important;
  }
</style>
