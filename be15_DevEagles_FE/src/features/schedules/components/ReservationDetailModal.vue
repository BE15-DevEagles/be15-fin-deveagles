<template>
  <div>
    <div v-if="modelValue && isLoaded">
      <div class="overlay" @click.self="close">
        <div class="modal-panel">
          <div class="modal-header">
            <div>
              <h1>등록된 스케줄</h1>
              <p class="type-label">예약</p>
            </div>
            <button class="close-btn" @click="close">×</button>
          </div>

          <div class="modal-body">
            <div class="left-detail">
              <!-- 고객명 -->
              <div class="row">
                <label>고객명</label>
                <span v-if="!isEditMode">{{ reservation.customerName || '미등록 고객' }}</span>
                <BaseForm
                  v-else
                  v-model="edited.customerName"
                  type="text"
                  readonly
                  :value="edited.customerName || '미등록 고객'"
                />
              </div>
              <!-- 연락처 -->
              <div class="row">
                <label>연락처</label>
                <span v-if="!isEditMode">{{ reservation.customerPhone || '미등록 고객' }}</span>
                <BaseForm
                  v-else
                  v-model="edited.customerPhone"
                  type="text"
                  readonly
                  :value="edited.customerPhone || '미등록 고객'"
                />
              </div>

              <!-- 예약일 -->
              <div class="row">
                <label>예약일</label>

                <span v-if="!isEditMode">
                  {{ formattedDate }} {{ formattedStartTime }} - {{ formattedEndTime }} ({{
                    reservation.duration
                  }})
                </span>

                <!-- 수정 모드 -->
                <div v-else class="date-time-edit">
                  <div class="date-row-inline">
                    <PrimeDatePicker
                      v-model="edited.date"
                      :show-time="false"
                      :show-button-bar="true"
                      :clearable="false"
                      style="width: 200px"
                      hour-format="24"
                    />
                    <PrimeDatePicker
                      v-model="edited.startTime"
                      :show-time="true"
                      :time-only="true"
                      :show-button-bar="true"
                      :clearable="false"
                      hour-format="24"
                      placeholder="시간을 선택하세요"
                      style="width: 160px"
                      @update:model-value="updateEditedEndTime"
                    />
                    <PrimeDatePicker
                      v-model="edited.endTime"
                      :show-time="true"
                      :time-only="true"
                      :show-button-bar="true"
                      :clearable="false"
                      hour-format="24"
                      placeholder="시간을 선택하세요"
                      style="width: 160px"
                      @update:model-value="updateDuration"
                    />
                  </div>
                </div>
              </div>

              <div v-if="isEditMode" class="row">
                <label>소요 시간</label>
                <BaseForm v-model="edited.duration" type="text" readonly />
              </div>

              <!-- 시술/상품 -->
              <div v-if="!isEditMode" class="row">
                <label>시술/상품</label>
                <span v-if="!isEditMode">
                  {{ reservation.itemNames ? reservation.itemNames.replace(/,/g, ', ') : '' }}</span
                >
                <BaseForm v-else v-model="edited.itemNames" type="text" />
              </div>
              <!-- 시술/상품 -->
              <div v-if="isEditMode" class="row">
                <label class="label-wide">시술/상품</label>
                <div class="selected-list">
                  <div
                    v-for="(item, index) in selectedServices"
                    :key="index"
                    class="selected-service"
                  >
                    <div class="service-card">
                      <div class="card-left">
                        <span class="service-name">({{ item.category }}) {{ item.name }}</span>
                        <span class="service-meta">
                          {{ item.duration }} / {{ item.price.toLocaleString() }} 원
                        </span>
                      </div>
                      <button v-if="isEditMode" class="remove-btn" @click="removeService(index)">
                        ✕
                      </button>
                    </div>
                  </div>
                  <BaseButton v-if="isEditMode" class="add-button" @click="showItemModal = true">
                    + 상품 선택
                  </BaseButton>
                </div>
              </div>

              <SelectSecondaryItemModal
                v-model="showItemModal"
                :selected-ids="selectedServices.map(s => s.selectedItems)"
                @select="addServiceFromModal"
              />

              <!-- 담당자 -->
              <div class="row row-select">
                <label>담당자</label>
                <div class="form-control-wrapper">
                  <span v-if="!isEditMode">{{ reservation.staffName }}</span>
                  <BaseForm
                    v-else
                    v-model="edited.staffId"
                    type="select"
                    :options="staffOptions"
                    placeholder="담당자 선택"
                    style="max-width: 400px"
                  />
                </div>
              </div>

              <!-- 예약 상태 -->
              <div class="row row-select">
                <label>예약 상태</label>
                <div class="form-control-wrapper">
                  <span v-if="!isEditMode">{{ reservationStatusLabel }}</span>
                  <BaseForm
                    v-else
                    v-model="edited.reservationStatusName"
                    type="select"
                    :options="editableStatusOptions"
                    placeholder="예약 상태 선택"
                  />
                </div>
              </div>

              <!-- 특이사항 -->
              <div class="row">
                <label>특이사항</label>
                <span v-if="!isEditMode">{{ reservation.staffMemo }}</span>
                <BaseForm v-else v-model="edited.staffMemo" type="textarea" rows="3" />
              </div>

              <!-- 고객 메모 -->
              <div class="row">
                <label>고객 메모</label>
                <span v-if="!isEditMode">{{ reservation.reservationMemo }}</span>
                <BaseForm v-else v-model="edited.reservationMemo" type="textarea" rows="3" />
              </div>
            </div>

            <!-- 우측 영역 -->
            <div v-if="!readonly && !isEditMode" class="right-box">
              <div class="right-box-inner">
                <div
                  v-if="reservation.customerName && reservation.customerName !== '미등록 고객'"
                  class="sales-list-item"
                  @click="handleCustomerInfo"
                >
                  <span class="label">고객 정보 조회</span>
                  <span class="arrow">›</span>
                </div>
                <div
                  v-if="reservation.reservationStatusName !== 'PAID'"
                  class="sales-list-item"
                  :class="{ disabled: reservation.salesRegistered }"
                  @click="!reservation.salesRegistered && (showSalesModal = true)"
                >
                  <span class="label">매출 등록</span>
                  <span class="arrow">›</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 푸터 -->
          <div v-if="!readonly" class="modal-footer">
            <BaseButton type="error" @click="close">닫기</BaseButton>
            <template v-if="isEditMode">
              <BaseButton type="primary" @click="handleSave">저장</BaseButton>
            </template>
            <template v-else>
              <div class="action-dropdown">
                <BaseButton
                  type="primary"
                  :disabled="reservation.reservationStatusName === 'PAID'"
                  @click="toggleMenu"
                >
                  수정 / 삭제
                </BaseButton>
                <ul
                  v-if="showMenu && reservation.reservationStatusName !== 'PAID'"
                  class="dropdown-menu"
                >
                  <li @click="handleEdit">수정하기</li>
                  <li @click="openDeleteConfirm">삭제하기</li>
                </ul>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <BaseToast ref="toast" />

    <BaseConfirm
      v-model="showConfirmModal"
      title="예약 삭제"
      message="정말 이 예약을 삭제하시겠습니까?"
      confirm-text="삭제"
      cancel-text="취소"
      confirm-type="error"
      icon-type="error"
      @confirm="handleDelete"
    />
    <BaseConfirm
      v-model="showEditConfirm"
      title="변경 내용을 저장하시겠습니까?"
      message="입력한 정보로 예약을 수정하시겠습니까?"
      confirm-text="수정"
      cancel-text="취소"
      confirm-type="primary"
      icon-type="info"
      @confirm="confirmEdit"
    />

    <SalesItemsModal
      v-if="showSalesModal"
      :reservation-id="reservation.reservationId"
      :initial-customer-id="reservation.customerId"
      :services="selectedServices.map(s => s.selectedItems)"
      @close="showSalesModal = false"
      @submit="handleSalesSubmit"
    />

    <CustomerDetailModal
      v-model="showCustomerModal"
      :customer="customerData"
      @request-reservation="handleReservationRequest"
      @request-sales="handleSalesRequest"
    />

    <ScheduleRegistModal
      v-if="showReservationRegistModal"
      v-model="showReservationRegistModal"
      :initial-customer="customerData"
      @submit="() => toast?.success('예약이 등록되었습니다.')"
      @error="err => toast?.error(err?.message || '예약 등록에 실패했습니다.')"
    />
  </div>
</template>

<script setup>
  import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import SalesItemsModal from '@/features/sales/components/ItemsSalesRegistModal.vue';
  import {
    fetchReservationDetail,
    deleteReservation,
    getStaffList,
    updateReservation,
    getActiveSecondaryItems,
    getAllPrimaryItems,
    getCustomerDetail,
  } from '@/features/schedules/api/schedules.js';
  import BaseToast from '@/components/common/BaseToast.vue';
  import BaseConfirm from '@/components/common/BaseConfirm.vue';
  import SelectSecondaryItemModal from '@/features/schedules/components/SelectSecondaryItemModal.vue';
  import dayjs from 'dayjs';
  import CustomerDetailModal from '@/features/customer/components/CustomerDetailModal.vue';
  import ScheduleRegistModal from '@/features/schedules/components/ScheduleRegistModal.vue';

  const emit = defineEmits(['update:modelValue', 'cancelReservation', 'closed']);
  const showCustomerModal = ref(false);
  const customerData = ref(null);
  const showReservationRegistModal = ref(false);
  const showEditConfirm = ref(false);
  const showSalesModal = ref(false);
  const handleSalesSubmit = () => {
    showSalesModal.value = false;
    reservation.value.salesRegistered = true;
    toast.value?.success('매출이 등록되었습니다.');
  };
  const handleCustomerInfo = async () => {
    if (!reservation.value || !reservation.value.customerId) {
      toast.value?.error('고객 정보가 없습니다.');
      return;
    }

    try {
      const detail = await getCustomerDetail(reservation.value.customerId);
      customerData.value = detail;
      showCustomerModal.value = true;
    } catch (e) {
      console.error('❌ 고객 상세 조회 실패:', e);
      toast.value?.error('고객 정보를 불러오지 못했습니다.');
    }
  };

  const handleReservationRequest = _customer => {
    showCustomerModal.value = false;
    showReservationRegistModal.value = true;
  };

  const handleSalesRequest = _customer => {
    showCustomerModal.value = false;
    selectedServices.value =
      reservation.value.secondaryItems?.map(item => ({
        selectedItems: [item.secondaryItemId],
      })) || [];
    showSalesModal.value = true;
  };
  const handleSave = () => {
    showEditConfirm.value = true;
  };
  const confirmEdit = async () => {
    await saveEdit();
  };
  const showItemModal = ref(false);
  const selectedServices = ref([]);
  const toast = ref(null);

  const props = defineProps({
    modelValue: Boolean,
    id: {
      type: Number,
      default: null,
    },
    readonly: {
      type: Boolean,
      default: false,
    },
  });

  const openDeleteConfirm = () => {
    showMenu.value = false;
    showConfirmModal.value = true;
  };

  const reservation = ref({});
  const edited = ref({
    customerName: '',
    customerPhone: '',
    itemNames: '',
    staffName: '',
    reservationStatusName: '',
    staffMemo: '',
    reservationMemo: '',
    date: '',
    startTime: null,
    endTime: null,
    duration: '',
  });
  const isEditMode = ref(false);
  const showMenu = ref(false);
  const isLoaded = ref(false);

  const statusOptions = [
    { text: '예약 확정', value: 'CONFIRMED' },
    { text: '예약 대기', value: 'PENDING' },
    { text: '노쇼', value: 'NO_SHOW' },
    { text: '고객에 의한 취소', value: 'CBC' },
    { text: '가게에 의한 취소', value: 'CBS' },
    { text: '결제 완료', value: 'PAID' },
  ];

  const reservationStatusMap = {
    CONFIRMED: '예약 확정',
    PENDING: '예약 대기',
    NO_SHOW: '노쇼',
    CBC: '고객 취소',
    CBS: '매장 취소',
    PAID: '결제 완료',
  };
  const form = ref({
    customer: '',
    customerId: null,
    selectedCustomer: null,
    staffId: '',
    date: null,
    startTime: new Date(0, 0, 0, 0, 0),
    endTime: new Date(0, 0, 0, 0, 0),
    duration: '',
    note: '',
    memo: '',
    selectedItems: [],
  });
  const reservationStatusLabel = computed(() => {
    const code = reservation.value?.reservationStatusName;
    return reservationStatusMap[code] ?? code ?? '';
  });

  const staffOptions = ref([]);
  const fetchStaffList = async () => {
    try {
      const res = await getStaffList({ isActive: true });
      staffOptions.value = [
        ...res.map(staff => ({
          text: staff.staffName,
          value: staff.staffId,
        })),
      ];
    } catch (e) {
      console.error('담당자 목록 조회 실패:', e);
    }
  };
  onMounted(() => {
    fetchStaffList();
  });
  watch(
    [() => props.modelValue, () => props.id],
    async ([modelValue, id]) => {
      if (modelValue && id) {
        try {
          const res = await fetchReservationDetail(id);

          const start = new Date(res.reservationStartAt);
          const end = new Date(res.reservationEndAt);

          reservation.value = {
            ...res,
            duration: calculateDuration(start, end),
          };
          edited.value = {
            customerName: res.customerName ?? '',
            customerPhone: res.customerPhone ?? '',
            itemNames: res.itemNames ?? '',
            staffName: res.staffName ?? '',
            reservationStatusName: res.reservationStatusName ?? '',
            staffMemo: res.staffMemo ?? '',
            reservationMemo: res.reservationMemo ?? '',
            date: start,
            startTime: start,
            endTime: end,
            duration: calculateDuration(start, end),
          };
          const secondaryMaster = await getActiveSecondaryItems();
          const primaryMaster = await getAllPrimaryItems();
          const idList = (res.secondaryItemIds || '').split(',').map(id => Number(id));

          selectedServices.value = idList.map(id => {
            const found = secondaryMaster.find(s => s.secondaryItemId === id);
            const primary = found
              ? primaryMaster.find(p => p.primaryItemId === found.primaryItemId)
              : null;
            return {
              category: primary?.primaryItemName || '',
              name: found?.secondaryItemName || '',
              selectedItems: id,
              duration: found?.timeTaken != null ? `${found.timeTaken}분` : '상품',
              price: found?.secondaryItemPrice || 0,
            };
          });

          form.value.selectedItems = idList.map(id => ({ id }));
          isLoaded.value = true;
        } catch (e) {
          console.error('❌ 상세 조회 실패:', e);
          isLoaded.value = false;
        }
      } else {
        isLoaded.value = false;
      }
    },
    { immediate: true }
  );

  const formattedDate = computed(() => {
    return reservation.value.reservationStartAt?.split('T')[0] ?? '';
  });
  const formattedStartTime = computed(() => {
    return reservation.value.reservationStartAt?.split('T')[1]?.slice(0, 5);
  });
  const formattedEndTime = computed(() => {
    return reservation.value.reservationEndAt?.split('T')[1]?.slice(0, 5);
  });

  const calculateDuration = (start, end) => {
    const diffMs = end - start;
    if (diffMs > 0) {
      const totalMinutes = Math.floor(diffMs / 60000);
      const hours = Math.floor(totalMinutes / 60);
      const minutes = totalMinutes % 60;

      if (hours > 0 && minutes > 0) {
        return `${hours}시간 ${minutes}분 소요`;
      } else if (hours > 0) {
        return `${hours}시간 소요`;
      } else if (minutes > 0) {
        return `${minutes}분 소요`;
      }
    }
    return '';
  };

  onMounted(() => {
    window.addEventListener('keydown', handleKeyDown);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeyDown);
  });

  const handleKeyDown = e => {
    if (e.key === 'Escape' || e.key === 'Esc') {
      close();
    }
  };

  const close = () => {
    emit('update:modelValue', false);
    emit('closed');
    isEditMode.value = false;
    showMenu.value = false;
  };
  const removeService = index => {
    selectedServices.value.splice(index, 1);
    form.value.selectedItems.splice(index, 1);

    if (isEditMode.value) {
      updateEditedEndTime();
    } else {
      updateEndTimeAndDuration();
    }
  };
  const updateEndTimeAndDuration = () => {
    const start = form.value.startTime;
    if (!(start instanceof Date) || isNaN(start)) return;

    const totalMinutes = selectedServices.value.reduce((sum, item) => {
      const parsed = parseInt(item.duration);
      return sum + (isNaN(parsed) ? 0 : parsed);
    }, 0);

    const hours = String(Math.floor(totalMinutes / 60)).padStart(2, '0');
    const minutes = String(totalMinutes % 60).padStart(2, '0');
    form.value.duration = `${hours}:${minutes}`;

    const end = dayjs(start).add(totalMinutes, 'minute');
    form.value.endTime = end.toDate();
  };

  const addServiceFromModal = item => {
    selectedServices.value.push({
      category: item.primaryItemName,
      name: item.secondaryItemName,
      selectedItems: item.secondaryItemId,
      duration: item.timeTaken != null ? `${item.timeTaken}분` : '상품',
      price: item.secondaryItemPrice,
    });

    form.value.selectedItems.push({ id: item.secondaryItemId });

    if (isEditMode.value) {
      updateEditedEndTime();
    } else {
      updateEndTimeAndDuration();
    }
  };

  const updateEditedEndTime = () => {
    const start = edited.value.startTime;
    if (!(start instanceof Date) || isNaN(start)) return;

    const totalMinutes = selectedServices.value.reduce((sum, item) => {
      const parsed = parseInt(item.duration);
      return sum + (isNaN(parsed) ? 0 : parsed);
    }, 0);

    const end = dayjs(start).add(totalMinutes, 'minute').toDate();
    edited.value.endTime = end;
    edited.value.duration = calculateDuration(start, end);
  };

  const toggleMenu = () => (showMenu.value = !showMenu.value);
  const handleEdit = () => {
    const res = reservation.value;
    const start = new Date(res.reservationStartAt);
    const end = new Date(res.reservationEndAt);

    const matched = staffOptions.value.find(opt => opt.text === res.staffName);
    const staffId = matched?.value ?? '';

    edited.value = {
      customerName: res.customerName ?? '',
      customerPhone: res.customerPhone ?? '',
      itemNames: res.itemNames ?? '',
      staffId,
      reservationStatusName: res.reservationStatusName ?? '',
      staffMemo: res.staffMemo ?? '',
      reservationMemo: res.reservationMemo ?? '',
      date: start.toISOString().split('T')[0],
      startTime: start,
      endTime: end,
      duration: calculateDuration(start, end),
    };

    isEditMode.value = true;
  };

  const showConfirmModal = ref(false);
  const handleDelete = async () => {
    if (!props.id) return;

    try {
      await deleteReservation(props.id);
      toast.value?.success('삭제가 완료되었습니다.');
      close();
    } catch (e) {
      console.error('❌ 삭제 실패:', e);
      toast.value?.error('삭제 중 오류가 발생했습니다.');
    }
  };
  const combineDateTime = (date, time) => {
    const dateStr = dayjs(date).format('YYYY-MM-DD');
    const timeStr = dayjs(time).format('HH:mm:ss');
    return dayjs(`${dateStr}T${timeStr}`).format('YYYY-MM-DDTHH:mm:ss');
  };

  const saveEdit = async () => {
    try {
      const payload = {
        staffId: edited.value.staffId,
        reservationStatusName: edited.value.reservationStatusName,
        staffMemo: edited.value.staffMemo,
        reservationMemo: edited.value.reservationMemo,
        reservationStartAt: combineDateTime(edited.value.date, edited.value.startTime),
        reservationEndAt: combineDateTime(edited.value.date, edited.value.endTime),
        secondaryItemIds: selectedServices.value.map(item => item.selectedItems),
      };

      await updateReservation(props.id, payload);
      toast.value?.success('예약이 수정되었습니다.');
      isEditMode.value = false;
      close();
    } catch (e) {
      console.error('예약 수정 실패:', e);
      toast.value?.error('예약 수정 중 오류가 발생했습니다.');
    }
  };
  const editableStatusOptions = computed(() => {
    return statusOptions.filter(option => option.value !== 'PAID');
  });
  const updateDuration = () => {
    if (edited.value.startTime instanceof Date && edited.value.endTime instanceof Date) {
      edited.value.duration = calculateDuration(edited.value.startTime, edited.value.endTime);
    }
  };
  watch(
    [() => reservation.value.staffName, staffOptions],
    ([name, options]) => {
      if (!name || !options.length) return;

      const matched = options.find(opt => opt.text === name);
      if (matched) {
        edited.value.staffId = matched.value;
      }
    },
    { immediate: true }
  );
</script>

<style scoped>
  .overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100vh;
    background: rgba(0, 0, 0, 0.3);
    z-index: 1000;
  }

  .selected-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: 100%;
  }

  .modal-panel {
    position: fixed;
    top: 0;
    left: 240px;
    width: calc(100% - 240px);
    height: 100vh;
    background: var(--color-neutral-white);
    display: flex;
    flex-direction: column;
    padding: 24px;
    overflow: hidden;
  }

  .right-box {
    flex: 0 0 200px;
    border-left: 1px solid var(--color-gray-200);
    height: auto;
    align-self: stretch;
  }

  .date-time-edit {
    display: flex;
    gap: 8px;
    align-items: center;
    flex-wrap: nowrap;
  }

  .date-row-inline {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: nowrap;
    flex: 1;
    margin-left: 0px;
  }

  .duration-inline {
    display: flex;
    align-items: center;
    margin-top: 8px;
    gap: 6px;
  }

  .duration-label {
    font-size: 13px;
    white-space: nowrap;
  }

  .duration-input {
    width: 60px;
    text-align: left;
    padding: 6px 8px;
    border: 1px solid var(--color-gray-300);
    border-radius: 4px;
    margin-left: 4px;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .modal-header h1 {
    font-size: 20px;
    font-weight: bold;
  }

  .close-btn {
    background: none;
    border: none;
    font-size: 24px;
    cursor: pointer;
  }

  .modal-body {
    display: flex;
    gap: 32px;
    flex: 1 1 auto;
    height: 100%;
    padding: 0;
    overflow-x: hidden;
  }

  .left-detail {
    flex: 1;
    padding: 24px;
    margin-top: 24px;
    overflow-y: auto;
  }

  .row {
    display: flex;
    align-items: flex-start;
    margin-bottom: 14px;
  }

  .row label {
    width: 100px;
    font-weight: bold;
    color: var(--color-gray-800);
    padding-top: 6px;
    line-height: 1.5;
  }

  .row span,
  .row input,
  .row textarea {
    font-size: 14px;
    line-height: 1.5;
    padding: 6px 8px;
    vertical-align: middle;
    width: 100%;
    max-width: 400px;
    box-sizing: border-box;
  }

  .form-group {
    display: flex;
    align-items: center;
    margin: 0;
    padding: 0;
    width: 200px !important;
  }

  .row input,
  .row textarea {
    border: 1px solid var(--color-gray-300);
    border-radius: 4px;
  }

  .row textarea {
    resize: vertical;
  }

  .form-control-wrapper :deep(.input) {
    width: 100%;
    max-width: 300px;
  }

  .right-box p {
    margin-bottom: 16px;
    font-weight: 500;
    color: var(--color-gray-700);
  }

  .modal-footer {
    display: flex;
    gap: 12px;
    justify-content: flex-end;
  }

  .action-dropdown {
    position: relative;
  }

  .dropdown-menu {
    position: absolute;
    bottom: 40px;
    right: 0;
    background: var(--color-neutral-white);
    border: 1px solid var(--color-gray-300);
    border-radius: 6px;
    list-style: none;
    padding: 8px 0;
    width: 120px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.15);
    z-index: 10;
  }

  .dropdown-menu li {
    padding: 8px 12px;
    cursor: pointer;
    color: var(--color-gray-800);
  }

  .dropdown-menu li:hover {
    background: var(--color-gray-100);
  }

  .type-label {
    margin-top: 4px;
    font-size: 18px;
    font-weight: 500;
    color: var(--color-gray-500);
  }

  .card-left {
    display: flex;
    flex-direction: column;
  }

  :deep(.service-name) {
    font-size: 16px !important;
    font-weight: 600 !important;
    color: var(--color-neutral-dark);
    margin-bottom: 0;
  }

  .service-meta {
    color: var(--color-gray-500);
    font-size: 14px !important;
    white-space: nowrap;
  }

  .remove-btn {
    background: none;
    border: none;
    color: var(--color-error-300);
    font-size: 18px;
    font-weight: bold;
    cursor: pointer;
    margin-left: 16px;
  }

  .form-group {
    display: flex;
    align-items: center;
    margin: 0;
    padding: 0;
    width: 200px !important;
  }

  .add-button {
    align-self: flex-start;
    margin-top: 4px;
    margin-left: 8px;
  }

  .selected-service {
    width: 100%;
    margin-left: 8px;
  }

  :deep(.service-card) {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: var(--color-neutral-white);
    border: 1px solid var(--color-gray-300);
    border-radius: 8px;
    padding: 10px 16px !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    width: 100%;
    max-width: 600px;
  }

  .sales-list-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid var(--color-gray-200);
    cursor: pointer;
    font-size: 16px;
    color: var(--color-neutral-dark);
  }

  .sales-list-item:hover {
    background-color: var(--color-gray-50);
  }

  .sales-list-item .arrow {
    font-size: 18px;
    color: var(--color-gray-500);
  }

  .sales-list-item.disabled {
    color: var(--color-gray-400);
    cursor: not-allowed;
  }

  .sales-list-item.disabled .arrow {
    color: var(--color-gray-300);
  }
</style>
