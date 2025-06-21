<template>
  <div>
    <!-- 일정 날짜 및 시간 -->
    <div class="row row-inline">
      <label class="label-wide">일정 날짜</label>
      <div class="flat-flex">
        <!-- 날짜는 항상 표시 -->
        <PrimeDatePicker
          v-model="form.date"
          :show-time="false"
          :show-button-bar="true"
          :clearable="false"
          hour-format="24"
          placeholder="날짜 선택"
          style="width: 160px"
        />
        <PrimeDatePicker
          v-model="form.startTime"
          :show-time="true"
          :time-only="true"
          :clearable="false"
          hour-format="24"
          placeholder="시작 시간"
          @update:model-value="updateDuration"
        />
        <PrimeDatePicker
          v-model="form.endTime"
          :show-time="true"
          :time-only="true"
          :clearable="false"
          hour-format="24"
          placeholder="종료 시간"
          @update:model-value="updateDuration"
        />

        <!-- 소요 시간 -->
        <input
          :value="form.duration"
          type="text"
          class="input input-time small-width"
          placeholder="소요 시간"
          readonly
        />

        <!-- 종일 체크 -->
        <label class="checkbox-inline">
          <input v-model="form.allDay" type="checkbox" /> 종일
        </label>
      </div>
    </div>

    <!-- 반복 여부 -->
    <div class="row row-inline">
      <label class="label-wide">반복</label>
      <select v-model="form.repeat" class="input">
        <option value="none">반복 안함</option>
        <option value="weekly">매주</option>
        <option value="monthly">매달</option>
      </select>
    </div>

    <!-- 일정 제목 -->
    <div class="row row-inline">
      <label class="label-wide">일정 제목</label>
      <input v-model="form.title" type="text" class="input" />
    </div>

    <!-- 담당자 -->
    <div class="row row-inline">
      <label class="label-wide">담당자</label>
      <select v-model="form.staff" class="input">
        <option value="">담당자</option>
        <option value="김이글">김이글</option>
        <option value="박이글">박이글</option>
      </select>
    </div>

    <!-- 메모 -->
    <div class="row align-top">
      <label class="label-wide">메모</label>
      <textarea v-model="form.memo" rows="3" class="input" />
    </div>
  </div>
</template>

<script setup>
  import { ref, watch } from 'vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';

  const form = ref({
    date: null,
    startTime: null,
    endTime: null,
    duration: '',
    allDay: false,
    repeat: 'none',
    title: '',
    staff: '',
    memo: '',
  });

  const updateDuration = () => {
    const start = form.value.startTime;
    const end = form.value.endTime;

    if (start instanceof Date && end instanceof Date && end > start) {
      const diff = end - start;
      const mins = Math.floor(diff / 60000);
      const hours = String(Math.floor(mins / 60)).padStart(2, '0');
      const minutes = String(mins % 60).padStart(2, '0');
      form.value.duration = `${hours}:${minutes}`;
    } else {
      form.value.duration = '';
    }
  };

  watch(
    () => form.value.allDay,
    val => {
      const base = form.value.date instanceof Date ? form.value.date : new Date();
      const y = base.getFullYear();
      const m = base.getMonth();
      const d = base.getDate();

      if (val) {
        form.value.startTime = new Date(y, m, d, 0, 0);
        form.value.endTime = new Date(y, m, d, 23, 59);
        form.value.duration = '23:59';
      } else {
        form.value.startTime = null;
        form.value.endTime = null;
        form.value.duration = '';
      }
    }
  );
  watch([() => form.value.startTime, () => form.value.endTime], ([start, end]) => {
    if (!form.value.allDay) return;

    const isStart00 = start instanceof Date && start.getHours() === 0 && start.getMinutes() === 0;
    const isEnd2359 = end instanceof Date && end.getHours() === 23 && end.getMinutes() === 59;

    if (!(isStart00 && isEnd2359)) {
      form.value.allDay = false;
    }
  });
</script>

<style scoped>
  .row {
    display: flex;
    align-items: flex-start;
    margin-bottom: 14px;
    width: 100%;
  }

  .row-inline {
    flex-wrap: nowrap;
    align-items: center;
  }

  .row.align-top {
    align-items: flex-start;
  }

  .label-wide {
    width: 120px;
    flex-shrink: 0;
    font-weight: bold;
    color: var(--color-gray-800);
    line-height: 1.5;
    white-space: nowrap;
  }

  .row input,
  .row textarea,
  .row select {
    font-size: 14px;
    line-height: 1.5;
    padding: 6px 8px;
    width: 100%;
    max-width: 400px;
    border: 1px solid var(--color-gray-300);
    border-radius: 4px;
    box-sizing: border-box;
    background-color: var(--color-neutral-white);
    color: var(--color-neutral-dark);
  }

  .input-date {
    width: 160px !important;
  }

  .input-time {
    width: 110px;
  }

  .small-width {
    width: 110px !important;
  }

  .big-width {
    width: 160px !important;
  }

  .row textarea {
    resize: vertical;
  }

  .flat-flex {
    display: flex;
    flex: 1;
    gap: 8px;
    flex-wrap: nowrap;
    align-items: center;
  }

  .checkbox-inline {
    display: flex;
    align-items: center;
    gap: 4px;
    white-space: nowrap;
    color: var(--color-neutral-dark);
  }
</style>
