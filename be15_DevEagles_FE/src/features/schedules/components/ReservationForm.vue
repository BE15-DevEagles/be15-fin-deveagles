<template>
  <div>
    <!-- 고객 검색 -->
    <div class="row row-inline" style="position: relative">
      <label class="label-wide">고객명 - 연락처</label>
      <BaseForm
        v-model="form.customer"
        type="text"
        placeholder="고객명 또는 연락처 검색"
        @focus="showSuggestions = true"
        @blur="handleBlur"
      />

      <ul v-if="showSuggestions && customerSuggestions.length > 0" class="autocomplete-list">
        <li
          v-for="(customer, index) in customerSuggestions"
          :key="index"
          class="autocomplete-item"
          @click="selectCustomer(customer)"
        >
          {{ customer.customerName }} - {{ customer.phoneNumber }}
        </li>
      </ul>
    </div>

    <!-- 예약 날짜 및 시간 -->
    <div class="row row-inline">
      <label class="label-wide">예약 날짜</label>
      <div class="flat-flex">
        <!-- 날짜 선택 -->
        <PrimeDatePicker
          v-model="form.date"
          :show-time="false"
          :show-button-bar="true"
          :clearable="false"
          hour-format="24"
          placeholder="날짜 선택"
          style="width: 160px"
        />

        <!-- 시작 시간 -->
        <PrimeDatePicker
          v-model="form.startTime"
          :show-time="true"
          :time-only="true"
          :clearable="true"
          hour-format="24"
          placeholder="시작 시간"
          style="width: 130px"
          @update:model-value="updateDuration"
        />

        <!-- 종료 시간 -->
        <PrimeDatePicker
          v-model="form.endTime"
          :show-time="true"
          :time-only="true"
          :clearable="true"
          hour-format="24"
          placeholder="종료 시간"
          style="width: 130px"
          @update:model-value="updateDuration"
        />

        <!-- 소요 시간 -->
        <p>소요시간 :</p>
        <input
          :value="form.duration"
          type="text"
          class="input input-time small-width"
          readonly
          placeholder="소요 시간"
        />
      </div>
    </div>

    <!-- 시술/상품 -->
    <div class="row">
      <label class="label-wide">시술/상품</label>
      <div class="selected-list">
        <div v-for="(item, index) in selectedServices" :key="index" class="selected-service">
          <div class="service-card">
            <div class="card-left">
              <span class="service-name">({{ item.category }}) {{ item.name }}</span>
              <span class="service-meta"
                >{{ item.duration }} / {{ item.price.toLocaleString() }} 원</span
              >
            </div>
            <button class="remove-btn" @click="removeService(index)">✕</button>
          </div>
        </div>
        <BaseButton class="add-button" @click="showItemModal = true">+ 상품 선택</BaseButton>
      </div>
    </div>

    <!-- 담당자 -->
    <div class="row row-inline">
      <label class="label-wide">담당자</label>
      <BaseForm
        v-model="form.staffId"
        type="select"
        :options="staffOptions"
        class="input"
        style="max-width: 400px"
      />
    </div>

    <!-- 특이사항 -->
    <div class="row row-inline">
      <label class="label-wide">특이사항</label>
      <BaseForm v-model="form.note" type="text" placeholder="특이사항 입력" />
    </div>

    <!-- 메모 -->
    <div class="row align-top">
      <label class="label-wide">메모</label>
      <BaseForm v-model="form.memo" type="textarea" rows="3" />
    </div>
  </div>

  <SelectSecondaryItemModal v-model="showItemModal" @select="addServiceFromModal" />
</template>

<script setup>
  import { ref, watch, onMounted } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import { searchCustomers, getStaffList } from '@/features/schedules/api/schedules.js';
  import BaseForm from '@/components/common/BaseForm.vue';
  const customerSuggestions = ref([]);
  const showSuggestions = ref(false);
  import SelectSecondaryItemModal from '@/features/schedules/components/SelectSecondaryItemModal.vue';
  const showItemModal = ref(false);
  import dayjs from 'dayjs';
  const staffOptions = ref([{ text: '담당자 선택', value: '' }]);

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
    updateEndTimeAndDuration();
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
  });

  const handleBlur = () => {
    setTimeout(() => {
      showSuggestions.value = false;
    }, 200);
  };
  const fetchStaffList = async () => {
    try {
      const res = await getStaffList({ isActive: true });
      staffOptions.value = [
        { text: '담당자 선택', value: '' },
        ...res.map(staff => ({
          text: staff.staffName,
          value: staff.staffId,
        })),
      ];
    } catch (e) {
      console.error('담당자 목록 조회 실패:', e);
    }
  };
  const selectCustomer = customer => {
    form.value.customer = `${customer.customerName} - ${customer.phoneNumber}`;
    form.value.customerId = customer.customerId;
    showSuggestions.value = false;
  };

  const updateDuration = () => {
    const start = form.value.startTime;
    const end = form.value.endTime;

    if (start instanceof Date && end instanceof Date && end > start) {
      const diff = end - start;
      const mins = Math.floor(diff / 60000);
      const hours = String(Math.floor(mins / 60)).padStart(2, '0');
      const minutes = String(mins % 60).padStart(2, '0');
      form.value.duration = `${hours}:${minutes}`;
    } else {
      form.value.duration = '';
    }
  };

  const selectedServices = ref([]);

  const removeService = index => {
    selectedServices.value.splice(index, 1);
  };
  onMounted(() => {
    fetchStaffList();
  });
  watch(
    () => form.value.customer,
    async keyword => {
      if (!keyword || keyword.trim() === '') {
        customerSuggestions.value = [];
        showSuggestions.value = false;
        return;
      }

      try {
        const result = await searchCustomers(keyword.trim());
        customerSuggestions.value = result;
        showSuggestions.value = result.length > 0;
      } catch (e) {
        console.error('❌ 고객 검색 실패', e);
        customerSuggestions.value = [];
        showSuggestions.value = false;
      }
    }
  );
  watch(
    () => [form.value.startTime, selectedServices.value.map(s => s.duration)],
    () => {
      updateEndTimeAndDuration();
    },
    { deep: true }
  );
  defineExpose({ form });
</script>

<style scoped>
  .row {
    display: flex;
    align-items: flex-start;
    margin-bottom: 14px;
    width: 100%;
  }

  .autocomplete-list {
    position: absolute;
    top: 38px;
    left: 120px;
    width: 400px;
    max-height: 250px;
    overflow-y: auto;
    background: white;
    border: 1px solid var(--color-gray-300);
    border-radius: 4px;
    z-index: 100;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  }

  .autocomplete-item {
    padding: 8px 12px;
    cursor: pointer;
    font-size: 14px;
  }

  .autocomplete-item:hover {
    background-color: var(--color-primary-50);
  }

  .row-inline {
    flex-wrap: nowrap;
    align-items: center;
  }

  .row.align-top {
    align-items: flex-start;
  }

  .row label {
    flex-shrink: 0;
    font-weight: bold;
    color: var(--color-gray-800);
    line-height: 1.5;
    white-space: nowrap;
  }

  .label-wide {
    width: 120px;
  }

  .row input,
  .row textarea,
  .row select {
    font-size: 14px;
    line-height: 1.5;
    padding: 6px 8px;
    width: 100%;
    max-width: 400px;
    border: 1px solid var(--color-gray-300);
    border-radius: 4px;
    box-sizing: border-box;
  }

  .input-date {
    width: 160px !important;
  }

  .input-time {
    width: 130px;
  }

  .small-width {
    width: 130px !important;
  }

  .big-width {
    width: 160px !important;
  }

  :deep(.p-datepicker-input) {
    padding-right: 38px !important;
  }

  .row textarea {
    resize: vertical;
  }

  .flat-flex {
    display: flex;
    flex: 1;
    gap: 8px;
    flex-wrap: nowrap;
    align-items: center;
  }

  .selected-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: 100%;
  }

  .selected-service {
    width: 100%;
  }

  .service-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: var(--color-neutral-white);
    border: 1px solid var(--color-gray-300);
    border-radius: 8px;
    padding: 10px 16px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    width: 100%;
    max-width: 600px;
  }

  .card-left {
    display: flex;
    flex-direction: column;
  }

  .form-group {
    display: flex;
    align-items: center;
    margin: 0;
    padding: 0;
    width: 200px !important;
  }

  .service-name {
    font-weight: 600;
    color: var(--color-neutral-dark);
    margin-bottom: 2px;
  }

  .service-meta {
    color: var(--color-gray-500);
    font-size: 14px;
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

  .add-button {
    align-self: flex-start;
    margin-top: 4px;
  }
</style>
