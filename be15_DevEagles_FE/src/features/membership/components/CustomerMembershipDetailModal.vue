<template>
  <Teleport to="body">
    <transition name="modal-fade">
      <div v-if="modelValue" class="custom-modal-backdrop" @click.self="close">
        <div class="custom-modal">
          <div class="modal-header">
            <h2>고객 회원권 상세 조회</h2>
            <button class="modal-close" @click="close">×</button>
          </div>

          <div class="modal-content">
            <!-- 고객 정보 -->
            <div class="section">
              <h3>고객 정보</h3>
              <div class="info-grid">
                <div><BaseForm label="이름" :model-value="customer.name" readonly /></div>
                <div><BaseForm label="연락처" :model-value="customer.phone" readonly /></div>
              </div>
            </div>

            <!-- 보유 회원권 -->
            <div class="section">
              <h3>보유 회원권</h3>
              <div class="tab-row">
                <BaseButton
                  v-for="tab in ['PREPAID', 'SESSION', 'EXPIRED']"
                  :key="tab"
                  :type="activeTab === tab ? 'primary' : 'ghost'"
                  @click="activeTab = tab"
                >
                  {{
                    tab === 'PREPAID' ? '선불권' : tab === 'SESSION' ? '횟수권' : '만료/사용완료'
                  }}
                </BaseButton>
              </div>

              <div class="base-table-wrapper scrollable-table">
                <BaseTable
                  :columns="membershipColumns"
                  :data="filteredMemberships"
                  row-key="id"
                  @row-click="openEditModal"
                >
                  <template #cell-type="{ value }">
                    {{ value === 'PREPAID' ? '선불권' : value === 'SESSION' ? '횟수권' : value }}
                  </template>
                </BaseTable>
              </div>
            </div>
          </div>

          <BaseToast ref="toastRef" />
        </div>
      </div>
    </transition>
  </Teleport>

  <!-- 수정 모달 -->
  <CustomerMembershipEditModal
    v-if="editModalVisible"
    v-model="editModalVisible"
    :membership="selectedMembership"
    @updated="handleUpdated"
  />
</template>

<script setup>
  import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
  import dayjs from 'dayjs';
  import {
    getCustomerPrepaidPasses,
    getCustomerSessionPasses,
    getExpiredOrUsedUpMemberships,
  } from '@/features/membership/api/membership.js';

  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseTable from '@/components/common/BaseTable.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import CustomerMembershipEditModal from '@/features/membership/components/CustomerMembershipEditModal.vue';

  const props = defineProps({
    modelValue: Boolean,
    customer: {
      type: Object,
      required: true,
      default: () => ({
        id: null,
        name: '',
        phone: '',
        usageHistory: [],
      }),
    },
  });

  const emit = defineEmits(['update:modelValue']);
  const close = () => emit('update:modelValue', false);

  const toastRef = ref(null);
  const memberships = ref([]);
  const activeTab = ref('PREPAID');
  const editModalVisible = ref(false);
  const selectedMembership = ref(null);

  const formatDate = value => {
    return value ? dayjs(value).format('YYYY-MM-DD') : '';
  };

  const handleKeydown = e => {
    if (e.key === 'Escape') {
      close();
    }
  };

  onMounted(() => {
    window.addEventListener('keydown', handleKeydown);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown);
  });

  const loadMemberships = async customerId => {
    try {
      const [prepaid, session, expired] = await Promise.all([
        getCustomerPrepaidPasses(customerId),
        getCustomerSessionPasses(customerId),
        getExpiredOrUsedUpMemberships(customerId),
      ]);

      const today = dayjs();
      const isValid = entry => {
        const isExpired =
          entry.expirationDate && dayjs(entry.expirationDate).isBefore(today, 'day');
        const isUsedUp = entry.remainingAmount === 0 || entry.remainingCount === 0;
        return !(isExpired || isUsedUp);
      };

      const filteredPrepaid = prepaid.filter(isValid);
      const filteredSession = session.filter(isValid);

      const expiredPrepaid = expired.plist ?? [];
      const expiredSession = expired.slist ?? [];

      memberships.value = [
        ...filteredPrepaid.map(p => ({
          id: p.customerPrepaidPassId,
          name: p.prepaidPassName,
          type: 'PREPAID',
          remaining: p.remainingAmount,
          expiry: formatDate(p.expirationDate),
          create: formatDate(p.createdAt),
        })),
        ...filteredSession.map(s => ({
          id: s.customerSessionPassId,
          name: s.secondaryItemName
            ? `(${s.secondaryItemName}) ${s.sessionPassName}`
            : s.sessionPassName,
          type: 'SESSION',
          remaining: s.remainingCount,
          expiry: formatDate(s.expirationDate),
          create: formatDate(s.createdAt),
        })),
        ...expiredPrepaid.map(e => ({
          id: e.customerPrepaidPassId,
          name: e.prepaidPassName,
          type: 'PREPAID',
          remaining: e.remainingAmount,
          expiry: formatDate(e.expirationDate),
          create: formatDate(e.createdAt),
          isExpired: true,
        })),
        ...expiredSession.map(e => ({
          id: e.customerSessionPassId,
          name: e.secondaryItemName
            ? `(${e.secondaryItemName}) ${e.sessionPassName}`
            : e.sessionPassName,
          type: 'SESSION',
          remaining: e.remainingCount,
          expiry: formatDate(e.expirationDate),
          create: formatDate(e.createdAt),
          isExpired: true,
        })),
      ];
    } catch (e) {
      console.error(e);
      toastRef.value?.error('회원권 데이터를 불러오는 데 실패했습니다.');
    }
  };

  watch(
    () => props.customer?.id,
    customerId => {
      if (customerId) {
        loadMemberships(customerId);
      }
    },
    { immediate: true }
  );

  const expiredMemberships = computed(() => {
    const today = dayjs();
    return memberships.value.filter(m => {
      const isExpired = m.expiry && dayjs(m.expiry).isBefore(today, 'day');
      const isUsedUp = m.remaining === 0;
      return isExpired || isUsedUp;
    });
  });

  const filteredMemberships = computed(() => {
    if (activeTab.value === 'EXPIRED') return expiredMemberships.value;
    return memberships.value.filter(m => m.type === activeTab.value && !m.isExpired);
  });

  const membershipColumns = [
    { key: 'name', title: '회원권명', width: '180px' },
    { key: 'type', title: '종류', width: '100px' },
    { key: 'remaining', title: '잔여', width: '80px' },
    { key: 'create', title: '결제일', width: '120px' },
    { key: 'expiry', title: '만료일', width: '120px' },
  ];

  const usageColumns = [
    { key: 'date', title: '사용일', width: '120px' },
    { key: 'detail', title: '내용', width: '300px' },
    { key: 'change', title: '증감', width: '100px' },
  ];

  const openEditModal = row => {
    selectedMembership.value = row;
    editModalVisible.value = true;
  };

  const handleUpdated = () => {
    toastRef.value?.success('고객 회원권 정보가 수정되었습니다.');
    if (props.customer?.id) {
      loadMemberships(props.customer.id);
    }
  };
</script>

<style scoped>
  .section h3 {
    margin-bottom: 1rem;
  }
  .custom-modal-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 500;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .custom-modal {
    background: white;
    width: 800px;
    height: 800px;
    border-radius: 8px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
  }
  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem 1.5rem;
    border-bottom: 1px solid #eee;
  }
  .modal-close {
    background: none;
    border: none;
    font-size: 20px;
    padding: 0 8px;
    line-height: 1;
  }
  .modal-content {
    padding: 1.5rem;
    flex: 1;
    overflow-y: auto;
  }
  .section {
    margin-top: 1.5rem;
  }
  .info-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 1rem;
  }
  .tab-row {
    display: flex;
    gap: 0.5rem;
    margin: 1rem 0;
  }
  .base-table-wrapper {
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    padding: 16px;
    margin-top: 0.5rem;
  }
  .scrollable-table {
    max-height: 200px;
    overflow-y: auto;
  }
</style>
