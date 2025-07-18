<template>
  <BaseModal v-model="isVisible" title="필터" @close="close">
    <div class="filter-form">
      <!-- 판매일시 -->
      <div class="form-section">
        <label class="section-title">판매일시</label>
        <div class="range-inputs">
          <PrimeDatePicker v-model="startDate" placeholder="시작일" append-to="body" />
          <PrimeDatePicker v-model="endDate" placeholder="종료일" append-to="body" />
        </div>
      </div>

      <!-- 매출유형 -->
      <div class="form-section">
        <label class="section-title">매출 유형</label>
        <BaseForm
          v-model="types"
          type="checkbox"
          :options="[
            { value: '상품', text: '상품' },
            { value: '회원권', text: '회원권' },
            { value: '환불', text: '환불' },
          ]"
        />
      </div>

      <!-- 담당자 -->
      <div class="form-section">
        <BaseForm
          v-model="selectedStaff"
          type="select"
          label="담당자"
          :options="staffOptions"
          placeholder="담당자 선택"
        />
      </div>
    </div>

    <!-- 푸터 -->
    <template #footer>
      <div class="footer-buttons">
        <BaseButton class="primary" outline @click="close">닫기</BaseButton>
        <BaseButton type="primary" @click="applyFilter">저장</BaseButton>
      </div>
    </template>
  </BaseModal>
</template>

<script setup>
  import { ref, watch, onMounted } from 'vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import { getStaff } from '@/features/staffs/api/staffs.js';

  const props = defineProps({
    modelValue: Boolean,
  });

  const emit = defineEmits(['close', 'apply', 'update:modelValue']);
  const selectedStaff = ref(null);
  const isVisible = ref(props.modelValue);
  const startDate = ref(null);
  const endDate = ref(null);
  const types = ref([]);
  const selectedStaffId = ref(null);
  const staffOptions = ref([]); // [{ value: 1, text: '홍길동' }, ...]

  watch(
    () => props.modelValue,
    val => {
      isVisible.value = val;
    }
  );
  watch(isVisible, val => {
    emit('update:modelValue', val);
  });

  const fetchStaffs = async () => {
    try {
      const response = await getStaff({ page: 1, size: 100, isActive: true });
      staffOptions.value = response.data.data.staffList.map(staff => ({
        value: staff.staffId,
        text: staff.staffName,
      }));
    } catch (e) {
      console.error('담당자 목록 불러오기 실패', e);
    }
  };

  onMounted(() => {
    fetchStaffs();
  });

  const close = () => {
    isVisible.value = false;
    emit('close');
  };

  const formatDateTimeKST = (date, endOfDay = false) => {
    if (!date) return null;

    const d = new Date(date);
    if (endOfDay) {
      d.setHours(23, 59, 59, 999);
    } else {
      d.setHours(0, 0, 0, 0);
    }

    // yyyy-MM-ddTHH:mm:ss 형식으로 반환
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    const hh = String(d.getHours()).padStart(2, '0');
    const mi = String(d.getMinutes()).padStart(2, '0');
    const ss = String(d.getSeconds()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}T${hh}:${mi}:${ss}`;
  };

  const applyFilter = () => {
    const formattedStart = formatDateTimeKST(startDate.value, false);
    const formattedEnd = formatDateTimeKST(endDate.value, true);

    const saleTypeMap = {
      상품: 'ITEMS',
      회원권: 'MEMBERSHIP',
      환불: 'REFUND',
    };

    const mappedTypes = types.value.map(t => saleTypeMap[t]);

    emit('apply', {
      startDate: formattedStart,
      endDate: formattedEnd,
      types: mappedTypes,
      staffId: selectedStaff.value,
      staffName: getStaffNameById(selectedStaff.value),
    });

    close();
  };

  const getStaffNameById = id => {
    return staffOptions.value.find(opt => opt.value === id)?.text || '';
  };
</script>

<style scoped>
  .filter-form {
    padding: 1rem 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.2rem;
  }

  .form-section {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .section-title {
    font-weight: bold;
    font-size: 15px;
    margin-bottom: 0.25rem;
  }

  .range-inputs {
    display: flex;
    gap: 1rem;
  }

  .footer-buttons {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding: 0 1.5rem 1rem;
  }

  .footer-buttons button {
    min-height: 36px;
    padding: 6px 16px;
    font-size: 14px;
  }
</style>

<style>
  .p-datepicker {
    z-index: 10010 !important;
  }
</style>
