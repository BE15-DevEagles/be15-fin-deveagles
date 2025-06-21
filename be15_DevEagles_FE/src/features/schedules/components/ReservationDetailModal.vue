<template>
  <div v-if="modelValue" class="overlay" @click.self="close">
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
            <span v-if="!isEditMode">{{ reservation.customer }}</span>
            <BaseForm v-else v-model="edited.customer" type="text" />
          </div>

          <!-- 연락처 -->
          <div class="row">
            <label>연락처</label>
            <span v-if="!isEditMode">{{ reservation.phone }}</span>
            <BaseForm v-else v-model="edited.phone" type="text" />
          </div>

          <!-- 예약일 -->
          <div class="row">
            <label>예약일</label>
            <span v-if="!isEditMode">
              {{ reservation.date }} {{ reservation.startTime }} - {{ reservation.endTime }} ({{
                reservation.duration
              }})
            </span>
            <div v-else class="date-time-edit">
              <div class="row">
                <div class="date-row-inline">
                  <PrimeDatePicker
                    v-model="edited.date"
                    :show-time="false"
                    :show-button-bar="true"
                    :clearable="false"
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
                    @update:model-value="updateDuration"
                  />
                  <PrimeDatePicker
                    v-model="edited.endTime"
                    :show-time="true"
                    :time-only="true"
                    :show-button-bar="true"
                    :clearable="false"
                    hour-format="24"
                    placeholder="시간을 선택하세요"
                    @update:model-value="updateDuration"
                  />
                  <div class="duration-inline">
                    <p>소요 시간:</p>
                    <input class="duration-input" :value="edited.duration" readonly />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 시술 -->
          <div class="row">
            <label>시술</label>
            <span v-if="!isEditMode">{{ reservation.service }}</span>
            <BaseForm v-else v-model="edited.service" type="text" />
          </div>

          <!-- 담당자 -->
          <div class="row row-select">
            <label>담당자</label>
            <div class="form-control-wrapper">
              <BaseForm
                v-if="isEditMode"
                v-model="edited.staff"
                type="select"
                :options="[
                  { text: '디자이너 A', value: '디자이너 A' },
                  { text: '디자이너 B', value: '디자이너 B' },
                ]"
                placeholder="담당자 선택"
              />
              <span v-else>{{ reservation.staff }}</span>
            </div>
          </div>

          <!-- 예약 상태 -->
          <div class="row row-select">
            <label>예약 상태</label>
            <div class="form-control-wrapper">
              <BaseForm
                v-if="isEditMode"
                v-model="edited.status"
                type="select"
                :options="statusOptions"
                placeholder="예약 상태 선택"
              />
              <span v-else>{{ reservation.status }}</span>
            </div>
          </div>

          <!-- 특이사항 -->
          <div class="row">
            <label>특이사항</label>
            <span v-if="!isEditMode">{{ reservation.note }}</span>
            <BaseForm v-else v-model="edited.note" type="textarea" rows="3" />
          </div>

          <!-- 고객 메모 -->
          <div class="row">
            <label>고객 메모</label>
            <span v-if="!isEditMode">{{ reservation.memo }}</span>
            <BaseForm v-else v-model="edited.memo" type="textarea" rows="3" />
          </div>
        </div>

        <!-- 우측 영역 -->
        <div class="right-box">
          <p>고객정보 확인</p>
          <p>매출 등록</p>
        </div>
      </div>

      <!-- 푸터 -->
      <div class="modal-footer">
        <BaseButton type="error" @click="close">닫기</BaseButton>
        <template v-if="isEditMode">
          <BaseButton type="primary" @click="saveEdit">저장</BaseButton>
        </template>
        <template v-else>
          <div class="action-dropdown">
            <BaseButton type="primary" @click="toggleMenu">수정 / 삭제</BaseButton>
            <ul v-if="showMenu" class="dropdown-menu">
              <li @click="handleEdit">수정하기</li>
              <li @click="handleDelete">삭제하기</li>
            </ul>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, defineProps, defineEmits, watch, onMounted, onBeforeUnmount } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';

  const props = defineProps({
    modelValue: Boolean,
    reservation: Object,
  });
  const emit = defineEmits(['update:modelValue']);

  const showMenu = ref(false);
  const isEditMode = ref(false);
  const edited = ref({});

  const statusOptions = [
    { text: '노쇼', value: '노쇼' },
    { text: '가게에 의한 취소', value: '가게에 의한 취소' },
    { text: '고객에 의한 취소', value: '고객에 의한 취소' },
    { text: '예약 대기', value: '예약 대기' },
    { text: '예약 확정', value: '예약 확정' },
  ];

  watch(
    () => props.modelValue,
    val => {
      if (val) {
        isEditMode.value = false;
        showMenu.value = false;
        edited.value = { ...props.reservation };
      }
    }
  );

  const close = () => {
    emit('update:modelValue', false);
    isEditMode.value = false;
    showMenu.value = false;
    edited.value = {};
  };

  const handleEsc = e => e.key === 'Escape' && close();
  onMounted(() => window.addEventListener('keydown', handleEsc));
  onBeforeUnmount(() => window.removeEventListener('keydown', handleEsc));

  const toggleMenu = () => (showMenu.value = !showMenu.value);
  const handleEdit = () => {
    isEditMode.value = true;
    showMenu.value = false;
    edited.value = { ...props.reservation };
  };
  const handleDelete = () => {
    showMenu.value = false;
    if (confirm('정말 삭제하시겠습니까?')) alert('삭제 요청 전송');
  };
  const saveEdit = () => {
    alert('수정 내용 저장 요청:\n' + JSON.stringify(edited.value, null, 2));
    isEditMode.value = false;
  };

  const updateDuration = () => {
    const start = edited.value.startTime;
    const end = edited.value.endTime;
    if (start instanceof Date && end instanceof Date) {
      const diffMs = end - start;
      if (diffMs > 0) {
        const totalMinutes = Math.floor(diffMs / 60000);
        const hours = String(Math.floor(totalMinutes / 60)).padStart(2, '0');
        const minutes = String(totalMinutes % 60).padStart(2, '0');
        edited.value.duration = `${hours}:${minutes}`;
      } else {
        edited.value.duration = '';
      }
    }
  };
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
    overflow-y: auto;
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
  }
  .duration-inline {
    display: flex;
    align-items: center;
    gap: 4px;
    white-space: nowrap;
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
    margin-bottom: 24px;
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
    flex: 1;
  }
  .left-detail {
    flex: 1;
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
  .row input,
  .row textarea {
    border: 1px solid var(--color-gray-300);
    border-radius: 4px;
  }
  .row textarea {
    resize: vertical;
  }
  .form-control-wrapper {
    flex: 1;
    display: flex;
    align-items: flex-start;
  }
  .form-control-wrapper :deep(.input) {
    width: 100%;
    max-width: 300px;
  }
  .right-box {
    width: 200px;
    padding: 12px;
    border-left: 1px solid var(--color-gray-200);
  }
  .right-box p {
    margin-bottom: 16px;
    font-weight: 500;
    color: var(--color-gray-700);
  }
  .modal-footer {
    margin-top: 32px;
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
</style>
