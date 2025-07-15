<script setup>
  import { ref, computed, nextTick, watch, onMounted, onBeforeUnmount } from 'vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';

  const props = defineProps({
    modelValue: Boolean,
    template: {
      type: Object,
      required: true,
    },
  });

  const emit = defineEmits(['update:modelValue', 'submit']);

  const visible = computed({
    get: () => props.modelValue,
    set: val => emit('update:modelValue', val),
  });

  // 입력값 상태
  const name = ref('');
  const content = ref('');
  const grade = ref('');
  const tags = ref([]);
  const type = ref('');

  const showDropdown = ref(false);
  const contentWrapper = ref(null);
  const dropdownWrapper = ref(null);

  const variables = [
    '#{고객명}',
    '#{예약확정일}',
    '#{예약날짜}',
    '#{예약변경일}',
    '#{예약취소일}',
    '#{선불권금액}',
    '#{횟수권횟수}',
  ];

  // 변수 삽입
  function insertVariable(variable) {
    const textarea = contentWrapper.value?.querySelector('textarea');
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const before = content.value.slice(0, start);
    const after = content.value.slice(end);

    content.value = before + variable + after;

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

  onMounted(() => {
    window.addEventListener('click', handleClickOutside);
  });
  onBeforeUnmount(() => {
    window.removeEventListener('click', handleClickOutside);
  });

  watch(
    () => props.modelValue,
    val => {
      if (val && props.template?.id) {
        name.value = props.template.name ?? '';
        content.value = props.template.content ?? '';
        grade.value = props.template.grade ?? '';
        tags.value = props.template.tags ?? [];
        type.value = props.template.type ?? '안내';
      }
    },
    { immediate: true }
  );

  function close() {
    visible.value = false;
  }

  function submit() {
    if (!name.value.trim() || !content.value.trim()) return;

    emit('submit', {
      id: props.template.id,
      name: name.value,
      content: content.value,
      grade: grade.value,
      tags: tags.value,
      type: type.value,
      createdAt: props.template.createdAt,
    });

    close();
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
            <BaseButton size="xs" type="ghost" @click.stop="toggleDropdown">
              변수 삽입 ▼
            </BaseButton>

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
        :options="['안내', '광고', '기타']"
        placeholder="유형 선택"
      />

      <BaseForm
        v-model="grade"
        type="select"
        label="대상 등급"
        :options="['전체', 'VIP', 'VVIP']"
        placeholder="등급 선택"
      />

      <BaseForm
        v-model="tags"
        type="multiselect"
        label="고객 태그"
        :options="['재방문', '신규', '이탈위험']"
        placeholder="고객 태그 선택"
      />

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
</style>
