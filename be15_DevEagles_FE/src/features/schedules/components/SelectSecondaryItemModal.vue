<template>
  <BaseModal v-model="show" title="상품 선택" :z-index="2000">
    <div class="tab-wrapper">
      <button
        v-for="primary in primaryItems"
        :key="primary.primaryItemId"
        :class="{ active: selectedPrimaryId === primary.primaryItemId }"
        @click="selectedPrimaryId = primary.primaryItemId"
      >
        {{ primary.primaryItemName }}
      </button>
    </div>

    <div class="item-list">
      <div
        v-for="item in filteredSecondaryItems"
        :key="item.secondaryItemId"
        class="item-card"
        :class="{ selected: isSelected(item.secondaryItemId) }"
        @click="selectItem(item)"
      >
        <p class="item-name">{{ item.secondaryItemName }}</p>
        <p class="item-price">{{ item.secondaryItemPrice.toLocaleString() }}원</p>
        <p v-if="item.timeTaken != null" class="item-time">{{ item.timeTaken }}분</p>
      </div>
    </div>
  </BaseModal>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import {
    getAllPrimaryItems,
    getActiveSecondaryItems,
  } from '@/features/schedules/api/schedules.js';

  const show = defineModel();
  const emit = defineEmits(['select']);

  const props = defineProps({
    selectedIds: {
      type: Array,
      default: () => [],
    },
  });

  const primaryItems = ref([]);
  const secondaryItems = ref([]);
  const selectedPrimaryId = ref(null);

  const filteredSecondaryItems = computed(() =>
    secondaryItems.value.filter(item => item.primaryItemId === selectedPrimaryId.value)
  );

  onMounted(async () => {
    try {
      const primaryRes = await getAllPrimaryItems();
      primaryItems.value = primaryRes;

      if (primaryRes.length > 0) {
        selectedPrimaryId.value = primaryRes[0].primaryItemId;
      }

      const secondaryRes = await getActiveSecondaryItems();
      secondaryItems.value = secondaryRes;
    } catch (error) {
      console.error('❌ 아이템 로딩 실패', error);
    }
  });

  const selectItem = item => {
    const primary = primaryItems.value.find(p => p.primaryItemId === selectedPrimaryId.value);
    emit('select', {
      ...item,
      primaryItemName: primary?.primaryItemName ?? '기타',
    });
    show.value = false;
  };
  const isSelected = id => props.selectedIds.includes(id);
</script>

<style scoped>
  .tab-wrapper {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
    flex-wrap: wrap;
  }

  .tab-wrapper button {
    padding: 8px 12px;
    border: 1px solid var(--color-gray-300);
    border-radius: 6px;
    background-color: white;
    cursor: pointer;
    transition: 0.2s;
  }

  .tab-wrapper button.active {
    background-color: var(--color-primary-main);
    color: white;
    border-color: var(--color-primary-main);
  }

  .item-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    max-height: 300px;
    overflow-y: auto;
  }

  .item-card {
    width: calc(50% - 6px);
    padding: 12px;
    border: 1px solid var(--color-gray-200);
    border-radius: 8px;
    cursor: pointer;
    transition: 0.2s;
    background-color: #f9f9f9;
  }

  .item-card:hover {
    background-color: var(--color-primary-50);
  }
  .item-card.selected {
    border-color: var(--color-primary-main);
    background-color: var(--color-primary-50);
  }
  .item-name {
    font-weight: 600;
    margin-bottom: 4px;
  }

  .item-price,
  .item-time {
    font-size: 14px;
    color: var(--color-gray-600);
  }
</style>
