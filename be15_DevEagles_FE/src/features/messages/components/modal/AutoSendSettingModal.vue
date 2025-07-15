<script setup>
  import { ref, computed, onMounted } from 'vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import AutoSendSettingDrawer from '@/features/messages/components/drawer/AutoSendSettingDrawer.vue';
  import EditIcon from '@/components/icons/EditIcon.vue';
  import NewIcon from '@/components/icons/NewIcon.vue';
  import AutoSendAPI from '@/features/messages/api/autoSend.js';

  const props = defineProps({
    modelValue: Boolean,
  });

  const emit = defineEmits(['update:modelValue']);

  const show = computed({
    get: () => props.modelValue,
    set: val => emit('update:modelValue', val),
  });

  const selectedIndex = ref(0);
  const drawerVisible = ref(false);
  const selectedMessage = ref(null);

  const DEFAULT_CATEGORIES = [
    { label: '신규 등록', triggerType: 'NEW_CUSTOMER' },
    { label: '예약 확정', triggerType: 'RESERVATION_CREATED' },
    { label: '예약 수정', triggerType: 'RESERVATION_MODIFIED' },
    { label: '예약 취소', triggerType: 'RESERVATION_CANCELLED' },
    { label: '횟수권 차감', triggerType: 'SESSION_PASS_USED' },
    { label: '선불권 차감', triggerType: 'PREPAID_USED' },
  ];

  const templates = ref([]);

  const computedItems = computed(() => {
    const map = new Map();
    templates.value.forEach(({ automaticEventType, templateContent, isActive }) => {
      const entry = { message: templateContent, isActive };
      if (!map.has(automaticEventType)) {
        map.set(automaticEventType, []);
      }
      map.get(automaticEventType).push(entry);
    });

    return DEFAULT_CATEGORIES.map(c => ({
      ...c,
      messages: map.get(c.triggerType) || [],
    }));
  });

  async function fetchTemplates() {
    try {
      const res = await AutoSendAPI.getTemplates();
      templates.value = Array.isArray(res) ? res : (res?.data ?? []);
    } catch (e) {
      console.error('자동발신 템플릿 초기 조회 실패', e);
      templates.value = [];
    }
  }

  onMounted(fetchTemplates);

  function openDrawer(message, index) {
    const selectedItem = computedItems.value[selectedIndex.value];
    selectedMessage.value = {
      ...message,
      parentIndex: selectedIndex.value,
      messageIndex: index,
      label: selectedItem.label,
      triggerType: selectedItem.triggerType,
    };
    drawerVisible.value = true;
  }

  function addNewMessage(index) {
    const selectedItem = computedItems.value[index];
    selectedIndex.value = index;
    selectedMessage.value = {
      message: '',
      isActive: true,
      triggerType: selectedItem.triggerType,
      label: selectedItem.label,
      parentIndex: index,
      messageIndex: null,
    };
    drawerVisible.value = true;
  }

  async function handleMessageSaved() {
    await fetchTemplates();
    drawerVisible.value = false;
  }
</script>

<template>
  <BaseModal v-model="show" title="자동 발신 설정">
    <div class="auto-send-modal-body">
      <div class="auto-send-list">
        <div
          v-for="(item, index) in computedItems"
          :key="item.triggerType"
          class="auto-send-item"
          :class="{ selected: index === selectedIndex }"
          @click="selectedIndex = index"
        >
          <div class="list-item-header">
            <span class="label">{{ item.label }}</span>
            <button class="add-icon-button" @click.stop="addNewMessage(index)">
              <NewIcon />
            </button>
          </div>
        </div>
      </div>

      <div class="auto-send-preview">
        <template v-if="computedItems[selectedIndex].messages.length > 0">
          <div
            v-for="(message, msgIdx) in computedItems[selectedIndex].messages"
            :key="msgIdx"
            class="preview-card"
          >
            <div class="preview-row">
              <span class="preview-label">활성화 여부</span>
              <span class="preview-value">{{ message.isActive ? 'ON' : 'OFF' }}</span>
            </div>
            <div class="preview-row">
              <span class="preview-label">메시지</span>
              <span class="preview-value">{{ message.message }}</span>
            </div>
            <div class="text-right mt-2">
              <button class="edit-icon-button" @click="openDrawer(message, msgIdx)">
                <EditIcon />
              </button>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="empty-preview">
            등록된 메시지가 없습니다.<br />
            <strong>＋ 버튼</strong>을 눌러 메시지를 추가해보세요.
          </div>
        </template>
      </div>
    </div>

    <AutoSendSettingDrawer
      v-if="selectedMessage"
      v-model="drawerVisible"
      :item="selectedMessage"
      @save="handleMessageSaved"
    />
  </BaseModal>
</template>

<style scoped>
  .auto-send-modal-body {
    display: flex;
    gap: 2rem;
    min-height: 400px;
  }
  .auto-send-list {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    max-width: 280px;
    border-right: 1px solid #e5e7eb;
    padding-right: 1rem;
  }
  .auto-send-item {
    padding: 0.75rem 1rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    background-color: #f9fafb;
    cursor: pointer;
    transition: background-color 0.2s;
  }
  .auto-send-item:hover {
    background-color: #f3f4f6;
  }
  .auto-send-item.selected {
    background-color: #e0f2fe;
    border-color: #38bdf8;
  }
  .list-item-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .label {
    font-size: 0.95rem;
    font-weight: 500;
    color: #111827;
  }
  .add-icon-button {
    background: none;
    border: none;
    padding: 0;
    margin-left: 0.25rem;
    cursor: pointer;
  }
  .auto-send-preview {
    flex: 2;
    padding: 1rem 0;
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }
  .preview-card {
    width: 100%;
    background-color: #f9fafb;
    border: 1px dashed #cbd5e1;
    border-radius: 8px;
    padding: 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }
  .preview-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    border-bottom: 1px solid #e5e7eb;
    padding-bottom: 0.5rem;
  }
  .preview-label {
    font-weight: 500;
    color: #374151;
    font-size: 0.875rem;
  }
  .preview-value {
    color: #4b5563;
    font-size: 0.875rem;
    max-width: 60%;
    text-align: right;
    word-break: break-word;
  }
  .edit-icon-button {
    background: none;
    border: none;
    padding: 0.25rem;
    cursor: pointer;
  }
  .empty-preview {
    font-size: 0.875rem;
    color: #9ca3af;
    padding: 1.25rem;
    text-align: center;
  }
</style>
