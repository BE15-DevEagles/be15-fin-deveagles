<template>
  <Teleport to="body">
    <div class="edit-modal-layer">
      <BaseModal v-model="isVisible" title="회원권 수정">
        <div class="edit-form">
          <!-- 조건부 필드: 선불권 -->
          <div v-if="type === 'PREPAID'" class="form-group">
            <BaseForm
              v-model.number="remaining"
              label="잔여 선불액"
              type="number"
              step="100"
              placeholder="잔여 선불권 금액"
            />
          </div>

          <!-- 조건부 필드: 횟수권 -->
          <div v-if="type === 'COUNT'" class="form-group">
            <BaseForm
              v-model.number="remaining"
              label="잔여 횟수"
              type="number"
              placeholder="잔여 횟수 입력"
            />
          </div>

          <!-- 공통 필드: 만료일 -->
          <div class="form-group">
            <label>만료일</label>
            <PrimeDatePicker v-model="expiry" placeholder="날짜 선택" append-to="body" />
          </div>
        </div>

        <template #footer>
          <div class="footer-buttons">
            <BaseButton class="primary" outline @click="close">닫기</BaseButton>
            <BaseButton class="primary" @click="submitEdit">수정</BaseButton>
          </div>
        </template>
      </BaseModal>
    </div>
  </Teleport>
</template>

<script setup>
  import { ref, watch } from 'vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';

  const props = defineProps({
    modelValue: Boolean,
    membership: {
      type: Object,
      required: true,
    },
  });
  const emit = defineEmits(['update:modelValue', 'submit']);

  const isVisible = ref(props.modelValue);
  const remaining = ref(null);
  const expiry = ref(null);
  const type = ref('');

  // props.modelValue → isVisible 로 동기화
  watch(
    () => props.modelValue,
    val => {
      isVisible.value = val;
    }
  );
  watch(isVisible, val => {
    emit('update:modelValue', val);
  });

  watch(
    () => props.membership,
    val => {
      if (!val) return;
      type.value = val.type;
      remaining.value = val.remaining ?? null;
      expiry.value = val.expiry ?? null;
    },
    { immediate: true }
  );

  const close = () => {
    isVisible.value = false;
  };

  const submitEdit = () => {
    emit('submit', {
      id: props.membership.id,
      type: type.value,
      remaining: remaining.value,
      expiry: expiry.value,
    });
    emit('updated'); // ✅ 부모에게 알림
    isVisible.value = false;
  };
</script>

<style scoped>
  .edit-modal-layer {
    position: fixed;
    inset: 0;
    z-index: 11000;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .edit-form {
    padding: 1rem 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .footer-buttons {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding: 0 1.5rem 1rem;
  }
</style>

<style>
  .modal-backdrop {
    z-index: 11000;
  }

  .p-datepicker {
    z-index: 99999 !important;
  }
</style>
