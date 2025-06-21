<template>
  <div>
    <!-- 고객 검색 -->
    <div class="row row-inline">
      <label class="label-wide">고객명 - 연락처</label>
      <input
        v-model="form.customer"
        type="text"
        class="input"
        placeholder="고객명 또는 연락처 검색"
      />
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
          :clearable="false"
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
          :clearable="false"
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
        <BaseButton class="add-button" @click="selectService">+ 상품 선택</BaseButton>
      </div>
    </div>

    <!-- 담당자 -->
    <div class="row row-inline">
      <label class="label-wide">담당자</label>
      <select v-model="form.staff" class="input">
        <option value="">담당자 미정</option>
        <option value="김이글">김이글</option>
        <option value="박이글">박이글</option>
      </select>
    </div>

    <!-- 특이사항 -->
    <div class="row row-inline">
      <label class="label-wide">특이사항</label>
      <input v-model="form.note" type="text" class="input" />
    </div>

    <!-- 메모 -->
    <div class="row align-top">
      <label class="label-wide">메모</label>
      <textarea v-model="form.memo" rows="3" class="input" />
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';

  const form = ref({
    date: null,
    startTime: null,
    endTime: null,
    duration: '',
  });

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

  const selectedServices = ref([
    {
      category: '염색',
      name: '전체염색',
      duration: '1시간 30분',
      price: 50000,
    },
  ]);

  const selectService = () => {
    selectedServices.value.push({
      category: '커트',
      name: '베이직컷',
      duration: '30분',
      price: 20000,
    });
  };

  const removeService = index => {
    selectedServices.value.splice(index, 1);
  };
</script>

<style scoped>
  .row {
    display: flex;
    align-items: flex-start;
    margin-bottom: 14px;
    width: 100%;
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
    width: 110px;
  }

  .small-width {
    width: 100px !important;
  }

  .big-width {
    width: 160px !important;
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
