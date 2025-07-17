<template>
  <div>
    <div class="row row-inline">
      <label class="label-wide">반복</label>
      <BaseForm
        v-model="form.repeat"
        type="select"
        :options="[
          { text: '반복 안함', value: 'none' },
          { text: '매주', value: 'weekly' },
          { text: '매달', value: 'monthly' },
        ]"
        placeholder="반복 선택"
      />
    </div>

    <div v-if="form.repeat === 'weekly'" class="row row-inline">
      <label class="label-wide">요일 선택</label>
      <BaseForm
        v-model="form.weeklyPlan"
        type="select"
        :options="[
          { text: '월요일', value: 'MON' },
          { text: '화요일', value: 'TUE' },
          { text: '수요일', value: 'WED' },
          { text: '목요일', value: 'THU' },
          { text: '금요일', value: 'FRI' },
          { text: '토요일', value: 'SAT' },
          { text: '일요일', value: 'SUN' },
        ]"
        placeholder="요일 선택"
      />
    </div>

    <div v-if="form.repeat === 'monthly'" class="row row-inline">
      <label class="label-wide">일 선택</label>
      <BaseForm
        v-model="form.monthlyPlan"
        type="select"
        :options="
          Array.from({ length: 31 }, (_, i) => ({
            text: `${i + 1}일`,
            value: i + 1,
          }))
        "
        placeholder="일 선택"
      />
    </div>

    <div v-if="form.repeat === 'none'" class="row row-inline">
      <label class="label-wide">날짜</label>
      <div class="flat-flex">
        <PrimeDatePicker
          v-model="form.startDate"
          :show-time="false"
          :clearable="false"
          hour-format="24"
          placeholder="시작 날짜"
          style="width: 190px"
          @update:model-value="updateDuration"
        />
        <PrimeDatePicker
          v-model="form.endDate"
          :show-time="false"
          :clearable="false"
          hour-format="24"
          placeholder="마감 날짜"
          style="width: 190px"
          @update:model-value="updateDuration"
        />
      </div>
    </div>

    <div class="row row-inline">
      <label class="label-wide">시간</label>
      <div class="flat-flex">
        <PrimeDatePicker
          v-model="form.startTime"
          :show-time="true"
          :time-only="true"
          :clearable="false"
          hour-format="24"
          placeholder="시작 시간"
          style="width: 190px"
          @update:model-value="updateDuration"
        />
        <PrimeDatePicker
          v-model="form.endTime"
          :show-time="true"
          :time-only="true"
          :clearable="false"
          hour-format="24"
          placeholder="종료 시간"
          style="width: 190px"
          @update:model-value="updateDuration"
        />
        <BaseForm
          type="text"
          :model-value="form.duration"
          readonly
          placeholder="소요 시간"
          class="input input-time small-width"
        />
        <label class="checkbox-inline">
          <input v-model="form.allDay" type="checkbox" /> 종일
        </label>
      </div>
    </div>

    <div class="row row-inline">
      <label class="label-wide">일정 제목</label>
      <BaseForm v-model="form.title" type="text" style="width: 190px" />
    </div>

    <div class="row row-inline">
      <label class="label-wide">담당자</label>
      <BaseForm
        v-model="form.staff"
        type="select"
        :options="staffOptions"
        class="input"
        style="max-width: 400px"
      />
    </div>

    <div class="row align-top">
      <label class="label-wide">메모</label>
      <BaseForm v-model="form.memo" type="textarea" :rows="3" style="width: 190px" />
    </div>
  </div>
</template>

<script setup>
  import { onMounted, ref, watch } from 'vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import { getStaffList } from '@/features/schedules/api/schedules.js';

  const staffOptions = ref([{ text: '담당자 선택', value: '' }]);

  const form = ref({
    date: null,
    startTime: new Date(0, 0, 0, 0, 0),
    endTime: new Date(0, 0, 0, 0, 0),
    duration: '',
    allDay: false,
    repeat: 'none',
    weeklyPlan: '',
    monthlyPlan: null,
    title: '',
    staff: '',
    memo: '',
  });

  const fetchStaffList = async () => {
    try {
      const res = await getStaffList({ isActive: true });
      staffOptions.value = [
        { text: '담당자 선택', value: '' },
        ...res.map(staff => ({
          text: staff.staffName,
          value: staff.staffId,
        })),
      ];
    } catch (e) {
      console.error('담당자 목록 조회 실패:', e);
    }
  };
  onMounted(() => {
    fetchStaffList();
  });

  const updateDuration = () => {
    const startDate = form.value.startDate;
    const endDate = form.value.endDate;
    const startTime = form.value.startTime;
    const endTime = form.value.endTime;

    if (
      startDate instanceof Date &&
      endDate instanceof Date &&
      startTime instanceof Date &&
      endTime instanceof Date
    ) {
      // 1. 날짜+시간 조합
      const start = new Date(
        startDate.getFullYear(),
        startDate.getMonth(),
        startDate.getDate(),
        startTime.getHours(),
        startTime.getMinutes()
      );

      const end = new Date(
        endDate.getFullYear(),
        endDate.getMonth(),
        endDate.getDate(),
        endTime.getHours(),
        endTime.getMinutes()
      );

      // 2. 유효성 및 차이 계산
      if (end > start) {
        const diff = end.getTime() - start.getTime();
        const totalMinutes = Math.floor(diff / 1000 / 60);
        const hours = String(Math.floor(totalMinutes / 60)).padStart(2, '0');
        const minutes = String(totalMinutes % 60).padStart(2, '0');
        form.value.duration = `${hours}:${minutes}`;
      } else {
        form.value.duration = '';
      }
    } else {
      form.value.duration = '';
    }
  };

  watch(
    () => form.value.allDay,
    val => {
      const startDate = form.value.startDate instanceof Date ? form.value.startDate : new Date();
      const endDate = form.value.endDate instanceof Date ? form.value.endDate : new Date();

      if (val) {
        form.value.startTime = new Date(
          startDate.getFullYear(),
          startDate.getMonth(),
          startDate.getDate(),
          0,
          0
        );
        form.value.endTime = new Date(
          endDate.getFullYear(),
          endDate.getMonth(),
          endDate.getDate(),
          23,
          59
        );

        updateDuration();
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
  watch([() => form.value.startDate, () => form.value.endDate], () => {
    if (form.value.allDay) {
      const startDate = form.value.startDate instanceof Date ? form.value.startDate : new Date();
      const endDate = form.value.endDate instanceof Date ? form.value.endDate : new Date();

      form.value.startTime = new Date(
        startDate.getFullYear(),
        startDate.getMonth(),
        startDate.getDate(),
        0,
        0
      );
      form.value.endTime = new Date(
        endDate.getFullYear(),
        endDate.getMonth(),
        endDate.getDate(),
        23,
        59
      );
      updateDuration();
    }
  });
  watch(
    () => form.value.repeat,
    val => {
      if (val !== 'none') {
        form.value.duration = '';
      } else {
        updateDuration();
      }
    }
  );

  defineExpose({ form });
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

  .form-group {
    display: flex;
    align-items: center;
    margin: 0;
    padding: 0;
    width: 200px !important;
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
