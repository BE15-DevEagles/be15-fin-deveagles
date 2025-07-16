<script setup>
  import { ref } from 'vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';

  const props = defineProps({
    modelValue: {
      type: Boolean,
      required: true,
    },
    messageContent: {
      type: Object,
      required: true,
    },
    customers: {
      type: Array,
      required: true,
    },
  });

  const emit = defineEmits(['update:modelValue', 'confirm']);

  function toKSTISOString(date) {
    const tzOffset = date.getTimezoneOffset() * 60000; // 540분 = 9시간
    return new Date(date - tzOffset).toISOString().slice(0, 16); // 'YYYY-MM-DDTHH:mm'
  }
  const selectedDate = ref(null);

  function close() {
    emit('update:modelValue', false);
  }

  function submit() {
    if (!selectedDate.value) {
      alert('예약 날짜와 시간을 선택해주세요.');
      return;
    }

    emit('confirm', {
      ...props.messageContent,
      messageSendingType: 'RESERVATION', // ✅ 중요
      customerIds: props.customers.map(c => c.id), // ✅ 리스트 형태
      scheduledAt: toKSTISOString(selectedDate.value), // ✅ 예약 시간
    });

    close();
  }
</script>

<template>
  <BaseModal
    :model-value="modelValue"
    title="예약 메시지 등록"
    @update:model-value="val => emit('update:modelValue', val)"
  >
    <div class="modal-body">
      <label class="form-label">메시지 내용</label>
      <div class="message-preview">
        <p><strong>내용:</strong> {{ props.messageContent.content || '없음' }}</p>
        <p v-if="props.messageContent.link">
          <strong>링크:</strong> {{ props.messageContent.link }}
        </p>
        <p v-if="props.messageContent.coupon">
          <strong>쿠폰:</strong> {{ props.messageContent.coupon.name }}
        </p>
        <p v-if="props.messageContent.grades?.length">
          <strong>등급:</strong> {{ props.messageContent.grades.join(', ') }}
        </p>
        <p v-if="props.messageContent.tags?.length">
          <strong>태그:</strong> {{ props.messageContent.tags.join(', ') }}
        </p>
        <p v-if="props.customers?.length">
          <strong>고객:</strong>
          {{ props.customers.map(c => c.name).join(', ') }}
        </p>
      </div>

      <PrimeDatePicker
        v-model="selectedDate"
        label="발송 날짜 및 시간"
        :min-date="new Date()"
        placeholder="날짜 및 시간을 선택하세요"
        :show-button-bar="true"
        :show-time="true"
        :hour-format="'24'"
        :selection-mode="'single'"
      />
    </div>

    <template #footer>
      <div class="footer-buttons">
        <BaseButton type="primary" @click="submit">예약 발송</BaseButton>
        <BaseButton type="ghost" @click="close">취소</BaseButton>
      </div>
    </template>
  </BaseModal>
</template>

<style scoped>
  .modal-body {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .message-preview {
    padding: 12px;
    border-radius: 6px;
    background-color: var(--color-gray-50);
    border: 1px solid var(--color-gray-200);
    font-size: 14px;
    color: var(--color-gray-800);
    white-space: pre-wrap;
    min-height: 80px;
  }

  .footer-buttons {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }

  .message-preview p {
    margin: 4px 0;
    font-size: 14px;
  }
</style>
