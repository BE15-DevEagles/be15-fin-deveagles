<template>
  <Teleport to="body">
    <transition name="modal-fade">
      <div v-if="show" class="modal-backdrop" @click.self="closeModal">
        <div class="modal-container">
          <div class="modal-header">
            <h2 class="modal-title">고객 상세정보</h2>
            <button class="modal-close" @click="closeModal">&times;</button>
          </div>
          <div class="modal-body">
            <div v-if="customer" class="customer-detail-content">
              <!-- 상단 정보 -->
              <div class="detail-header">
                <div class="customer-name-phone">
                  <span class="name">{{ processedCustomer.customerName }}</span>
                  <span class="phone">({{ processedCustomer.phoneNumber }})</span>
                </div>
                <div class="actions">
                  <BaseButton type="primary" size="sm" @click="openReservationModal"
                    >예약 등록</BaseButton
                  >
                  <BaseButton type="secondary" size="sm" @click="openSalesModal"
                    >매출 등록</BaseButton
                  >
                  <button class="icon-btn delete-btn" aria-label="삭제" @click="requestDelete">
                    <TrashIcon :size="20" />
                  </button>
                </div>
              </div>
              <div class="detail-sections">
                <div class="info-section">
                  <div class="section-header">
                    <h3 class="section-title">기본정보</h3>
                    <button class="edit-btn" @click="requestEdit">
                      <EditIcon :size="16" />
                      <span>수정</span>
                    </button>
                  </div>
                  <div class="info-group">
                    <div class="info-item">
                      <span class="label">이름</span>
                      <span class="value">{{ processedCustomer.customerName || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">연락처</span>
                      <span class="value">{{ processedCustomer.phoneNumber || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">성별</span>
                      <span class="value">{{ processedCustomer.gender || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">생일</span>
                      <span class="value">{{ processedCustomer.birthdate || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">담당자</span>
                      <span class="value">{{ processedCustomer.staffName }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">등급</span>
                      <span class="value">{{ processedCustomer.customerGradeName }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">태그</span>
                      <span class="value tag-list">
                        <template
                          v-if="processedCustomer.tags && processedCustomer.tags.length > 0"
                        >
                          <BaseBadge
                            v-for="tag in processedCustomer.tags"
                            :key="tag.tagId"
                            :text="tag.tagName"
                            :style="{ backgroundColor: tag.colorCode, color: '#222' }"
                            pill
                          />
                        </template>
                        <template v-else>-</template>
                      </span>
                    </div>
                    <div class="info-item">
                      <span class="label">메모</span>
                      <span class="value">{{ processedCustomer.memo || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">유입경로</span>
                      <span class="value">{{ processedCustomer.acquisitionChannelName }}</span>
                    </div>
                  </div>
                </div>

                <div class="info-section">
                  <div class="section-header">
                    <h3 class="section-title">이용정보</h3>
                  </div>
                  <div class="info-group">
                    <div class="info-item">
                      <span class="label">최초 등록일</span>
                      <span class="value">{{ processedCustomer.createdAt || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">최근 방문일</span>
                      <span class="value">{{ processedCustomer.recentVisitDate || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">방문 횟수</span>
                      <span class="value">{{ processedCustomer.visitCount ?? '-' }} 회</span>
                    </div>
                    <div class="info-item">
                      <span class="label">노쇼 횟수</span>
                      <span class="value">{{ processedCustomer.noshowCount ?? '-' }} 회</span>
                    </div>
                    <div class="info-item">
                      <span class="label">누적 매출액</span>
                      <span class="value"
                        >{{ formatCurrency(processedCustomer.totalRevenue) }} 원</span
                      >
                    </div>
                    <div class="info-item">
                      <span class="label">평균 매출액</span>
                      <span class="value">{{ formatCurrency(avgRevenue) }} 원</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="pass-sections">
                <div class="info-section">
                  <h3 class="section-title">보유 횟수권</h3>
                  <ul
                    v-if="
                      processedCustomer.customerSessionPasses &&
                      processedCustomer.customerSessionPasses.length > 0
                    "
                    class="pass-list"
                  >
                    <li
                      v-for="pass in processedCustomer.customerSessionPasses"
                      :key="pass.customerSessionPassId"
                      class="pass-item"
                    >
                      <span>{{ pass.sessionPassName }}</span>
                      <span>잔여 {{ pass.remainingCount }}회</span>
                      <span>(~{{ pass.expirationDate }})</span>
                    </li>
                  </ul>
                  <div v-else class="no-data">보유 내역 없음</div>
                </div>
                <div class="info-section">
                  <h3 class="section-title">잔여 선불액</h3>
                  <div class="prepaid-balance">
                    {{ formatCurrency(processedCustomer.remainingPrepaidAmount) }} 원
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
  import { computed, ref, watch, onBeforeUnmount } from 'vue';
  import BaseBadge from '@/components/common/BaseBadge.vue';
  import TrashIcon from '@/components/icons/TrashIcon.vue';
  import EditIcon from '@/components/icons/EditIcon.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import {
    getCustomerSessionPasses,
    getCustomerPrepaidPasses,
  } from '@/features/membership/api/membership.js';

  const props = defineProps({
    modelValue: { type: Boolean, required: true },
    customer: { type: Object, default: null },
  });

  const emit = defineEmits([
    'update:modelValue',
    'request-delete',
    'request-edit',
    'request-reservation',
    'request-sales',
  ]);

  const show = computed({
    get: () => props.modelValue,
    set: value => emit('update:modelValue', value),
  });

  const sessionPasses = ref([]);
  const prepaidAmount = ref(0);
  const passesLoading = ref(false);
  const passesError = ref(null);

  async function fetchMembershipData(customerId) {
    if (!customerId) return;
    passesLoading.value = true;
    passesError.value = null;
    try {
      const [session, prepaid] = await Promise.all([
        getCustomerSessionPasses(customerId),
        getCustomerPrepaidPasses(customerId),
      ]);
      sessionPasses.value = session || [];
      prepaidAmount.value =
        prepaid?.reduce?.((acc, cur) => acc + (cur.remainingAmount || 0), 0) || 0;
    } catch (err) {
      passesError.value = err.message || '회원권 정보를 불러오지 못했습니다.';
      sessionPasses.value = [];
      prepaidAmount.value = 0;
    } finally {
      passesLoading.value = false;
    }
  }

  watch(
    () => props.customer?.customerId,
    id => {
      if (show.value && id) fetchMembershipData(id);
    }
  );
  watch(show, v => {
    if (v && props.customer?.customerId) fetchMembershipData(props.customer.customerId);
  });

  function handleEscKey(e) {
    if (e.key === 'Escape') {
      closeModal();
    }
  }

  watch(show, v => {
    if (v) {
      window.addEventListener('keydown', handleEscKey);
    } else {
      window.removeEventListener('keydown', handleEscKey);
    }
  });
  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleEscKey);
  });

  const processedCustomer = computed(() => {
    if (!props.customer) return null;

    const genderMap = { M: '남성', F: '여성' };
    const formatDate = dateString => {
      if (!dateString) return '-';
      return dateString.split('T')[0];
    };

    return {
      ...props.customer,
      customerSessionPasses: sessionPasses.value,
      remainingPrepaidAmount: prepaidAmount.value,
      gender: genderMap[props.customer.gender] || props.customer.gender,
      createdAt: formatDate(props.customer.createdAt),
      birthdate: formatDate(props.customer.birthdate),
      noshowCount: props.customer.noshowCount ?? '-',
      staffName: props.customer.staff?.staffName || props.customer.staffName || '-',
      customerGradeName:
        props.customer.customerGrade?.customerGradeName || props.customer.customerGradeName || '-',
      acquisitionChannelName:
        props.customer.acquisitionChannel?.acquisitionChannelName ||
        props.customer.acquisitionChannelName ||
        '-',
    };
  });

  const closeModal = () => {
    show.value = false;
  };

  const requestDelete = () => {
    if (props.customer) {
      emit('request-delete', props.customer.customerId);
    }
  };

  const requestEdit = () => {
    if (props.customer) {
      emit('request-edit', props.customer);
    }
  };

  const formatCurrency = amount => {
    if (amount === null || amount === undefined || isNaN(amount)) return '0';
    return amount.toLocaleString('ko-KR');
  };

  const avgRevenue = computed(() => {
    if (!props.customer || !props.customer.visitCount) {
      return 0;
    }
    const avg = props.customer.totalRevenue / props.customer.visitCount;
    return isNaN(avg) ? 0 : Math.round(avg);
  });

  const openReservationModal = () => {
    emit('request-reservation', props.customer);
  };

  const openSalesModal = () => {
    emit('request-sales', props.customer);
  };
</script>

<style scoped>
  .modal-backdrop {
    position: fixed;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 10000;
  }
  .modal-container {
    background: #ffffff;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    width: 90%;
    height: auto;
    max-width: 900px;
    max-height: 90vh;
    border-radius: 8px;
    margin: 0;
  }
  .modal-header {
    position: relative;
    padding: 1.5rem 2rem;
    border-bottom: 1px solid #eeeeee;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;
  }
  .modal-title {
    font-size: 1.5rem;
    font-weight: 700;
    margin: 0;
    color: #333;
  }
  .modal-close {
    position: absolute;
    top: 1rem;
    right: 1.5rem;
    border: none;
    font-size: 2rem;
    padding: 0;
    cursor: pointer;
    font-weight: bold;
    color: #888;
    background: transparent;
    z-index: 1;
  }
  .modal-close:hover {
    color: #333;
  }
  .modal-body {
    flex-grow: 1;
    padding: 1.5rem 2rem;
    overflow-y: auto;
  }
  .modal-fade-enter-active,
  .modal-fade-leave-active {
    transition: opacity 0.3s ease;
  }
  .modal-fade-enter-from,
  .modal-fade-leave-to {
    opacity: 0;
  }
  .customer-detail-content {
    font-family: inherit;
    color: #333;
  }
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 1rem;
    border-bottom: 1px solid #eee;
    margin-bottom: 1.5rem;
  }
  .customer-name-phone {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
  }
  .name {
    font-size: 1.25rem;
    font-weight: 700;
  }
  .phone {
    font-size: 1rem;
    color: #666;
  }
  .actions {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  .icon-btn {
    background: none;
    border: none;
    cursor: pointer;
    padding: 0.25rem;
    line-height: 1;
    color: #666;
    transition: color 0.2s;
  }
  .icon-btn:hover {
    color: #333;
  }
  .delete-btn:hover {
    color: #e53935;
  }
  .detail-sections {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 2rem;
  }
  .info-section {
    display: flex;
    flex-direction: column;
  }
  .pass-sections {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 2rem;
    margin-top: 1rem;
    padding-top: 1.5rem;
    border-top: 1px solid #eee;
  }
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
    padding-bottom: 0.75rem;
    border-bottom: 1px solid #f0f0f0;
  }
  .section-title {
    font-size: 1.2rem;
    font-weight: 600;
  }
  .edit-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    background: none;
    border: none;
    color: #888;
    cursor: pointer;
    font-size: 0.9rem;
    font-weight: 500;
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
    transition: background-color 0.2s;
  }
  .edit-btn:hover {
    background-color: #f0f0f0;
    color: #333;
  }
  .info-group {
    display: flex;
    flex-direction: column;
  }
  .info-item {
    display: flex;
    align-items: baseline;
    gap: 1.5rem;
    margin-bottom: 0.75rem;
  }
  .label {
    font-weight: 600;
    color: #555;
    white-space: nowrap;
    flex-shrink: 0;
    min-width: 90px;
  }
  .value {
    color: #333;
  }
  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 0.25rem;
  }
  .pass-list {
    list-style: none;
    padding: 0;
    margin: 0;
  }
  .pass-item {
    display: flex;
    justify-content: space-between;
    padding: 0.5rem 0;
    border-bottom: 1px solid #f5f5f5;
    font-size: 0.9rem;
  }
  .pass-item:last-child {
    border-bottom: none;
  }
  .no-data {
    color: #999;
    padding: 1rem 0;
    font-size: 0.9rem;
  }
  .prepaid-balance {
    font-size: 1.5rem;
    font-weight: 700;
    color: #3fc1c9;
    padding-top: 1rem;
  }
</style>
