<script setup>
  import BaseButton from '@/components/common/BaseButton.vue';
  import TrashIcon from '@/components/icons/TrashIcon.vue';
  import EditIcon from '@/components/icons/EditIcon.vue';

  const props = defineProps({
    template: {
      type: Object,
      required: true,
    },
    columnWidths: {
      type: Object,
      required: true,
    },
  });

  const emit = defineEmits(['edit', 'delete', 'open-detail']);

  function handleEdit() {
    emit('edit', props.template);
  }

  function handleDelete() {
    emit('delete', props.template);
  }

  function handleDetailOpen() {
    emit('open-detail', props.template);
  }

  // 템플릿 유형 매핑
  const typeLabelMap = {
    advertising: '광고',
    announcement: '안내',
    etc: '기타',
  };
</script>

<template>
  <tr>
    <td class="text-center" :style="{ width: columnWidths.name }">
      {{ template.templateName }}
    </td>

    <td class="text-center" :style="{ width: columnWidths.type }">
      <span class="badge">{{ typeLabelMap[template.templateType] || '기타' }}</span>
    </td>

    <td
      class="text-center clickable"
      :style="{ width: columnWidths.content }"
      @click="handleDetailOpen"
    >
      <div class="truncate">
        {{ template.templateContent }}
      </div>
    </td>

    <td class="text-center" :style="{ width: columnWidths.createdAt }">
      {{ template.createdAt ? template.createdAt.slice(0, 10) : '-' }}
    </td>

    <td class="text-center" :style="{ width: columnWidths.actions }">
      <div class="action-cell">
        <BaseButton size="xs" icon aria-label="수정" @click="handleEdit">
          <EditIcon class="icon" />
        </BaseButton>
        <BaseButton size="xs" icon aria-label="삭제" @click="handleDelete">
          <TrashIcon class="icon" />
        </BaseButton>
      </div>
    </td>
  </tr>
</template>

<style scoped>
  .truncate {
    max-width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .clickable {
    cursor: pointer;
    color: var(--color-primary-main);
  }

  .action-cell {
    display: flex;
    justify-content: center;
    gap: 0.5rem;
  }

  .badge {
    display: inline-block;
    background-color: var(--color-gray-100);
    border-radius: 4px;
    padding: 2px 8px;
    font-size: 12px;
    color: var(--color-text-secondary);
  }
</style>
