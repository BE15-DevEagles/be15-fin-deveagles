<template>
  <div class="container">
    <div class="page-header">
      <h1 class="font-screen-title">예약 캘린더</h1>
    </div>

    <div class="filter-bar filter-align-right">
      <input
        v-model="searchText"
        type="text"
        placeholder="고객명 또는 연락처 검색"
        class="input input-search"
      />
      <select v-model="selectedService" class="input input-select">
        <option value="">시술 종류</option>
        <option value="커트">커트</option>
        <option value="염색">염색</option>
        <option value="펌">펌</option>
      </select>
      <select v-model="selectedStaff" class="input input-select">
        <option value="">담당자</option>
        <option value="최민수">최민수</option>
        <option value="이채은">이채은</option>
      </select>
      <select v-model="selectedType" class="input input-select">
        <option value="">스케줄</option>
        <option value="reservation">예약</option>
        <option value="leave">휴무</option>
        <option value="event">일정</option>
      </select>
      <button class="btn btn-primary schedule-btn" @click="handleClickScheduleRegist = true">
        스케줄 등록
      </button>
    </div>
    <div class="calendar-wrapper">
      <ScheduleCalendar :schedules="calendarEvents" @click-schedule="handleClickSchedule" />
    </div>
    <ReservationDetailModal
      v-if="modalType === 'reservation'"
      v-model="isModalOpen"
      :reservation="selectedReservation"
    />
    <LeaveDetailModal
      v-if="modalType === 'leave'"
      v-model="isModalOpen"
      :reservation="selectedReservation"
    />
    <PlanDetailModal
      v-if="modalType === 'plan'"
      v-model="isModalOpen"
      :reservation="selectedReservation"
    />

    <ScheduleRegistModal v-if="handleClickScheduleRegist" v-model="handleClickScheduleRegist" />
  </div>
</template>

<script setup>
  import { ref, computed } from 'vue';
  import ScheduleCalendar from '@/features/schedules/components/ScheduleCalendar.vue';
  import LeaveDetailModal from '@/features/schedules/components/LeaveDetailModal.vue';
  import PlanDetailModal from '@/features/schedules/components/PlanDetailModal.vue';
  import ReservationDetailModal from '@/features/schedules/components/ReservationDetailModal.vue';
  import ScheduleRegistModal from '@/features/schedules/components/ScheduleRegistModal.vue';

  const staffColors = {
    최민수: 'var(--color-primary-main)',
    김민지: 'var(--color-success-200)',
    이채은: 'var(--color-secondary-100)',
  };

  const searchText = ref('');
  const selectedService = ref('');
  const selectedStaff = ref('');
  const selectedType = ref('');

  const schedules = ref([
    {
      id: 1,
      title: '김민지 고객 - 커트',
      start: '2025-06-17T10:00:00',
      end: '2025-06-17T11:00:00',
      type: 'reservation',
      service: '커트',
      staff: '최민수',
      customer: '김민지',
      phone: '010-1234-5678',
      status: '예약 확정',
      note: '첫 방문 고객',
      memo: '고객 요청사항 없음',
      date: '2025-06-17',
      startTime: '10:00',
      endTime: '11:00',
      timeRange: '오전 10:00 - 오전 11:00',
      duration: '01:00',
    },
    {
      id: 2,
      title: '워크샵',
      start: '2025-06-18T10:00:00',
      end: '2025-06-18T16:00:00',
      type: 'event',
      staff: '이채은',
      date: '2025-06-17',
      timeRange: '오후 01:00 - 오후 04:00',
      duration: '03:00',
      memo: '외부 미팅 장소',
      repeat: 'none',
    },
    {
      id: 3,
      title: '개인 사유로 휴무',
      start: '2025-06-19',
      end: '2025-06-20',
      type: 'leave',
      staff: '김민지',
      service: '',
      customer: '',
      status: '',
      memo: '사전 공지 완료',
      repeat: 'none',
      date: '2025-06-19',
    },
  ]);

  const selectedReservation = ref(null);
  const isModalOpen = ref(false);
  const modalType = ref('');

  const handleClickSchedule = id => {
    const target = schedules.value.find(item => String(item.id) === String(id));
    if (target) {
      selectedReservation.value = target;
      isModalOpen.value = true;

      if (target.type === 'reservation') modalType.value = 'reservation';
      else if (['leave', 'regular_leave'].includes(target.type)) modalType.value = 'leave';
      else if (['event', 'regular_event'].includes(target.type)) modalType.value = 'plan';
    }
  };

  const handleClickScheduleRegist = ref(false);

  const calendarEvents = computed(() =>
    schedules.value.map(item => {
      const isLeave = item.type === 'leave';
      const isAllDay = isLeave;

      return {
        id: item.id,
        title: item.title,
        start: item.start,
        end: item.end,
        allDay: isAllDay,
        backgroundColor: staffColors[item.staff] || 'var(--color-gray-300)',
        textColor: 'var(--color-text-primary)',
        type: item.type,
        status: item.status,
        staff: item.staff,
        customer: item.customer,
        service: item.service,
        memo: item.memo,
      };
    })
  );
</script>

<style scoped>
  .calendar-wrapper {
    background-color: var(--color-neutral-white);
    padding: 20px;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }
  .container {
    padding: 14px;
  }
  .page-header {
    margin-bottom: 32px;
  }
  .filter-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 32px;
    justify-content: flex-end;
  }
  .input-search {
    width: 240px;
  }
  .input-select {
    width: 160px;
  }
  .schedule-btn {
    flex-shrink: 0;
  }
</style>
