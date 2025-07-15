<script setup>
  import { computed, ref, watch, nextTick } from 'vue';
  import BaseDrawer from '@/components/common/BaseDrawer.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import Pagination from '@/components/common/Pagination.vue';

  const props = defineProps({
    modelValue: Boolean,
    customers: {
      type: Array,
      default: () => [],
    },
    pageSize: {
      type: Number,
      default: 20,
    },
  });

  const emit = defineEmits(['update:modelValue', 'select']);

  const drawerOpen = computed({
    get: () => props.modelValue,
    set: val => emit('update:modelValue', val),
  });

  const selected = ref([]);
  const currentPage = ref(1);
  const search = ref('');

  const filteredCustomers = computed(() => {
    const keyword = search.value.trim().toLowerCase();
    return props.customers.filter(
      c => c.name?.toLowerCase().includes(keyword) || c.phone?.toLowerCase().includes(keyword)
    );
  });

  const totalElements = computed(() => filteredCustomers.value.length);

  const pagedCustomers = computed(() => {
    const start = (currentPage.value - 1) * props.pageSize;
    return filteredCustomers.value.slice(start, start + props.pageSize);
  });

  const totalPages = computed(() => Math.ceil(totalElements.value / props.pageSize));

  watch(drawerOpen, async opened => {
    if (opened) {
      await nextTick();
      const el = document.querySelector('.drawer-backdrop');
      if (el) el.style.zIndex = '1200';
      currentPage.value = 1;
      selected.value = [];
      search.value = '';
    }
  });

  function toggleSelect(customer) {
    const exists = selected.value.find(c => c.phone === customer.phone);
    if (exists) {
      selected.value = selected.value.filter(c => c.phone !== customer.phone);
    } else {
      selected.value.push(customer);
    }
  }

  function isSelected(customer) {
    return selected.value.some(c => c.phone === customer.phone);
  }

  function onPageChange(page) {
    currentPage.value = page;
  }

  function onSearchChange(event) {
    search.value = event.target.value;
    currentPage.value = 1;
  }

  function confirmSelect() {
    emit('select', selected.value);
  }
</script>

<template>
  <BaseDrawer v-model="drawerOpen" title="고객 선택">
    <div class="search-box">
      <input
        :value="search"
        type="text"
        class="input input--md"
        placeholder="이름 또는 번호로 검색"
        @input="onSearchChange"
      />
    </div>

    <div v-if="selected.length" class="selected-preview">
      <div v-for="customer in selected" :key="customer.phone" class="selected-item">
        {{ customer.name }}
        <span class="remove" @click.stop="toggleSelect(customer)">×</span>
      </div>
    </div>

    <div class="list-section">
      <div
        v-for="customer in pagedCustomers"
        :key="customer.phone"
        class="list-item"
        :class="{ selected: isSelected(customer) }"
        @click="toggleSelect(customer)"
      >
        <div class="list-item-content">
          <div class="list-item-title">{{ customer.name }}</div>
          <div class="list-item-sub">{{ customer.phone }}</div>
        </div>
        <div class="list-item-check">
          <div class="checkbox" :class="{ checked: isSelected(customer) }"></div>
        </div>
      </div>
    </div>

    <Pagination
      v-if="totalPages > 1"
      :current-page="currentPage"
      :total-pages="totalPages"
      :total-items="totalElements"
      :items-per-page="pageSize"
      @page-change="onPageChange"
    />

    <template #footer>
      <BaseButton type="primary" class="w-full" @click="confirmSelect"> 선택 완료 </BaseButton>
    </template>
  </BaseDrawer>
</template>

<style scoped>
  .search-box {
    padding: 16px 16px 0;
  }
  .selected-preview {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding: 12px 16px 0;
  }
  .selected-item {
    background-color: var(--color-primary-100);
    color: var(--color-primary-600);
    padding: 4px 8px;
    border-radius: 12px;
    font-size: 13px;
    display: flex;
    align-items: center;
    gap: 4px;
  }
  .selected-item .remove {
    cursor: pointer;
    font-weight: bold;
  }
  .selected-item .remove:hover {
    color: var(--color-error-500);
  }
  .list-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 16px;
  }
  .list-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    border: 1px solid var(--color-gray-200);
    border-radius: 8px;
    padding: 12px 16px;
    cursor: pointer;
    transition: all 0.2s ease;
  }
  .list-item:hover {
    background-color: var(--color-gray-50);
  }
  .list-item.selected {
    border-color: var(--color-primary-500);
    background-color: var(--color-primary-50);
  }
  .list-item-content {
    display: flex;
    flex-direction: column;
  }
  .list-item-title {
    font-weight: 600;
    font-size: 14px;
  }
  .list-item-sub {
    font-size: 13px;
    color: var(--color-gray-500);
  }
  .list-item-check {
    width: 20px;
    height: 20px;
  }
  .checkbox {
    width: 20px;
    height: 20px;
    border-radius: 4px;
    border: 2px solid var(--color-gray-300);
  }
  .checkbox.checked {
    background-color: var(--color-primary-500);
    border-color: var(--color-primary-500);
  }
</style>
