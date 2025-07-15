<template>
  <Teleport to="body">
    <div class="overlay" @click.self="closeModal">
      <div class="modal-box">
        <div class="modal-header">
          <h2 class="title">회원권 선택</h2>
          <button @click="closeModal">×</button>
        </div>

        <div class="content">
          <!-- 탭 -->
          <div class="tab-buttons">
            <BaseButton
              v-for="tab in tabs"
              :key="tab"
              :type="selectedTab === tab ? 'primary' : 'ghost'"
              class="tab-button"
              @click="selectedTab = tab"
            >
              {{ tab === 'PREPAID' ? '선불권' : '횟수권' }}
            </BaseButton>
          </div>

          <!-- 리스트 or 상세 -->
          <div v-if="!selectedItem" class="item-list">
            <div
              v-for="item in membershipList"
              :key="item.id"
              :class="['item', selectedItem?.id === item.id ? 'selected' : '']"
              @click="selectItem(item)"
            >
              {{ getItemName(item) }} : {{ formatPrice(getItemPrice(item)) }}
            </div>
          </div>

          <div v-else class="item-detail">
            <h3 class="detail-title">{{ selectedItem.name }}</h3>
            <p class="detail-price">{{ formatPrice(selectedItem.price) }}</p>

            <div class="field-row">
              <label>유효 기간</label>
              <div>{{ selectedItem.expiration }}</div>
            </div>
            <div class="field-row">
              <label>보너스</label>
              <div>{{ selectedItem.bonus }} 회 / 원</div>
            </div>
            <div class="field-row">
              <label>할인율</label>
              <div>{{ selectedItem.discountRate }}%</div>
            </div>
            <div class="field-row">
              <label>메모</label>
              <div>{{ selectedItem.memo }}</div>
            </div>

            <BaseButton type="ghost" size="sm" class="back-button" @click="selectedItem = null">
              ← 목록으로
            </BaseButton>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
  import { ref, defineProps, defineEmits, watch, onMounted, onBeforeUnmount } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import { getPrepaidPass, getSessionPass } from '@/features/membership/api/membership.js';

  const props = defineProps({ modelValue: Boolean });
  const emit = defineEmits(['update:modelValue', 'apply', 'close']);

  const visible = ref(props.modelValue);
  watch(
    () => props.modelValue,
    val => (visible.value = val)
  );
  watch(visible, val => emit('update:modelValue', val));

  const tabs = ['PREPAID', 'SESSION'];
  const selectedTab = ref('PREPAID');
  const selectedItem = ref(null);
  const membershipList = ref([]);

  const fetchMembershipList = async tab => {
    try {
      const data = tab === 'PREPAID' ? await getPrepaidPass() : await getSessionPass();
      membershipList.value = data.map(item => ({
        ...item,
        type: tab,
        id: item.prepaid_pass_id || item.session_pass_id,
      }));
    } catch (error) {
      console.error('회원권 조회 실패:', error);
    }
  };

  onMounted(() => {
    fetchMembershipList(selectedTab.value);
  });

  watch(selectedTab, newTab => {
    selectedItem.value = null;
    fetchMembershipList(newTab);
  });

  onMounted(() => {
    window.addEventListener('keydown', handleKeydown);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown);
  });

  const handleKeydown = event => {
    if (event.key === 'Escape') {
      closeModal();
    }
  };
  const getItemName = item =>
    item.type === 'PREPAID' ? item.prepaidPassName : item.sessionPassName;

  const getItemPrice = item =>
    item.type === 'PREPAID' ? item.prepaidPassPrice : item.sessionPassPrice;

  const selectItem = item => {
    selectedItem.value = {
      name: getItemName(item),
      price: getItemPrice(item),
      expiration: `${item.expirationPeriod} ${expirationLabel(item.expirationPeriodType)}`,
      bonus: item.bonus ?? 0,
      discountRate: item.discountRate ?? 0,
      memo: item.prepaidPassMemo || item.sessionPassMemo || '-',
    };
    emit('apply', item);
  };

  const expirationLabel = type => {
    switch (type) {
      case 'DAY':
        return '일';
      case 'WEEK':
        return '주';
      case 'MONTH':
        return '개월';
      case 'YEAR':
        return '년';
      default:
        return '';
    }
  };

  const formatPrice = price => (typeof price === 'number' ? `${price.toLocaleString()} 원` : '-');

  const closeModal = () => {
    visible.value = false;
    emit('close');
  };
</script>

<style scoped>
  .overlay {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 2000;
    width: 100%;
    height: 100vh;
    background: rgba(0, 0, 0, 0.3);
    display: flex;
    justify-content: center;
    align-items: center;
  }
  .modal-box {
    width: 500px;
    height: 600px;
    background: white;
    border-radius: 8px;
    overflow-y: auto;
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
  }
  .content {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .tab-buttons {
    display: flex;
    gap: 8px;
  }
  .tab-button {
    flex: 1;
  }
  .item-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .item {
    padding: 10px;
    border: 1px solid #ccc;
    border-radius: 6px;
    cursor: pointer;
  }
  .item.selected {
    background-color: #f0f0f0;
    border-color: black;
  }
  .item-detail {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .detail-title {
    font-size: 16px;
    font-weight: bold;
  }
  .detail-price {
    font-size: 18px;
    font-weight: bold;
    color: #333;
    margin-bottom: 8px;
  }
  .field-row {
    display: flex;
    justify-content: space-between;
    font-size: 14px;
    border-bottom: 1px solid #eee;
    padding: 4px 0;
  }
  .back-button {
    margin-top: 16px;
    text-align: left;
    padding: 0;
  }
</style>
