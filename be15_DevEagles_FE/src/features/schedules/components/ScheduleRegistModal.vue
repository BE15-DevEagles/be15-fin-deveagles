<template>
  <div class="overlay" @click.self="close">
    <div class="modal-panel">
      <!-- 헤더 -->
      <div class="modal-header">
        <h1>스케줄 등록</h1>
        <button class="close-btn" @click="close">&times;</button>
      </div>

      <!-- 바디 -->
      <div class="modal-body">
        <div class="left-detail">
          <div class="tab-buttons">
            <BaseButton :outline="tab !== 'reservation'" @click="tab = 'reservation'"
              >예약</BaseButton
            >
            <BaseButton :outline="tab !== 'plan'" @click="tab = 'plan'">일정</BaseButton>
            <BaseButton :outline="tab !== 'leave'" @click="tab = 'leave'">휴무</BaseButton>
          </div>
          <component :is="currentTabComponent" ref="formRef" />
        </div>
      </div>

      <!-- 푸터 -->
      <div class="modal-footer">
        <BaseButton outline @click="close">등록 취소</BaseButton>
        <BaseButton @click="submit">등록</BaseButton>
      </div>
    </div>
  </div>

  <BaseConfirm
    v-model="showConfirm"
    title="예약 확인"
    message="해당 예약을 등록하시겠습니까?"
    confirm-text="등록"
    cancel-text="취소"
    @confirm="handleConfirm"
  />

  <BaseToast ref="toast" />
</template>

<script setup>
  import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import ReservationForm from './ReservationForm.vue';
  import PlanForm from './PlanForm.vue';
  import LeaveForm from './LeaveForm.vue';
  import {
    createReservation,
    createLeave,
    createRegularLeave,
    createPlan,
    createRegularPlan,
  } from '@/features/schedules/api/schedules.js';
  import BaseConfirm from '@/components/common/BaseConfirm.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import dayjs from 'dayjs';

  const showConfirm = ref(false);
  let confirmCallback = null;

  const formRef = ref();
  const toast = ref(null);

  const props = defineProps({
    modelValue: { type: Boolean, required: true },
    defaultTab: { type: String, default: 'reservation' },
  });

  const emit = defineEmits(['update:modelValue']);

  const tab = ref(props.defaultTab);

  const currentTabComponent = computed(() => {
    switch (tab.value) {
      case 'plan':
        return PlanForm;
      case 'leave':
        return LeaveForm;
      default:
        return ReservationForm;
    }
  });
  const toDateTimeString = (date, time) => {
    if (!(date instanceof Date) || !(time instanceof Date)) return '';

    return dayjs(date)
      .set('hour', time.getHours())
      .set('minute', time.getMinutes())
      .set('second', 0)
      .set('millisecond', 0)
      .format('YYYY-MM-DDTHH:mm:ss');
  };
  const submit = async () => {
    const data = formRef.value?.form;

    if (!data) {
      console.log('formRef or form is undefined');
      return;
    }

    let isValid = false;
    let requiredFields = [];

    if (tab.value === 'reservation') {
      requiredFields = [data.staffId, data.date, data.startTime, data.endTime];
      isValid = requiredFields.every(Boolean);
      if (isValid && new Date(data.startTime) >= new Date(data.endTime)) {
        toast.value?.error('시작 시간이 종료 시간보다 늦을 수 없습니다.');
        return;
      }
      if (!isValid) {
        toast.value?.error('담당자, 날짜, 시작/종료 시간을 모두 입력해주세요.');
        return;
      }
    } else if (tab.value === 'leave') {
      requiredFields = [data.staff, data.title];
      if (data.repeat === 'none') {
        requiredFields.push(data.date);
      } else if (data.repeat === 'weekly') {
        requiredFields.push(data.weeklyLeave);
      } else if (data.repeat === 'monthly') {
        requiredFields.push(data.monthlyLeave);
      }

      isValid = requiredFields.every(Boolean);
      if (!isValid) {
        toast.value?.error('모든 필수 항목을 입력해주세요.');
        return;
      }
    } else if (tab.value === 'plan') {
      requiredFields = [data.staff, data.title];

      if (data.repeat === 'none') {
        requiredFields.push(data.startDate, data.endDate, data.startTime, data.endTime);
      } else if (data.repeat === 'weekly') {
        requiredFields.push(data.weeklyPlan);
      } else if (data.repeat === 'monthly') {
        requiredFields.push(data.monthlyPlan);
      }

      isValid = requiredFields.every(Boolean);
      if (isValid && data.repeat === 'none') {
        const start = new Date(
          data.startDate.getFullYear(),
          data.startDate.getMonth(),
          data.startDate.getDate(),
          data.startTime.getHours(),
          data.startTime.getMinutes()
        );
        const end = new Date(
          data.endDate.getFullYear(),
          data.endDate.getMonth(),
          data.endDate.getDate(),
          data.endTime.getHours(),
          data.endTime.getMinutes()
        );
        if (start >= end) {
          toast.value?.error('일정 시작 시간이 종료 시간보다 늦을 수 없습니다.');
          return;
        }
      } else {
        const start = new Date(data.startTime);
        const end = new Date(data.endTime);
        if (start >= end) {
          toast.value?.error('시간 범위를 올바르게 설정해주세요.');
          return;
        }
      }

      if (!isValid) {
        toast.value?.error('모든 필수 항목을 입력해주세요.');
        return;
      }
    }

    confirmCallback = async () => {
      const date = new Date(data.date);
      const startTime = new Date(data.startTime);
      const endTime = new Date(data.endTime);

      await submitSchedule(
        {
          ...data,
          date,
          startTime,
          endTime,
        },
        !data.customerId
      );
    };

    showConfirm.value = true;
  };

  const handleConfirm = async () => {
    if (typeof confirmCallback === 'function') {
      await confirmCallback();
    }
  };

  const formatDateTime = (date, time) => {
    if (!(date instanceof Date) || !(time instanceof Date)) return '';

    return dayjs(date)
      .set('hour', dayjs(time).hour())
      .set('minute', dayjs(time).minute())
      .set('second', 0)
      .set('millisecond', 0)
      .format('YYYY-MM-DDTHH:mm:ss');
  };

  const submitSchedule = async (formData, isAnonymous) => {
    try {
      if (tab.value === 'plan') {
        if (formData.repeat === 'none') {
          await createPlan({
            staffId: formData.staff,
            planTitle: formData.title,
            planMemo: formData.memo || '',
            planStartAt: toDateTimeString(formData.startDate, formData.startTime),
            planEndAt: toDateTimeString(formData.endDate, formData.endTime),
          });
        } else {
          await createRegularPlan({
            staffId: formData.staff,
            regularPlanTitle: formData.title,
            regularPlanMemo: formData.memo || '',
            weeklyPlan: formData.repeat === 'weekly' ? formData.weeklyPlan : null,
            monthlyPlan: formData.repeat === 'monthly' ? formData.monthlyPlan : null,
            regularPlanStartAt: formatTime(formData.startTime),
            regularPlanEndAt: formatTime(formData.endTime),
          });
        }

        toast.value?.success('일정이 등록되었습니다.');
        close();
        return;
      }

      if (tab.value === 'leave') {
        if (formData.repeat === 'none') {
          await createLeave({
            staffId: formData.staff,
            leaveTitle: formData.title,
            leaveAt: dayjs(formData.date).format('YYYY-MM-DD'),
            leaveMemo: formData.memo || '',
          });
        } else {
          await createRegularLeave({
            staffId: formData.staff,
            regularLeaveTitle: formData.title,
            regularLeaveMemo: formData.memo || '',
            weeklyLeave: formData.repeat === 'weekly' ? formData.weeklyLeave : null,
            monthlyLeave: formData.repeat === 'monthly' ? formData.monthlyLeave : null,
          });
        }

        toast.value?.success('휴무가 등록되었습니다.');
        close();
        return;
      }

      const payload = {
        customerId: isAnonymous ? null : formData.customerId,
        staffId: formData.staffId,
        secondaryItemIds: formData.selectedItems?.map(item => item.id) || [],
        reservationDate: dayjs(formData.date).format('YYYY-MM-DD'),
        reservationStartAt: formatDateTime(formData.date, formData.startTime),
        reservationEndAt: formatDateTime(formData.date, formData.endTime),
        reservationMemo: formData.memo || '',
        staffMemo: formData.staffMemo || '',
      };
      await createReservation(payload);

      close();
    } catch (err) {
      toast.value?.error('예약 등록 중 오류가 발생했습니다.');
      console.error(err);
    }
  };

  const formatTime = time => {
    return dayjs(time).format('HH:mm:ss');
  };

  const close = () => {
    emit('update:modelValue', false);
  };

  const handleEsc = e => {
    if (e.key === 'Escape') close();
  };

  onMounted(() => window.addEventListener('keydown', handleEsc));
  onBeforeUnmount(() => window.removeEventListener('keydown', handleEsc));
</script>

<style scoped>
  .overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100vh;
    background: rgba(0, 0, 0, 0.3);
    z-index: 1000;
  }

  .modal-panel {
    position: fixed;
    top: 0;
    left: 240px;
    width: calc(100% - 240px);
    height: 100vh;
    background: var(--color-neutral-white);
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
  }

  .close-btn {
    background: none;
    border: none;
    font-size: 24px;
    cursor: pointer;
  }

  .modal-body {
    display: flex;
    gap: 32px;
    flex: 1;
  }

  .left-detail {
    flex: 1;
  }

  .modal-footer {
    margin-top: 32px;
    display: flex;
    flex-direction: row;
    gap: 12px;
    justify-content: flex-end;
  }

  .tab-buttons {
    display: flex;
    gap: 8px;
    margin-bottom: 24px;
  }

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
</style>
