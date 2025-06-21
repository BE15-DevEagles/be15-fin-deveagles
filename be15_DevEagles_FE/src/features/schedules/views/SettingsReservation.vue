<template>
  <div class="reservation-settings">
    <h1 class="font-screen-title">예약 캘린더</h1>

    <!-- 예약 가능 시간 설정 -->
    <div class="section">
      <p class="section-title">예약 가능 시간 설정</p>

      <div v-for="day in days" :key="day" class="time-row">
        <button
          class="day-button"
          :class="{ active: activeDays.includes(day) }"
          @click="toggleDay(day)"
        >
          {{ day }}
        </button>

        <div v-if="activeDays.includes(day)" class="time-range">
          <PrimeDatePicker
            v-model="availableTimes[day].start"
            placeholder="시작 시간"
            :time-only="true"
            :show-time="true"
            :show-icon="false"
            hour-format="24"
            :manual-input="true"
          />
          ~
          <PrimeDatePicker
            v-model="availableTimes[day].end"
            placeholder="종료 시간"
            :time-only="true"
            :show-time="true"
            :show-icon="false"
            hour-format="24"
            :manual-input="true"
          />
        </div>
        <div v-else class="disabled-text">미사용</div>
      </div>
    </div>

    <!-- 예약 시간 간격 -->
    <div class="section">
      <p class="section-title">예약 시간 간격</p>
      <div class="radio-horizontal inline-radio">
        <label><input v-model="interval" type="radio" value="10" /> 10분</label>
        <label><input v-model="interval" type="radio" value="30" /> 30분</label>
      </div>
    </div>

    <!-- 식사 시간 설정 -->
    <div class="section">
      <p class="section-title">식사 시간</p>
      <div class="radio-horizontal inline-radio">
        <label><input v-model="lunchOption" type="radio" value="enabled" /> 설정</label>
        <label><input v-model="lunchOption" type="radio" value="disabled" /> 비설정</label>
      </div>
      <div v-if="lunchOption === 'enabled'" class="lunch-time">
        <PrimeDatePicker
          v-model="lunchStart"
          placeholder="시작 시간"
          :time-only="true"
          :show-time="true"
          :show-icon="false"
          hour-format="24"
        />
        <span class="time-separator">~</span>
        <PrimeDatePicker
          v-model="lunchEnd"
          placeholder="종료 시간"
          :time-only="true"
          :show-time="true"
          :show-icon="false"
          hour-format="24"
        />
      </div>
    </div>

    <!-- 하단 버튼 -->
    <div class="button-group">
      <BaseButton type="primary" @click="saveSettings">설정 저장</BaseButton>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';

  const days = ['월', '화', '수', '목', '금', '토', '일'];
  const activeDays = ref(['월', '화', '수', '목', '금']);
  const availableTimes = ref({});
  const interval = ref('10');
  const lunchOption = ref('enabled');
  const lunchStart = ref('12:00');
  const lunchEnd = ref('13:00');

  for (const day of days) {
    availableTimes.value[day] = {
      start: '09:00',
      end: '18:00',
    };
  }

  const toggleDay = day => {
    if (activeDays.value.includes(day)) {
      activeDays.value = activeDays.value.filter(d => d !== day);
    } else {
      activeDays.value.push(day);
    }
  };

  const saveSettings = () => {
    if (activeDays.value.length === 0) {
      alert('최소 하나 이상의 요일을 선택해야 합니다.');
      return;
    }

    for (const day of activeDays.value) {
      const { start, end } = availableTimes.value[day];
      if (!start || !end) {
        alert(`${day}요일의 시작 및 종료 시간을 모두 설정해주세요.`);
        return;
      }
      if (start >= end) {
        alert(`${day}요일의 시작 시간이 종료 시간보다 빠르거나 같아야 합니다.`);
        return;
      }
    }

    if (lunchOption.value === 'enabled') {
      if (!lunchStart.value || !lunchEnd.value) {
        alert('점심시간을 설정하셨다면 시작/종료 시간을 모두 입력해야 합니다.');
        return;
      }
      if (lunchStart.value >= lunchEnd.value) {
        alert('점심 시작 시간은 종료 시간보다 빨라야 합니다.');
        return;
      }
    }

    alert('✅ 설정이 정상적으로 저장되었습니다!');
  };
</script>

<style scoped>
  .reservation-settings {
    padding: 24px;
    max-width: 640px;
  }

  .section {
    margin-bottom: 24px;
  }

  .section-title {
    font-weight: bold;
    margin-bottom: 12px;
  }

  .time-row {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 12px;
  }

  .day-button {
    padding: 6px 12px;
    border: 1px solid var(--color-gray-300);
    border-radius: 6px;
    background: var(--color-neutral-white);
    cursor: pointer;
    min-width: 40px;
  }

  .day-button.active {
    background-color: var(--color-primary-main);
    color: var(--color-neutral-white);
    border-color: var(--color-primary-main);
  }

  .time-range {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .disabled-text {
    color: var(--color-gray-500);
    font-size: 14px;
  }

  .radio-horizontal {
    display: flex;
    gap: 24px;
    align-items: center;
    margin-top: 8px;
    flex-wrap: wrap;
  }

  .inline-radio label {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  .lunch-time {
    display: flex;
    gap: 12px;
    margin-top: 12px;
    align-items: center;
  }

  .button-group {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 32px;
  }

  .time-separator {
    display: inline-block;
    margin: 0 4px;
    font-size: 16px;
    line-height: 36px;
    vertical-align: middle;
  }
</style>
