<script setup>
  import { ref, watch, computed, nextTick, onMounted, onUnmounted } from 'vue';
  import BaseDrawer from '@/components/common/BaseDrawer.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseToggleSwitch from '@/components/common/BaseToggleSwitch.vue';
  import AutoSendAPI from '@/features/messages/api/autoSend.js';

  const props = defineProps({
    modelValue: Boolean,
    item: Object,
  });

  const emit = defineEmits(['update:modelValue', 'save']);

  const show = computed({
    get: () => props.modelValue,
    set: val => emit('update:modelValue', val),
  });

  const templateContent = ref('');
  const isActive = ref(true);
  const sendTime = ref('immediate'); // UI에만 사용 중

  const variableOptions = [
    '#{고객명}',
    '#{예약확정일}',
    '#{예약날짜}',
    '#{예약변경일}',
    '#{예약취소일}',
    '#{선불권금액}',
    '#{횟수권횟수}',
  ];
  const showVariableDropdown = ref(false);

  watch(
    () => props.item,
    val => {
      if (val) {
        templateContent.value = val.message || '';
        isActive.value = val.isActive ?? true;
        sendTime.value = val.sendTime || 'immediate';
      }
    },
    { immediate: true }
  );

  function insertVariable(variable) {
    const textarea = document.getElementById('auto-message-textarea');
    const cursorPos = textarea?.selectionStart ?? templateContent.value.length;
    const textBefore = templateContent.value.substring(0, cursorPos);
    const textAfter = templateContent.value.substring(cursorPos);
    templateContent.value = textBefore + variable + textAfter;
    nextTick(() => {
      textarea?.focus();
      textarea?.setSelectionRange(cursorPos + variable.length, cursorPos + variable.length);
    });
  }

  async function handleSave() {
    try {
      const isNew = props.item.messageIndex == null;

      if (isNew) {
        await AutoSendAPI.createTemplate({
          automaticEventType: props.item.triggerType,
          templateContent: templateContent.value,
          isActive: isActive.value,
        });
      }

      emit('save', {
        parentIndex: props.item.parentIndex,
        messageIndex: props.item.messageIndex ?? null,
        message: templateContent.value,
        isActive: isActive.value,
        triggerType: props.item.triggerType,
      });

      show.value = false;
    } catch (err) {
      console.error('자동발송 메시지 저장 실패', err.message);
      alert(err.message); // 추후 Toast로 대체 가능
    }
  }

  function handleClickOutside(e) {
    const wrapper = document.querySelector('.dropdown-wrapper');
    if (wrapper && !wrapper.contains(e.target)) {
      showVariableDropdown.value = false;
    }
  }

  onMounted(() => {
    document.addEventListener('click', handleClickOutside);
  });
  onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside);
  });
</script>

<template>
  <BaseDrawer v-model="show" title="자동 메시지 설정" size="md">
    <div class="drawer-body">
      <p class="drawer-label">{{ item.label }} 메시지를 수정하세요</p>

      <!-- 자동발송 토글 -->
      <div class="flex justify-between items-center mb-4">
        <label class="field-label mb-0">자동발송 활성화</label>
        <div class="ml-2">
          <BaseToggleSwitch v-model="isActive" />
        </div>
      </div>

      <!-- 메시지 입력과 변수 삽입 -->
      <div class="mt-2">
        <div class="flex justify-between items-end">
          <label class="field-label mb-0">메시지 내용</label>
          <div class="dropdown-wrapper relative inline-block">
            <BaseButton
              size="xs"
              type="ghost"
              @click.stop="showVariableDropdown = !showVariableDropdown"
            >
              변수 삽입 ▼
            </BaseButton>

            <div
              v-if="showVariableDropdown"
              class="variable-dropdown-list absolute right-0 top-full mt-1"
              @click.stop
            >
              <ul class="dropdown-ul">
                <li
                  v-for="v in variableOptions"
                  :key="v"
                  class="variable-option"
                  @click="
                    insertVariable(v);
                    showVariableDropdown = false;
                  "
                >
                  {{ v }}
                </li>
              </ul>
            </div>
          </div>
        </div>

        <textarea
          id="auto-message-textarea"
          v-model="templateContent"
          class="w-full p-2 border border-gray-300 rounded-md mt-2"
          rows="5"
          placeholder="메시지 내용을 입력하세요"
        />
      </div>

      <!-- 저장 버튼 -->
      <div class="mt-8 flex justify-end">
        <BaseButton type="primary" @click="handleSave">저장</BaseButton>
      </div>
    </div>
  </BaseDrawer>
</template>

<style scoped>
  .drawer-body {
    display: flex;
    flex-direction: column;
  }

  .drawer-label {
    font-size: 0.875rem;
    margin-bottom: 1.5rem;
    color: #6b7280;
  }

  .field-label {
    font-size: 0.875rem;
    font-weight: 600;
    color: #111827;
  }

  textarea {
    font-size: 0.875rem;
    resize: vertical;
  }

  .dropdown-wrapper {
    position: relative;
    display: inline-block;
  }

  .variable-dropdown-list {
    position: absolute;
    z-index: 20;
    background-color: #ffffff;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    padding: 0.25rem;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    min-width: 120px;
  }

  .dropdown-ul {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  .variable-option {
    padding: 0.375rem 0.75rem;
    font-size: 0.8125rem;
    cursor: pointer;
    white-space: nowrap;
    border-radius: 4px;
  }

  .variable-option:hover {
    background-color: #f3f4f6;
  }
</style>
