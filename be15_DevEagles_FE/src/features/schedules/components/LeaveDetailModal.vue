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
        <div class="left-detail">
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
                v-model="edited.title"
                type="text"
                placeholder="제목 입력"
              />
              <span v-else>{{ reservation.title }}</span>
            </div>
          </div>

          <div class="row row-select">
            <label>담당자</label>
            <div class="form-control-wrapper">
              <BaseForm
                v-if="isEditMode"
                v-model="edited.staff"
                type="select"
                :options="[
                  { text: '디자이너 A', value: '디자이너 A' },
                  { text: '디자이너 B', value: '디자이너 B' },
                ]"
                placeholder="담당자 선택"
              />
              <span v-else>{{ reservation.staff || '미지정' }}</span>
            </div>
          </div>

          <div class="row">
            <label>날짜</label>
            <div class="form-control-wrapper">
              <PrimeDatePicker
                v-if="isEditMode"
                v-model="edited.date"
                :show-time="false"
                :show-button-bar="true"
                :clearable="false"
                hour-format="24"
                placeholder="날짜를 선택하세요"
              />
              <span v-else>{{ reservation.start }}</span>
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
              <span v-else>{{ reservation.memo }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <BaseButton type="error" @click="close">닫기</BaseButton>
        <template v-if="isEditMode">
          <BaseButton type="primary" @click="saveEdit">저장</BaseButton>
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
</template>

<script setup>
  import { ref, defineProps, defineEmits, watch, computed, onMounted, onBeforeUnmount } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';

  const props = defineProps({
    modelValue: { type: Boolean, required: true },
    reservation: { type: Object, default: () => ({}) },
  });
  const emit = defineEmits(['update:modelValue']);

  const isEditMode = ref(false);
  const showMenu = ref(false);
  const edited = ref({});

  const close = () => {
    emit('update:modelValue', false);
    isEditMode.value = false;
    showMenu.value = false;
    edited.value = {};
  };

  const handleEsc = e => {
    if (e.key === 'Escape') {
      close();
    }
  };

  onMounted(() => {
    window.addEventListener('keydown', handleEsc);
  });
  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleEsc);
  });

  watch(
    () => props.modelValue,
    newVal => {
      if (newVal) {
        isEditMode.value = false;
        showMenu.value = false;
        edited.value = {};
      }
    }
  );

  const toggleMenu = () => {
    showMenu.value = !showMenu.value;
  };

  const handleEdit = () => {
    isEditMode.value = true;
    showMenu.value = false;
    edited.value = { ...props.reservation };
  };

  const handleDelete = () => {
    showMenu.value = false;
    if (confirm('정말 삭제하시겠습니까?')) {
      alert('삭제 요청 전송');
    }
  };

  const saveEdit = () => {
    alert('수정 내용 저장:\n' + JSON.stringify(edited.value, null, 2));
    isEditMode.value = false;
  };

  const leaveTypeLabel = computed(() => {
    return props.reservation.type === 'regular_leave' ? '정기휴무' : '휴무';
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
