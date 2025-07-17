<script setup>
  import { onMounted, ref } from 'vue';
  import { getChatRooms } from '@/features/chat/api/chat.js';

  const emit = defineEmits(['select']);
  const chatRooms = ref([]);

  onMounted(async () => {
    try {
      const res = await getChatRooms();
      chatRooms.value = res.data.sort(
        (a, b) => new Date(b.lastMessageAt || 0) - new Date(a.lastMessageAt || 0)
      );

      console.log(chatRooms.value);
    } catch (e) {
      console.error('❌ 채팅방 목록 불러오기 실패:', e);
    }
  });
</script>

<template>
  <div class="chat-list">
    <p class="chat-list-title">배정된 채팅방</p>

    <div
      v-for="room in chatRooms"
      :key="room.roomId"
      class="chat-room-card"
      @click="$emit('select', room.roomId)"
    >
      <div class="chat-room-content">
        <div class="chat-room-info">
          <p class="customer-name">{{ room.customerName || '고객 이름 없음' }}</p>
          <p class="shop-name">{{ room.customerShopName || '매장 이름 없음' }}</p>
        </div>
        <div class="chat-message-preview">
          <span class="message-text">{{ room.lastMessage || '메시지 없음' }}</span>
          <span v-if="room.lastMessageAt" class="message-time">
            {{
              new Date(room.lastMessageAt).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
              })
            }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
  .chat-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .chat-list-title {
    font-size: 15px;
    font-weight: bold;
    margin-bottom: 0.5rem;
    color: var(--color-gray-800);
  }

  .chat-room-card {
    background: #fff;
    border-radius: 12px;
    padding: 0.75rem 1rem;
    cursor: pointer;
    transition: background 0.2s ease;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
    display: flex;
    align-items: center;
    border: 1px solid #ececec;
  }

  .chat-room-card:hover {
    background: #f4f6f8;
  }

  .chat-room-content {
    display: flex;
    flex-direction: column;
    width: 100%;
  }

  .chat-room-info {
    display: flex;
    justify-content: space-between;
    margin-bottom: 4px;
  }

  .customer-name {
    font-weight: 600;
    font-size: 14px;
    color: var(--color-gray-900);
  }

  .shop-name {
    font-size: 12px;
    color: var(--color-gray-500);
  }

  .chat-message-preview {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .message-text {
    font-size: 13px;
    color: var(--color-gray-700);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 220px;
  }

  .message-time {
    font-size: 12px;
    color: var(--color-gray-400);
    margin-left: 8px;
    white-space: nowrap;
  }
</style>
