<script setup>
  import BasePopover from '@/components/common/BasePopover.vue';
  import { useNotifications } from '@/features/notifications/composables/useNotifications.js';

  defineProps({
    modelValue: Boolean,
    triggerElement: Object,
  });
  const emit = defineEmits(['update:modelValue']);

  const { allNotifications, isLoading, handleMarkAsRead, handleMarkAllAsRead, unreadCount } =
    useNotifications();

  const getNotificationDetails = type => {
    switch (type) {
      case 'RESERVATION':
        return { icon: '📅', sender: '예약 시스템', color: '#3b82f6' };
      case 'ANALYSIS':
        return { icon: '📈', sender: '데이터 분석', color: '#10b981' };
      case 'NOTICE':
        return { icon: '📢', sender: '공지사항', color: '#f97316' };
      default:
        return { icon: '🔔', sender: '시스템', color: '#6b7280' };
    }
  };
  const formatDate = dateString => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return `${date.getMonth() + 1}월 ${date.getDate()}일`;
  };
</script>

<template>
  <BasePopover
    :model-value="modelValue"
    :trigger-element="triggerElement"
    placement="bottom"
    size="md"
    :show-actions="false"
    :mask-closable="true"
    class="z-50"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template #default>
      <div class="notification-wrapper">
        <div class="notification-header">
          <div class="header-left">
            <span class="icon">🔔</span>
            <h3 class="title">새 알림</h3>
          </div>
          <button
            class="mark-all-read-btn"
            :disabled="unreadCount === 0"
            @click="handleMarkAllAsRead"
          >
            모두 읽음
          </button>
        </div>

        <div class="notification-scroll custom-scrollbar">
          <div v-if="isLoading" class="empty-state">
            <p class="empty-text">알림을 불러오는 중입니다...</p>
          </div>

          <ul v-else-if="allNotifications.length > 0" class="notification-list">
            <li
              v-for="item in allNotifications"
              :key="item.notificationId"
              :class="['notification-item', { 'is-read': item.read }]"
              @click="handleMarkAsRead(item)"
            >
              <div class="item-icon">
                {{ getNotificationDetails(item.type).icon }}
              </div>
              <div class="item-content">
                <div class="item-meta">
                  <span class="sender" :style="{ color: getNotificationDetails(item.type).color }">
                    {{ getNotificationDetails(item.type).sender }}
                  </span>
                  <span class="date">{{ formatDate(item.createdAt) }}</span>
                </div>
                <p class="message">{{ item.content }}</p>
              </div>
            </li>
          </ul>

          <div v-else class="empty-state empty-state--enhanced">
            <div class="empty-icon">📭</div>
            <p class="empty-text">알림이 없습니다.</p>
          </div>
        </div>
      </div>
    </template>
  </BasePopover>
</template>

<style scoped>
  .notification-wrapper {
    width: 100%;
    max-width: 340px;
    background-color: #fff;
    border-radius: 12px;
    box-sizing: border-box;
    overflow: hidden;
    padding: 0;
  }

  .notification-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 12px 16px;
    border-bottom: 1px solid #e5e7eb;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .mark-all-read-btn {
    font-size: 12px;
    font-weight: 500;
    color: var(--color-primary-500);
    background-color: transparent;
    border: none;
    padding: 4px 8px;
    border-radius: 6px;
    cursor: pointer;
    transition: background-color 0.2s;
  }

  .mark-all-read-btn:hover:not(:disabled) {
    background-color: var(--color-primary-50);
  }

  .mark-all-read-btn:disabled {
    color: var(--color-gray-400);
    cursor: not-allowed;
  }

  .title {
    font-size: 14px;
    font-weight: 600;
    color: #111827;
  }

  .notification-scroll {
    max-height: 340px;
    overflow-y: auto;
  }

  .notification-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 8px 12px 16px;
  }

  .notification-item {
    display: flex;
    gap: 12px;
    padding: 14px 16px;
    border-radius: 12px;
    background-color: #fefefe;
    border: 1px solid #e0e0e0;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
    transition:
      background-color 0.2s,
      opacity 0.2s;
    word-break: break-word;
    cursor: pointer;
  }

  .notification-item:hover {
    background-color: #f9fafb;
  }

  .notification-item.is-read {
    opacity: 0.65;
  }

  .item-icon {
    font-size: 20px;
    line-height: 1;
    width: 32px;
    text-align: center;
    margin-top: 4px;
  }

  .item-content {
    flex: 1;
    min-width: 0;
  }

  .item-meta {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    margin-bottom: 6px;
  }

  .sender {
    font-weight: 600;
  }

  .date {
    color: #9ca3af;
  }

  .message {
    font-size: 13.5px;
    color: #374151;
    line-height: 1.5;
    white-space: pre-line;
  }

  .empty-state {
    padding: 40px 20px;
    text-align: center;
    color: #6b7280;
  }

  .empty-state--enhanced .empty-icon {
    font-size: 2rem;
    margin-bottom: 8px;
  }

  .custom-scrollbar {
    scrollbar-width: thin;
    scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
  }

  .custom-scrollbar::-webkit-scrollbar {
    width: 5px;
  }

  .custom-scrollbar::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.3);
    border-radius: 6px;
  }

  .custom-scrollbar::-webkit-scrollbar-thumb:hover {
    background-color: rgba(0, 0, 0, 0.5);
  }
</style>
