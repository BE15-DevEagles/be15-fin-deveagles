<script setup>
  import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue';
  import TemplatesAPI from '@/features/messages/api/templates.js';

  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';

  const props = defineProps({
    modelValue: Boolean,
  });
  const emit = defineEmits(['update:modelValue', 'success']);

  const visible = computed({
    get: () => props.modelValue,
    set: val => emit('update:modelValue', val),
  });

  const name = ref('');
  const content = ref('');
  const grade = ref('전체');
  const tags = ref('');
  const type = ref('안내');
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

  function close() {
    visible.value = false;
  }

  async function submit() {
    if (!name.value || !content.value) return;

    const typeEnum = {
      안내: 'announcement',
      광고: 'advertising',
      기타: 'etc',
    };

    const payload = {
      templateName: name.value,
      templateContent: content.value,
      templateType: typeEnum[type.value] ?? 'announcement',
      customerGradeId: null,
      tagId: null,
    };

    try {
      await TemplatesAPI.createTemplate(payload);
      emit('success');
      close();
    } catch (error) {
      alert('템플릿 등록에 실패했습니다.');
    }
  }
</script>

<template>
  <BaseModal v-model="visible" title="템플릿 등록">
    <div class="space-y-4">
      <BaseForm v-model="name" label="템플릿명" placeholder="예: 예약 안내" />

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
            :rows="8"
            placeholder="메시지 내용을 입력하세요"
          />
        </div>
      </div>

      <BaseForm
        v-model="type"
        label="유형"
        type="select"
        :options="[
          { value: '안내', text: '안내' },
          { value: '광고', text: '광고' },
          { value: '기타', text: '기타' },
        ]"
      />

      <BaseForm
        v-model="grade"
        label="대상 등급"
        type="select"
        :options="[
          { value: '전체', text: '전체' },
          { value: 'VIP', text: 'VIP' },
          { value: '단골', text: '단골' },
          { value: '신규', text: '신규' },
        ]"
      />

      <BaseForm v-model="tags" label="태그 (쉼표로 구분)" placeholder="예: 여름,이벤트,첫방문" />

      <div class="flex justify-end mt-4">
        <BaseButton type="error" @click="close">취소</BaseButton>
        <BaseButton type="primary" :disabled="!name || !content" class="ml-3" @click="submit"
          >등록</BaseButton
        >
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
    padding: 4px 0;
  }
  .insert-item {
    @apply py-1 px-2 text-sm hover:bg-gray-100 rounded cursor-pointer;
  }
</style>
