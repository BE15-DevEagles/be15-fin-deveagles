<template>
  <div v-if="modelValue" class="overlay" @click.self="close">
    <div class="modal-panel">
      <div class="modal-header">
        <div>
          <h1>등록된 스케줄</h1>
          <p class="type-label">{{ leaveTypeLabel }}</p>
        </div>
        <button class="close-btn" @click="close">×</button>
      </div>

      <div class="modal-body">
        <div v-if="Object.keys(leave || {}).length" class="left-detail">
          <div class="row row-select">
            <label>구분</label>
            <div class="form-control-wrapper">
              <BaseForm
                v-if="isEditMode"
                v-model="edited.type"
                type="select"
                :options="[
                  { text: '휴무', value: 'leave' },
                  { text: '정기휴무', value: 'regular_leave' },
                ]"
                placeholder="구분 선택"
              />
              <span v-else>{{ leaveTypeLabel }}</span>
            </div>
          </div>

          <div class="row">
            <label>제목</label>
            <div class="form-control-wrapper">
              <BaseForm
                v-if="isEditMode"
                v-model="edited.leaveTitle"
                type="text"
                placeholder="제목 입력"
              />
              <span v-else>{{ leave.leaveTitle || '없음' }}</span>
            </div>
          </div>

          <div class="row row-select">
            <label>담당자</label>
            <div class="form-control-wrapper">
              <BaseForm
                v-if="isEditMode"
                type="text"
                :model-value="leave.staffName"
                :disabled="true"
                style="pointer-events: none"
              />
              <span v-else>{{ leave.staffName || '미지정' }}</span>
            </div>
          </div>

          <div class="row">
            <label>날짜</label>
            <div class="form-control-wrapper">
              <template v-if="isEditMode">
                <PrimeDatePicker
                  v-if="edited.type === 'leave'"
                  v-model="edited.date"
                  :show-time="false"
                  :show-button-bar="true"
                  :clearable="false"
                  hour-format="24"
                  placeholder="날짜를 선택하세요"
                />

                <!-- 반복 주기 선택 (정기 휴무 전용) -->
                <BaseForm
                  v-if="edited.type === 'regular_leave'"
                  v-model="edited.repeat"
                  type="select"
                  :options="[
                    { text: '반복 안함', value: 'none' },
                    { text: '매달 반복', value: 'monthly' },
                    { text: '요일 반복', value: 'weekly' },
                  ]"
                  placeholder="반복 주기 선택"
                  style="margin-top: 8px; max-width: 200px"
                />

                <!-- 반복 주기에 따른 상세 선택 -->
                <BaseForm
                  v-if="edited.type === 'regular_leave' && edited.repeat === 'monthly'"
                  v-model="edited.monthlyLeave"
                  type="select"
                  :options="monthlyDayOptions"
                  placeholder="반복 일자 선택"
                  style="margin-top: 8px; max-width: 200px"
                />

                <BaseForm
                  v-if="edited.type === 'regular_leave' && edited.repeat === 'weekly'"
                  v-model="edited.weeklyLeave"
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
                  style="margin-top: 8px; max-width: 180px"
                />
              </template>

              <template v-else>
                <span>{{ displayStart }}</span>
              </template>
            </div>
          </div>

          <div class="row">
            <label>메모</label>
            <div class="form-control-wrapper">
              <BaseForm
                v-if="isEditMode"
                v-model="edited.memo"
                type="textarea"
                placeholder="메모 입력"
              />
              <span v-else>{{ leave.memo }}</span>
            </div>
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
              <li @click="handleDelete">삭제하기</li>
            </ul>
          </div>
        </template>
      </div>
    </div>
  </div>
  <BaseToast ref="toast" />
  <BaseConfirm
    v-model="showDeleteConfirm"
    title="정말 삭제하시겠습니까?"
    message="해당 휴무를 삭제하면 복구할 수 없습니다.
    계속 진행하시겠습니까?"
    confirm-text="삭제"
    cancel-text="취소"
    confirm-type="error"
    icon-type="error"
    @confirm="confirmDelete"
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
  import { ref, defineProps, defineEmits, watch, computed, onMounted, onBeforeUnmount } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import {
    fetchScheduleDetail,
    getStaffList,
    updateLeave,
    updateRegularLeave,
    switchLeaveType,
    deleteLeaves,
  } from '@/features/schedules/api/schedules.js';
  import BaseToast from '@/components/common/BaseToast.vue';
  import dayjs from 'dayjs';
  import BaseConfirm from '@/components/common/BaseConfirm.vue';
  const toast = ref(null);
  const monthlyDayOptions = Array.from({ length: 31 }, (_, i) => ({
    text: `${i + 1}일`,
    value: i + 1,
  }));
  const props = defineProps({
    modelValue: { type: Boolean, required: true },
    id: { type: Number, required: true },
    type: { type: String, required: true },
  });
  const emit = defineEmits(['update:modelValue']);
  const showEditConfirm = ref(false);
  const isEditMode = ref(false);
  const showMenu = ref(false);
  const edited = ref({});
  const staffOptions = ref([]);
  const leave = ref(null);
  const handleSave = () => {
    showEditConfirm.value = true;
  };

  const confirmEdit = async () => {
    await saveEdit();
  };
  const close = () => {
    emit('update:modelValue', false);
    isEditMode.value = false;
    showMenu.value = false;
    edited.value = {};
    leave.value = null;
  };

  const handleEsc = e => {
    if (e.key === 'Escape') close();
  };
  onMounted(async () => {
    window.addEventListener('keydown', handleEsc);
    const staffResponse = await getStaffList();
    staffOptions.value = staffResponse.map(s => ({ text: s.staffName, value: s.staffId }));
  });
  onBeforeUnmount(() => window.removeEventListener('keydown', handleEsc));

  watch(
    () => props.modelValue,
    async val => {
      if (val) {
        isEditMode.value = false;
        showMenu.value = false;
        edited.value = {};
        try {
          const response = await fetchScheduleDetail(props.type, props.id);
          const data = response.data.data;

          leave.value = {
            leaveTitle: data.leaveTitle ?? data.title ?? '',
            staffName: data.staffName ?? data.staff ?? '',
            staffId: data.staffId ?? null,
            memo: data.memo ?? '',
            startAt: props.type === 'leave' ? (data.start ?? data.leaveDate ?? '') : undefined,
            repeatRule: props.type === 'regular_leave' ? (data.repeatRule ?? '') : undefined,
          };
          console.log('🧪 fetched type:', props.type);
          console.log('🧪 fetched repeatRule:', data.repeatRule);
          console.log('🧪 fetched full data:', data);
        } catch (e) {
          console.error('❌ 휴무 상세 조회 실패', e);
        }
      }
    },
    { immediate: true }
  );

  const toggleMenu = () => (showMenu.value = !showMenu.value);
  const handleEdit = () => {
    isEditMode.value = true;
    showMenu.value = false;

    const rawRule = leave.value.repeatRule;

    let repeat = 'none';
    let monthlyLeave = '';
    let weeklyLeave = '';

    // "매월 1일" → monthlyLeave = 1
    if (rawRule?.startsWith('매월')) {
      repeat = 'monthly';
      const dayMatch = rawRule.match(/\d+/);
      monthlyLeave = dayMatch ? Number(dayMatch[0]) : '';
    }

    // "매주 월요일" → weeklyLeave = 'MON'
    else if (rawRule?.startsWith('매주')) {
      repeat = 'weekly';
      const weekdayMap = {
        월요일: 'MON',
        화요일: 'TUE',
        수요일: 'WED',
        목요일: 'THU',
        금요일: 'FRI',
        토요일: 'SAT',
        일요일: 'SUN',
      };
      const matched = Object.entries(weekdayMap).find(([kor]) => rawRule.includes(kor));
      weeklyLeave = matched?.[1] || '';
    }

    edited.value = {
      staffId: leave.value.staffId,
      date: leave.value.startAt?.split('T')[0] || '',
      leaveTitle: leave.value.leaveTitle,
      memo: leave.value.memo,
      type: props.type,
      repeat,
      weeklyLeave,
      monthlyLeave,
    };
  };

  const showDeleteConfirm = ref(false);

  const handleDelete = () => {
    showMenu.value = false;
    showDeleteConfirm.value = true;
  };

  const confirmDelete = async () => {
    try {
      const payload = [{ id: props.id, type: props.type }];
      await deleteLeaves(payload);
      toast.value.success('삭제되었습니다.');
      close();
    } catch (e) {
      console.error('❌ 삭제 실패:', e);
      toast.value.error('삭제 중 오류가 발생했습니다.');
    }
  };

  const formatDate = date => {
    return dayjs(date).format('YYYY-MM-DD');
  };

  const saveEdit = async () => {
    try {
      const originalType = props.type;
      const newType = edited.value.type;

      // 전환이 아닌 경우
      if (originalType === newType) {
        if (newType === 'leave') {
          const payload = {
            staffId: edited.value.staffId,
            leaveTitle: edited.value.leaveTitle,
            leaveAt: formatDate(edited.value.date),
            leaveMemo: edited.value.memo,
          };
          console.log('📦 payload:', payload);
          await updateLeave(props.id, payload);
        } else {
          const payload = {
            staffId: edited.value.staffId,
            regularLeaveTitle: edited.value.leaveTitle,
            weeklyLeave: edited.value.repeat === 'weekly' ? edited.value.weeklyLeave : null,
            monthlyLeave: edited.value.repeat === 'monthly' ? edited.value.monthlyLeave : null,
            regularLeaveMemo: edited.value.memo,
          };
          console.log('📦 payload:', payload);
          await updateRegularLeave(props.id, payload);
        }
      } else {
        // 전환 처리
        const fromType = originalType === 'leave' ? 'LEAVE' : 'REGULAR_LEAVE';
        const toType = newType === 'leave' ? 'LEAVE' : 'REGULAR_LEAVE';

        const switchPayload = {
          fromType,
          fromId: props.id,
          toType,
          leaveRequest:
            newType === 'leave'
              ? {
                  staffId: edited.value.staffId,
                  leaveTitle: edited.value.leaveTitle,
                  leaveAt: formatDate(edited.value.date),
                  leaveMemo: edited.value.memo,
                }
              : null,
          regularLeaveRequest:
            newType === 'regular_leave'
              ? {
                  staffId: edited.value.staffId,
                  regularLeaveTitle: edited.value.leaveTitle,
                  weeklyLeave: edited.value.repeat === 'weekly' ? edited.value.weeklyLeave : null,
                  monthlyLeave:
                    edited.value.repeat === 'monthly' ? edited.value.monthlyLeave : null,
                  regularLeaveMemo: edited.value.memo,
                }
              : null,
        };

        await switchLeaveType(switchPayload);
      }

      toast.value.success('수정되었습니다.');
      isEditMode.value = false;
      close();
    } catch (e) {
      console.error('❌ 수정 실패:', e);
      toast.value.error('수정 중 오류가 발생했습니다.');
    }
  };

  const leaveTypeLabel = computed(() => {
    return props.type === 'regular_leave' ? '정기 휴무' : '휴무';
  });

  const displayStart = computed(() => {
    if (!leave.value) return '';
    if (props.type === 'regular_leave') {
      return leave.value.repeatRule || '-';
    }
    return leave.value.startAt
      ? new Date(leave.value.startAt).toLocaleDateString('ko-KR', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
        })
      : '-';
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

  .form-group {
    display: flex;
    align-items: center;
    margin: 0;
    padding: 0;
    width: 200px !important;
  }

  .modal-panel {
    position: fixed;
    top: 0;
    left: 240px;
    width: calc(100% - 240px);
    height: 100vh;
    background-color: var(--color-bg-primary);
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
    font-weight: 700;
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
    min-height: 200px;
  }

  .left-detail {
    flex: 1;
    display: block !important;
  }

  .row {
    display: flex;
    align-items: flex-start;
    margin-bottom: 14px;
  }

  .row label {
    width: 100px;
    font-weight: 700;
    padding-top: 6px;
    color: var(--color-text-secondary);
    line-height: 1.5;
  }

  .row span,
  .row input,
  .row textarea {
    font-size: 14px;
    line-height: 1.5;
    padding: 6px 8px;
    width: 100%;
    max-width: 400px;
    box-sizing: border-box;
    color: var(--color-text-primary);
  }

  .row input,
  .row textarea {
    border: 1px solid var(--color-gray-300);
    border-radius: 4px;
    background-color: var(--color-bg-primary);
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
    background-color: var(--color-bg-primary);
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
    background-color: var(--color-bg-primary);
    color: var(--color-text-primary);
    min-width: 120px;
    height: 32px;
  }

  .all-day-checkbox {
    display: flex;
    align-items: center;
    gap: 4px;
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
    color: var(--color-text-secondary);
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
</style>
