<script setup>
  import { ref, computed, nextTick, watch, onMounted, onBeforeUnmount } from 'vue';
  import TemplatesAPI from '@/features/messages/api/templates.js';

  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';

  const props = defineProps({
    modelValue: Boolean,
    template: Object,
    grades: Array,
  });
  const emit = defineEmits(['update:modelValue', 'success']);

  const visible = computed({
    get: () => props.modelValue,
    set: val => emit('update:modelValue', val),
  });

  const name = ref('');
  const content = ref('');
  const type = ref('');
  const selectedGradeId = ref(null);

  const showDropdown = ref(false);
  const contentWrapper = ref(null);
  const dropdownWrapper = ref(null);

  const variables = [
    '#{고객명}',
    '#{예약날짜}',
    '#{횟수권잔여횟수}',
    '#{선불권잔여금액}',
    '#{프로필링크}',
    '#{인스타url}',
    '#{상점명}',
  ];

  function insertVariable(variable) {
    const textarea = contentWrapper.value?.querySelector('textarea');
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    content.value = content.value.slice(0, start) + variable + content.value.slice(end);
    nextTick(() => {
      textarea.focus();
      textarea.selectionStart = textarea.selectionEnd = start + variable.length;
    });
  }

  function toggleDropdown() {
    showDropdown.value = !showDropdown.value;
  }
  function handleClickOutside(event) {
    if (dropdownWrapper.value && !dropdownWrapper.value.contains(event.target)) {
      showDropdown.value = false;
    }
  }
  onMounted(() => window.addEventListener('click', handleClickOutside));
  onBeforeUnmount(() => window.removeEventListener('click', handleClickOutside));

  watch(
    () => props.template,
    template => {
      if (template) {
        name.value = template.templateName ?? '';
        content.value = template.templateContent ?? '';
        type.value = template.templateType ?? 'announcement';
        selectedGradeId.value = template.customerGradeId ?? null;
      }
    },
    { immediate: true }
  );

  function close() {
    visible.value = false;
  }

  async function submit() {
    if (!name.value.trim() || !content.value.trim()) return;

    const payload = {
      templateId: props.template.templateId,
      templateName: name.value,
      templateContent: content.value,
      templateType: type.value,
      customerGradeId: selectedGradeId.value || null,
    };

    try {
      await TemplatesAPI.updateTemplate(payload.templateId, payload);
      emit('success');
      close();
    } catch (e) {
      alert('템플릿 수정에 실패했습니다.');
    }
  }
</script>

<template>
  <BaseModal v-model="visible" title="템플릿 수정">
    <div class="space-y-4">
      <BaseForm v-model="name" type="input" label="템플릿명" placeholder="예: 예약 안내" />

      <div class="form-group relative z-0">
        <div class="form-label-area">
          <label class="form-label">내용</label>
          <div ref="dropdownWrapper" class="dropdown-wrapper">
            <BaseButton size="xs" type="ghost" @click.stop="toggleDropdown">변수 삽입 ▼</BaseButton>
            <div v-if="showDropdown" class="dropdown-list">
              <div
                v-for="v in variables"
                :key="v"
                class="insert-item"
                @click.stop="insertVariable(v)"
              >
                {{ v }}
              </div>
            </div>
          </div>
        </div>
        <div ref="contentWrapper">
          <BaseForm
            v-model="content"
            type="textarea"
            :rows="10"
            placeholder="메시지 내용을 입력하세요"
          />
        </div>
      </div>

      <BaseForm
        v-model="type"
        type="select"
        label="유형"
        :options="[
          { value: 'announcement', text: '안내' },
          { value: 'advertising', text: '광고' },
          { value: 'etc', text: '기타' },
        ]"
        placeholder="유형 선택"
      />

      <div class="form-row">
        <label class="form-label">대상 등급</label>
        <select v-model="selectedGradeId" class="form-input">
          <option :value="null">전체</option>
          <option v-for="grade in grades" :key="grade.id" :value="grade.id">
            {{ grade.name }}
          </option>
        </select>
      </div>

      <div class="action-buttons mt-4 d-flex justify-content-end gap-2">
        <BaseButton type="ghost" @click="close">취소</BaseButton>
        <BaseButton :disabled="!name.trim() || !content.trim()" @click="submit">수정</BaseButton>
      </div>
    </div>
  </BaseModal>
</template>

<style scoped>
  .form-label-area {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .dropdown-wrapper {
    position: relative;
  }
  .dropdown-list {
    position: absolute;
    top: 100%;
    right: 0;
    margin-top: 4px;
    background: white;
    border: 1px solid #ddd;
    border-radius: 6px;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
    z-index: 999;
    min-width: 160px;
    padding: 4px 0;
  }
  .insert-item {
    @apply py-1 px-2 text-sm hover:bg-gray-100 rounded cursor-pointer;
  }
  .form-row {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .form-label {
    font-size: 14px;
    font-weight: 500;
    color: #222;
  }
  .form-input {
    height: 40px;
    border: 1px solid #ddd;
    border-radius: 8px;
    font-size: 15px;
    padding: 0 12px;
    background: #fff;
    transition: border 0.2s;
  }
  .form-input:focus {
    outline: none;
    border-color: #364f6b;
    background: #f8fafd;
  }
</style>
