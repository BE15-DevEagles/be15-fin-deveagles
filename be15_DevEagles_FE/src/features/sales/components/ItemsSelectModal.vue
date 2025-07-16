<template>
  <Teleport to="body">
    <div class="overlay" @click.self="closeModal">
      <div class="modal-box">
        <div class="modal-header">
          <h2 class="title">상품 선택</h2>
          <button type="ghost" size="sm" @click="closeModal">×</button>
        </div>

        <div class="modal-content">
          <!-- 탭 -->
          <div class="tab-buttons">
            <BaseButton
              v-for="tab in tabs"
              :key="tab"
              :type="selectedTab === tab ? 'primary' : 'ghost'"
              class="tab-button"
              @click="selectedTab = tab"
            >
              {{ tab === 'SERVICE' ? '시술' : '제품' }}
            </BaseButton>
          </div>

          <!-- 상품 목록 -->
          <div class="product-list scrollable">
            <div
              v-for="product in filteredProducts"
              :key="product.id"
              :class="['product', selectedIds.includes(product.id) ? 'selected' : '']"
              @click="toggleSelection(product)"
            >
              <div class="name">{{ product.name }}</div>
              <div class="price">{{ formatPrice(product.price) }}</div>
            </div>
          </div>

          <!-- 선택된 상품 뱃지 -->
          <div class="selected-products scrollable">
            <div v-for="product in selectedProducts" :key="product.id" class="selected-badge">
              {{ product.name }}
              <span class="remove-btn" @click.stop="toggleSelection(product)">×</span>
            </div>
          </div>
        </div>

        <!-- 하단 버튼 -->
        <div class="footer-buttons">
          <BaseButton type="primary" outline @click="closeModal">취소</BaseButton>
          <BaseButton type="primary" @click="applySelection">상품 선택 완료</BaseButton>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
  import { ref, computed, defineEmits, onMounted, watch } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import { getActiveAllSecondaryItems } from '@/features/items/api/items.js';

  const props = defineProps({
    modelValue: { type: Boolean, default: false },
    selectedItemIds: { type: Array, default: () => [] },
  });

  const emit = defineEmits(['close', 'apply', 'update:modelValue']);

  const tabs = ['SERVICE', 'PRODUCT'];
  const selectedTab = ref('SERVICE');
  const selectedIds = ref([]);
  const productList = ref([]);
  const loading = ref(false);

  onMounted(async () => {
    try {
      loading.value = true;
      const result = await getActiveAllSecondaryItems();
      productList.value = result.map(item => ({
        id: item.secondaryItemId,
        name: item.secondaryItemName,
        price: item.secondaryItemPrice,
        type: item.timeTaken === null ? 'PRODUCT' : 'SERVICE',
      }));
    } catch (e) {
      console.error('상품 목록 조회 실패:', e);
    } finally {
      loading.value = false;
    }
  });

  watch(
    () => props.selectedItemIds,
    newVal => {
      selectedIds.value = [...newVal];
    },
    { immediate: true }
  );

  const filteredProducts = computed(() =>
    productList.value.filter(p => p.type === selectedTab.value)
  );

  const selectedProducts = computed(() =>
    productList.value.filter(p => selectedIds.value.includes(p.id))
  );

  const toggleSelection = product => {
    const index = selectedIds.value.indexOf(product.id);
    if (index > -1) {
      selectedIds.value.splice(index, 1);
    } else {
      selectedIds.value.push(product.id);
    }
  };

  const applySelection = () => {
    const selectedItems = productList.value.filter(p => selectedIds.value.includes(p.id));
    emit('apply', selectedItems);
    emit('update:modelValue', false);
  };

  const closeModal = () => {
    emit('update:modelValue', false); // 모달 닫기
  };

  const formatPrice = price => `${price.toLocaleString()}원`;
</script>

<style scoped>
  .overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.3);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 9999;
  }

  .modal-box {
    width: 500px;
    height: 600px;
    background: white;
    border-radius: 8px;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #ddd;
  }

  .title {
    font-size: 18px;
    font-weight: bold;
  }

  .modal-content {
    flex: 1;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    overflow: hidden;
  }

  .tab-buttons {
    display: flex;
    gap: 8px;
  }

  .tab-buttons button {
    flex: 1;
  }

  .tab-buttons button.active {
    background: #364f6b;
    color: white;
  }

  .scrollable {
    overflow-y: auto;
    max-height: 150px;
  }

  .product-list {
    flex: none;
    max-height: 280px;
    overflow-y: auto;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .product {
    width: calc(50% - 4px);
    border: 2px solid #ccc;
    border-radius: 6px;
    padding: 8px;
    cursor: pointer;
  }

  .product.selected {
    border: 2px solid #364f6b;
    background: #f0f0f0;
  }

  .product .name {
    font-size: 14px;
    margin-bottom: 4px;
  }

  .product .price {
    font-size: 12px;
    color: #666;
  }

  .selected-products {
    overflow-y: auto;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 8px;
  }

  .selected-badge {
    background: #ff6b91;
    color: white;
    border-radius: 16px;
    padding: 4px 12px;
    font-size: 13px;
    display: flex;
    align-items: center;
  }

  .selected-badge .remove-btn {
    margin-left: 8px;
    font-weight: bold;
    cursor: pointer;
  }

  .footer-buttons {
    margin-top: 16px; /* 본문과의 간격 */
    padding: 16px 24px; /* 내부 여백 */
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 8px;
    border-top: 1px solid #ddd;
    background-color: white;
    flex-shrink: 0;
    min-height: 64px; /* 버튼 영역 높이 최소 확보 */
  }
  .footer-buttons button {
    min-height: 36px;
    padding: 6px 16px;
    font-size: 14px;
  }
</style>
