<template>
  <div>
    <!-- 반복 여부 -->
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
      />
    </div>
    <!-- 휴무 날짜 (한 줄) -->
    <div v-if="form.repeat === 'none'" class="row row-inline">
      <label class="label-wide">휴무 날짜</label>
      <div class="flat-flex">
        <PrimeDatePicker
          v-model="form.date"
          :show-time="false"
          :show-button-bar="true"
          :clearable="false"
          hour-format="24"
          placeholder="날짜를 선택하세요"
          style="width: 190px"
        />
      </div>
    </div>

    <div v-if="form.repeat === 'weekly'" class="row row-inline">
      <label class="label-wide">요일 선택</label>
      <BaseForm
        v-model="form.weeklyLeave"
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
      />
    </div>

    <div v-if="form.repeat === 'monthly'" class="row row-inline">
      <label class="label-wide">일 선택</label>
      <BaseForm
        v-model="form.monthlyLeave"
        type="select"
        :options="
          Array.from({ length: 31 }, (_, i) => ({
            text: `${i + 1}일`,
            value: i + 1,
          }))
        "
      />
    </div>

    <!-- 휴무 제목 -->
    <div class="row row-inline">
      <label class="label-wide">휴무 제목</label>
      <BaseForm v-model="form.title" type="text" style="width: 190px" />
    </div>

    <!-- 담당자 -->
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

    <!-- 메모 -->
    <div class="row align-top">
      <label class="label-wide">메모</label>
      <BaseForm v-model="form.memo" type="textarea" :rows="3" style="width: 190px" />
    </div>
  </div>
</template>

<script setup>
  import { onMounted, ref } from 'vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import { getStaffList } from '@/features/schedules/api/schedules.js';

  const form = ref({
    date: '',
    repeat: 'none',
    weeklyLeave: '',
    monthlyLeave: '',
    title: '',
    staffId: '',
    memo: '',
  });
  const staffOptions = ref([{ text: '담당자 선택', value: '' }]);

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

  .row label {
    flex-shrink: 0;
    font-weight: 600;
    color: var(--color-gray-800);
    line-height: 1.5;
    white-space: nowrap;
  }

  .label-wide {
    width: 120px;
  }

  .row input,
  .row textarea,
  .row select {
    font-size: 14px;
    line-height: 1.5;
    padding: 6px 10px;
    width: 100%;
    max-width: 400px;
    border: 1px solid var(--color-gray-300);
    border-radius: 6px;
    background-color: var(--color-neutral-white);
    color: var(--color-gray-900);
    box-sizing: border-box;
    transition:
      border-color 0.2s ease,
      box-shadow 0.2s ease;
  }

  .row input:focus,
  .row textarea:focus,
  .row select:focus {
    outline: none;
    border-color: var(--color-primary-main);
    box-shadow: 0 0 0 2px rgba(100, 149, 237, 0.2);
  }

  .row textarea {
    resize: vertical;
  }

  .form-group {
    display: flex;
    align-items: center;
    margin: 0;
    padding: 0;
    width: 200px !important;
  }

  .flat-flex {
    display: flex;
    flex: 1;
    gap: 8px;
    flex-wrap: nowrap;
    align-items: center;
  }
</style>
