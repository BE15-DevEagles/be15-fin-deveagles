<script setup>
  import { computed } from 'vue';
  import PlayIcon from '@/components/icons/MessageCircleIcon.vue';
  import CalendarIcon from '@/components/icons/CalendarIcon.vue';
  import BellIcon from '@/components/icons/BellIcon.vue';

  const props = defineProps({
    messages: {
      type: Array,
      required: true,
    },
  });
  const sentCount = computed(() => props.messages.filter(m => m.statusLabel === 'SENT').length);

  const reservedCount = computed(
    () =>
      props.messages.filter(m => m.messageSendingType === 'RESERVATION' && m.statusLabel !== 'SENT')
        .length
  );

  const totalCount = computed(() => props.messages.length);
</script>

<template>
  <div class="stats-wrapper">
    <div class="stats-card">
      <div class="stats-icon-wrapper icon-blue">
        <PlayIcon :size="20" />
      </div>
      <div class="stats-content">
        <div class="stats-value">{{ sentCount }}</div>
        <div class="stats-label">발송 완료</div>
      </div>
    </div>

    <div class="stats-card">
      <div class="stats-icon-wrapper icon-gray">
        <CalendarIcon :size="20" />
      </div>
      <div class="stats-content">
        <div class="stats-value">{{ reservedCount }}</div>
        <div class="stats-label">예약 문자</div>
      </div>
    </div>

    <div class="stats-card">
      <div class="stats-icon-wrapper icon-yellow">
        <BellIcon :size="20" />
      </div>
      <div class="stats-content">
        <div class="stats-value">{{ totalCount }}</div>
        <div class="stats-label">전체 메시지</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
  .stats-wrapper {
    display: flex;
    gap: 20px;
    margin-bottom: 24px;
  }
  .stats-card {
    flex: 1;
    display: flex;
    align-items: center;
    padding: 16px 20px;
    border: 1px solid var(--color-gray-200);
    border-radius: 16px;
    background-color: var(--color-neutral-white);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  }
  .stats-icon-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 8px;
    margin-right: 12px;
  }
  .icon-blue,
  .icon-gray,
  .icon-yellow {
    background-color: var(--color-gray-100);
  }
  .stats-content {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .stats-value {
    font-size: 20px;
    font-weight: 700;
    color: var(--color-gray-900);
  }
  .stats-label {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-gray-500);
  }
</style>
