<template>
  <div>
    <div v-if="modelValue && edited && edited.id" class="overlay" @click.self="close">
      <div class="modal-panel">
        <div class="modal-header">
          <div>
            <h1>등록된 스케줄</h1>
            <p class="type-label">{{ planTypeLabel }}</p>
          </div>
          <button class="close-btn" @click="close">×</button>
        </div>

        <div class="modal-body">
          <div class="left-detail">
            <div class="row row-select">
              <label>반복</label>
              <div class="form-control-wrapper">
                <BaseForm
                  v-if="isEditMode"
                  v-model="edited.repeat"
                  type="select"
                  :options="[
                    { text: '반복 안함', value: 'none' },
                    { text: '매달 반복', value: 'monthly' },
                    { text: '요일 반복', value: 'weekly' },
                  ]"
                  placeholder="반복 주기 선택"
                  style="width: 200px"
                />

                <span v-else>{{ planTypeLabel }}</span>
              </div>
            </div>

            <div class="row">
              <label>제목</label>
              <span v-if="!isEditMode">{{ edited.title }}</span>
              <BaseForm v-else v-model="edited.title" type="text" />
            </div>

            <div class="row row-select">
              <label>담당자</label>
              <div class="form-control-wrapper">
                <BaseForm
                  v-if="isEditMode"
                  v-model="edited.staffName"
                  type="text"
                  :options="staffOptions"
                  placeholder="담당자 선택"
                  :disabled="true"
                />
                <span v-else>{{ edited.staffName || '미지정' }}</span>
              </div>
            </div>

            <div class="row">
              <label>날짜 및 시간</label>
              <div class="date-inline">
                <template v-if="isEditMode">
                  <!-- 날짜 -->
                  <template v-if="edited.repeat === 'none'">
                    <PrimeDatePicker
                      v-model="edited.date"
                      :clearable="false"
                      :show-time="false"
                      :show-button-bar="true"
                      placeholder="시작 날짜"
                      :style="{ width: '200px' }"
                    />
                    <PrimeDatePicker
                      v-model="edited.endDate"
                      :clearable="false"
                      :show-time="false"
                      :show-button-bar="true"
                      placeholder="종료 날짜"
                      :style="{ width: '200px' }"
                    />
                  </template>

                  <template v-else-if="edited.repeat === 'monthly'">
                    <BaseForm
                      v-model="edited.monthlyDay"
                      type="select"
                      :options="
                        Array.from({ length: 31 }, (_, i) => ({ text: `${i + 1}일`, value: i + 1 }))
                      "
                      placeholder="매달 반복일 선택"
                      style="width: 200px"
                    />
                  </template>

                  <template v-else-if="edited.repeat === 'weekly'">
                    <BaseForm
                      v-model="edited.weeklyDay"
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
                      placeholder="반복 요일 선택"
                      style="width: 200px"
                    />
                  </template>

                  <!-- 시간 -->
                  <PrimeDatePicker
                    v-model="edited.startTime"
                    :clearable="false"
                    :show-time="true"
                    :time-only="true"
                    hour-format="24"
                    placeholder="시작 시간"
                    :style="{ width: '140px' }"
                  />
                  <PrimeDatePicker
                    v-model="edited.endTime"
                    :clearable="false"
                    :show-time="true"
                    :time-only="true"
                    hour-format="24"
                    placeholder="종료 시간"
                    :style="{ width: '140px' }"
                  />
                  <span>소요 시간:</span>
                  <BaseForm v-model="durationText" type="text" readonly placeholder="00:00" />
                  <label class="all-day-checkbox">
                    <input v-model="edited.allDay" type="checkbox" @change="handleAllDayToggle" />
                    <span>종일</span>
                  </label>
                </template>

                <template v-else>
                  <span v-if="edited.timeRange">
                    {{ edited.timeRange }}
                    <span v-if="edited.duration"> ({{ edited.duration }} 소요) </span>
                  </span>
                  <span v-else>시간 정보 없음</span>
                </template>
              </div>
            </div>

            <div class="row">
              <label>메모</label>
              <span v-if="!isEditMode">{{ edited.memo }}</span>
              <BaseForm v-else v-model="edited.memo" type="textarea" rows="3" />
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <BaseButton type="error" @click="close">닫기</BaseButton>
          <template v-if="isEditMode">
            <BaseButton type="primary" @click="handleSave">저장</BaseButton>
          </template>
          <template v-else>
            <div class="action-dropdown">
              <BaseButton type="primary" @click="toggleMenu">수정 / 삭제</BaseButton>
              <ul v-if="showMenu" class="dropdown-menu">
                <li @click="handleEdit">수정하기</li>
                <li @click="openDeleteConfirm">삭제하기</li>
              </ul>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
  <BaseToast ref="toast" />
  <BaseConfirm
    v-model="showConfirmModal"
    title="일정 삭제"
    message="정말 이 일정을 삭제하시겠습니까?"
    confirm-text="삭제"
    cancel-text="취소"
    confirm-type="error"
    icon-type="error"
    @confirm="handleDelete"
  />
  <BaseConfirm
    v-model="showEditConfirm"
    title="변경 내용을 저장하시겠습니까?"
    message="입력한 정보로 휴무를 수정하시겠습니까?"
    confirm-text="수정"
    cancel-text="취소"
    confirm-type="primary"
    icon-type="info"
    @confirm="confirmEdit"
  />
</template>

<script setup>
  import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import {
    fetchScheduleDetail,
    getStaffList,
    deletePlans,
    updatePlan,
    updateRegularPlan,
    switchScheduleType,
  } from '@/features/schedules/api/schedules.js';
  import BaseConfirm from '@/components/common/BaseConfirm.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import dayjs from 'dayjs';

  const staffOptions = ref([]);
  const fetchStaffList = async () => {
    try {
      const res = await getStaffList({ isActive: true });
      staffOptions.value = [
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

  const durationText = ref('');
  const toast = ref(null);
  const emit = defineEmits(['update:modelValue']);
  const isEditMode = ref(false);
  const showMenu = ref(false);
  const edited = ref({});
  const backup = ref({});
  const showEditConfirm = ref(false);
  const showConfirmModal = ref(false);

  const handleSave = () => {
    showEditConfirm.value = true;
  };

  const confirmEdit = async () => {
    await saveEdit();
  };

  const openDeleteConfirm = () => {
    showMenu.value = false;
    showConfirmModal.value = true;
  };
  defineOptions({
    name: 'PlanDetailModal',
  });

  const props = defineProps({
    modelValue: Boolean,
    id: [String, Number],
    type: String,
  });

  watch(
    [() => edited.value.staffName, staffOptions],
    ([name, options]) => {
      if (!name || !options.length) return;

      const matched = options.find(opt => opt.text === name);
      if (matched) {
        edited.value.staffId = matched.value;
      }
    },
    { immediate: true }
  );

  const close = () => {
    emit('update:modelValue', false);
    isEditMode.value = false;
    showMenu.value = false;
    edited.value = {};
  };

  const toggleMenu = () => (showMenu.value = !showMenu.value);
  const toDate = str => {
    const today = new Date();
    const [h, m] = str.split(':').map(Number);
    today.setHours(h, m, 0, 0);
    return new Date(today);
  };

  const handleEdit = () => {
    isEditMode.value = true;
    showMenu.value = false;

    const { date, timeRange, duration, repeat = 'none', ...rest } = edited.value;

    const [startStr, endStr] = (timeRange || '').split(/[-~]/).map(str => str.trim());

    const toTime = str => {
      if (!str || !/^\d{2}:\d{2}$/.test(str)) return null;
      const [h, m] = str.split(':').map(Number);
      const base = new Date();
      base.setHours(h, m, 0, 0);
      return base;
    };

    // 기본값 00:00
    const defaultStart = new Date();
    defaultStart.setHours(0, 0, 0, 0);

    const defaultEnd = new Date();
    defaultEnd.setHours(1, 0, 0, 0);

    const parsedStart = toTime(startStr) || defaultStart;
    const parsedEnd = toTime(endStr) || defaultEnd;

    edited.value = {
      ...rest,
      type: props.type === 'regular_plan' ? 'regular_plan' : 'plan',
      date: repeat === 'none' ? new Date(date) : date,
      endDate: repeat === 'none' ? (rest.endDate ? new Date(rest.endDate) : new Date(date)) : null,
      startTime: parsedStart,
      endTime: parsedEnd,
      duration,
      timeRange,
      allDay: timeRange === '00:00 - 23:59',
      repeat,
    };

    backup.value = {
      ...edited.value,
      type: edited.value.type,
      startTime: new Date(edited.value.startTime),
      endTime: new Date(edited.value.endTime),
      date: edited.value.date ? new Date(edited.value.date) : null,
      endDate: edited.value.endDate ? new Date(edited.value.endDate) : null,
    };
  };

  const handleDelete = async () => {
    showMenu.value = false;

    try {
      const deleteRequest = [
        {
          id: edited.value.id,
          type: edited.value.type?.toUpperCase() === 'REGULAR_PLAN' ? 'REGULAR_PLAN' : 'PLAN',
        },
      ];

      await deletePlans(deleteRequest);

      toast.value?.success('삭제가 완료되었습니다.');
      close();
    } catch (error) {
      console.error('삭제 실패:', error);
      toast.value?.error('삭제 중 오류가 발생했습니다.');
    }
  };
  const getDayOfWeekCode = date => {
    const days = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
    return days[new Date(date).getDay()];
  };
  const saveEdit = async () => {
    try {
      const formatDateTime = (date, time) =>
        dayjs(`${dayjs(date).format('YYYY-MM-DD')}T${dayjs(time).format('HH:mm')}`).format(
          'YYYY-MM-DDTHH:mm:ss'
        );

      const formatTime = d => dayjs(d).format('HH:mm');
      const isTypeChanged = edited.value.type !== backup.value.type;

      if (isTypeChanged) {
        const request = {
          staffId: edited.value.staffId,
          fromType: backup.value.type === 'regular_plan' ? 'REGULAR_PLAN' : 'PLAN',
          fromId: edited.value.id,
          toType: edited.value.type === 'regular_plan' ? 'REGULAR_PLAN' : 'PLAN',
          planRequest: null,
          regularPlanRequest: null,
        };

        if (request.toType === 'PLAN') {
          request.planRequest = {
            staffId: edited.value.staffId,
            planTitle: edited.value.title,
            planMemo: edited.value.memo,
            planStartAt: formatDateTime(edited.value.date, edited.value.startTime),
            planEndAt: formatDateTime(edited.value.endDate, edited.value.endTime),
          };
        } else {
          request.regularPlanRequest = {
            staffId: edited.value.staffId,
            regularPlanTitle: edited.value.title,
            regularPlanMemo: edited.value.memo,
            regularPlanStartAt: formatTime(edited.value.startTime),
            regularPlanEndAt: formatTime(edited.value.endTime),
            weeklyPlan: edited.value.repeat === 'weekly' ? edited.value.weeklyDay : null,
            monthlyPlan: edited.value.repeat === 'monthly' ? edited.value.monthlyDay : null,
          };
        }

        await switchScheduleType(request);
        toast.value?.success('일정 타입이 변경되었습니다.');
        close();
        return;
      }

      // 일반 수정
      const payload = {
        staffId: edited.value.staffId,
      };

      if (edited.value.type === 'plan') {
        Object.assign(payload, {
          planTitle: edited.value.title,
          planMemo: edited.value.memo,
          planStartAt: formatDateTime(edited.value.date, edited.value.startTime),
          planEndAt: formatDateTime(edited.value.endDate, edited.value.endTime),
        });

        await updatePlan(edited.value.id, payload);
      } else if (edited.value.type === 'regular_plan') {
        Object.assign(payload, {
          regularPlanTitle: edited.value.title,
          regularPlanMemo: edited.value.memo,
          regularPlanStartAt: formatTime(edited.value.startTime),
          regularPlanEndAt: formatTime(edited.value.endTime),
          weeklyPlan: edited.value.repeat === 'weekly' ? edited.value.weeklyDay : null,
          monthlyPlan: edited.value.repeat === 'monthly' ? edited.value.monthlyDay : null,
        });

        await updateRegularPlan(edited.value.id, payload);
      }

      toast.value?.success('일정이 수정되었습니다.');
      isEditMode.value = false;
      close();
    } catch (error) {
      console.error('수정 오류:', error);
      toast.value?.error('일정 수정 중 오류가 발생했습니다.');
    }
  };

  const handleAllDayToggle = () => {
    const base = new Date();
    base.setSeconds(0);
    base.setMilliseconds(0);

    if (edited.value.allDay) {
      const start = new Date(base);
      start.setHours(0, 0, 0);
      const end = new Date(base);
      end.setHours(23, 59, 0);

      edited.value.startTime = start;
      edited.value.endTime = end;
    } else {
      edited.value.startTime = new Date(backup.value.startTime);
      edited.value.endTime = new Date(backup.value.endTime);
    }
  };

  watch(
    () => edited.value.repeat,
    repeat => {
      if (repeat === 'none') {
        if (!(edited.value.date instanceof Date)) {
          edited.value.date = new Date();
          edited.value.startDate = new Date();
        }
        if (!(edited.value.endDate instanceof Date)) {
          edited.value.endDate = new Date();
        }
        edited.value.type = 'plan';
      } else {
        durationText.value = '';
        edited.value.type = 'regular_plan';
      }
    }
  );

  watch(
    [
      () => edited.value.date,
      () => edited.value.endDate,
      () => edited.value.startTime,
      () => edited.value.endTime,
      () => edited.value.repeat,
    ],
    ([startDate, endDate, startTime, endTime, repeat]) => {
      if (!startTime || !endTime) {
        durationText.value = '';
        return;
      }

      let start, end;

      if (repeat === 'none') {
        if (!(startDate instanceof Date) || !(endDate instanceof Date)) {
          durationText.value = '';
          return;
        }

        start = dayjs(startDate)
          .hour(dayjs(startTime).hour())
          .minute(dayjs(startTime).minute())
          .second(0);

        end = dayjs(endDate).hour(dayjs(endTime).hour()).minute(dayjs(endTime).minute()).second(0);
      } else {
        const dummyDate = '2000-01-01';
        start = dayjs(`${dummyDate} ${dayjs(startTime).format('HH:mm:ss')}`);
        end = dayjs(`${dummyDate} ${dayjs(endTime).format('HH:mm:ss')}`);
      }

      if (!start.isValid() || !end.isValid()) {
        durationText.value = '';
        return;
      }

      const diffMin = Math.round(end.diff(start, 'minute', true));
      if (diffMin <= 0) {
        durationText.value = '';
        return;
      }

      const hours = Math.floor(diffMin / 60);
      const minutes = diffMin % 60;
      durationText.value = `${hours.toString().padStart(2, '0')}:${minutes
        .toString()
        .padStart(2, '0')}`;
    },
    { immediate: true }
  );

  watch(
    () => [props.id, props.type, props.modelValue],
    async ([id, type, visible]) => {
      if (!id || !type || !visible) return;

      const { data } = await fetchScheduleDetail(type, id);
      const d = data.data;

      const convert = (startStr, endStr, showDate = false) => {
        const startDate = new Date(startStr);
        const endDate = new Date(endStr);

        if (isNaN(startDate) || isNaN(endDate)) {
          return {
            timeRange: '시간 정보 없음',
            duration: '',
            startDate: null,
            endDate: null,
          };
        }

        const startTimeStr = startDate.toTimeString().slice(0, 5);
        const endTimeStr = endDate.toTimeString().slice(0, 5);

        const toYMD = date =>
          `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
            date.getDate()
          ).padStart(2, '0')}`;

        const timeRange = showDate
          ? toYMD(startDate) === toYMD(endDate)
            ? `${toYMD(startDate)} ${startTimeStr} - ${endTimeStr}`
            : `${toYMD(startDate)} ${startTimeStr} ~ ${toYMD(endDate)} ${endTimeStr}`
          : `${startTimeStr} - ${endTimeStr}`;

        const diffMs = endDate - startDate;
        const minutes = Math.floor(diffMs / 60000);
        const hours = Math.floor(minutes / 60);
        const mins = minutes % 60;

        const duration = `${hours ? `${hours}시간` : ''} ${mins ? `${mins}분` : ''}`.trim();

        return {
          timeRange,
          duration,
          startTime: startDate,
          endTime: endDate,
        };
      };

      let result = {
        id,
        type,
        title: d.title ?? d.leaveTitle,
        staffName: d.staffName,
        staffId: d.staffId,
        memo: d.memo,
        allDay: false,
        repeat: 'none',
        startTime: null,
        endTime: null,
      };

      const { timeRange, duration, startTime, endTime } = convert(
        d.startAt,
        d.endAt,
        type !== 'regular_plan'
      );
      result.timeRange = timeRange;
      result.duration = duration;
      result.startTime = startTime;
      result.endTime = endTime;

      if (type === 'regular_plan') {
        result.type = 'regular_plan';
        let repeatText = '';

        if (d.weeklyPlan) {
          // 요일 코드 → 한글
          const weekdayMap = {
            MON: '월요일',
            TUE: '화요일',
            WED: '수요일',
            THU: '목요일',
            FRI: '금요일',
            SAT: '토요일',
            SUN: '일요일',
          };
          repeatText = `매주 ${weekdayMap[d.weeklyPlan]}`;
        } else if (d.monthlyPlan) {
          repeatText = `매달 ${d.monthlyPlan}일`;
        }

        // 시간 문자열 파싱
        const parseTime = t => {
          if (!t || !/^\d{2}:\d{2}/.test(t)) return '';
          return t.slice(0, 5); // HH:mm
        };

        const startTimeStr = parseTime(d.startAt);
        const endTimeStr = parseTime(d.endAt);

        result.repeat = d.weeklyPlan ? 'weekly' : 'monthly';
        result.weeklyDay = d.weeklyPlan || null;
        result.monthlyDay = d.monthlyPlan || null;
        result.timeRange = `${repeatText} ${startTimeStr} - ${endTimeStr}`;
        result.duration = ''; // 필요시 소요시간 계산 로직 추가 가능
      } else {
        // 기존 단기 일정 처리 로직 그대로
        result.type = 'plan';
        result.date = new Date(d.startAt);
        result.endDate = new Date(d.endAt);

        const { timeRange, duration, startTime, endTime } = convert(
          d.startAt,
          d.endAt,
          type !== 'regular_plan'
        );
        result.timeRange = timeRange;
        result.duration = duration;
        result.startTime = startTime;
        result.endTime = endTime;
      }

      edited.value = result;
    },
    { immediate: true }
  );

  const handleEsc = e => e.key === 'Escape' && close();
  onMounted(() => window.addEventListener('keydown', handleEsc));
  onBeforeUnmount(() => window.removeEventListener('keydown', handleEsc));

  const planTypeLabel = computed(() => {
    return edited.value?.type === 'regular_plan' ? '정기 일정' : '일정';
  });
</script>

<style scoped>
  .overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100vh;
    background-color: rgba(0, 0, 0, 0.3);
    z-index: 1000;
  }

  .modal-panel {
    position: fixed;
    top: 0;
    left: 240px;
    width: calc(100% - 240px);
    height: 100vh;
    background-color: var(--color-neutral-white);
    display: flex;
    flex-direction: column;
    padding: 24px;
    overflow-y: auto;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
  }

  .modal-header h1 {
    font-size: 20px;
    font-weight: bold;
    color: var(--color-text-primary);
  }

  .close-btn {
    background: none;
    border: none;
    font-size: 24px;
    cursor: pointer;
    color: var(--color-text-primary);
  }

  .modal-body {
    display: flex;
    gap: 32px;
    flex: 1;
  }

  .left-detail {
    flex: 1;
  }

  .row {
    display: flex;
    align-items: flex-start;
    margin-bottom: 14px;
  }

  .row label {
    width: 100px;
    font-weight: bold;
    color: var(--color-gray-800);
    padding-top: 6px;
    line-height: 1.5;
  }

  .row span,
  .row input,
  .row textarea {
    font-size: 14px;
    line-height: 1.5;
    padding: 6px 8px;
    vertical-align: middle;
    width: 100%;
    max-width: 400px;
    box-sizing: border-box;
    color: var(--color-text-primary);
    background-color: var(--color-neutral-white);
  }

  .row input,
  .row textarea {
    border: 1px solid var(--color-gray-300);
    border-radius: 4px;
  }

  .row textarea {
    resize: vertical;
  }

  .form-control-wrapper {
    flex: 1;
    display: flex;
    align-items: flex-start;
  }

  .form-control-wrapper :deep(.input) {
    width: 100%;
    max-width: 300px;
  }

  .right-box {
    width: 200px;
    padding: 12px;
    border-left: 1px solid var(--color-gray-200);
  }

  .right-box p {
    margin-bottom: 16px;
    font-weight: 500;
    color: var(--color-gray-700);
  }

  .modal-footer {
    margin-top: 32px;
    display: flex;
    gap: 12px;
    justify-content: flex-end;
  }

  .action-dropdown {
    position: relative;
  }

  .dropdown-menu {
    position: absolute;
    bottom: 40px;
    right: 0;
    background-color: var(--color-neutral-white);
    border: 1px solid var(--color-gray-300);
    border-radius: 6px;
    list-style: none;
    padding: 8px 0;
    width: 120px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.15);
    z-index: 10;
  }

  .dropdown-menu li {
    padding: 8px 12px;
    cursor: pointer;
    color: var(--color-gray-800);
  }

  .dropdown-menu li:hover {
    background-color: var(--color-gray-100);
  }

  .type-label {
    margin-top: 4px;
    font-size: 18px;
    font-weight: 500;
    color: var(--color-gray-500);
  }

  .date-inline {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: nowrap;
  }

  .date-inline input[type='date'],
  .date-inline input[type='text'],
  .date-inline select {
    font-size: 14px;
    padding: 6px 8px;
    border: 1px solid var(--color-gray-300);
    border-radius: 4px;
    background-color: var(--color-neutral-white);
    color: var(--color-gray-900);
    min-width: 120px;
    height: 32px;
  }

  .all-day-checkbox {
    display: flex;
    align-items: center;
    white-space: nowrap;
    color: var(--color-gray-700);
  }

  .repeat-inline {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: nowrap;
  }

  .repeat-inline :deep(.input) {
    display: inline-block;
    width: auto;
    min-width: 160px;
  }

  .repeat-description {
    font-size: 14px;
    color: var(--color-gray-500);
    white-space: nowrap;
  }

  .date-inline span,
  .repeat-inline span {
    white-space: nowrap;
    width: auto !important;
    max-width: none;
    color: var(--color-gray-800);
  }

  .duration-input {
    font-size: 14px;
    padding: 6px 8px;
    border: 1px solid var(--color-gray-300);
    border-radius: 4px;
    background-color: var(--color-gray-100);
    color: var(--color-gray-800);
    min-width: 100px;
    height: 32px;
    white-space: nowrap;
  }

  .form-group {
    display: flex;
    align-items: center;
    margin: 0;
    padding: 0;
    width: 200px !important;
  }
</style>
