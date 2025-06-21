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
          <component :is="currentTabComponent" />
        </div>
      </div>

      <!-- 푸터 -->
      <div class="modal-footer">
        <BaseButton outline @click="close">등록 취소</BaseButton>
        <BaseButton @click="submit">등록</BaseButton>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import ReservationForm from './ReservationForm.vue';
  import PlanForm from './PlanForm.vue';
  import LeaveForm from './LeaveForm.vue';

  const props = defineProps({
    modelValue: { type: Boolean, required: true },
    defaultTab: { type: String, default: 'reservation' }, // 'reservation', 'plan', 'leave'
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

  const submit = () => {
    alert(`${tab.value} 등록 로직 실행`);
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
</style>
