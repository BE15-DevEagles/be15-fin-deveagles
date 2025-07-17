<template>
  <Teleport to="body">
    <div v-if="modelValue" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">필터</h3>
          <button class="modal-close" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="filter-header-actions">
            <BaseButton type="secondary" size="xs" outline @click="resetFilters">
              <RefreshCwIcon :size="14" style="margin-right: 4px" />
              초기화
            </BaseButton>
          </div>

          <!-- 필터 폼 -->
          <div class="filter-form">
            <!-- 검색 조건 -->
            <div class="filter-section">
              <div class="radio-group horizontal">
                <label class="radio-item">
                  <input v-model="filterForm.searchType" type="radio" value="and" />
                  <span>교집합 검색</span>
                </label>
                <label class="radio-item">
                  <input v-model="filterForm.searchType" type="radio" value="or" />
                  <span>합집합 검색</span>
                </label>
              </div>
              <p class="search-type-desc">
                <AlertTriangleIcon :size="14" />
                {{
                  filterForm.searchType === 'and'
                    ? '입력한 값들에 모두 만족해야 검색됩니다.'
                    : '입력한 값들 중 하나라도 만족하면 검색됩니다.'
                }}
              </p>
            </div>

            <!-- 생일 -->
            <div class="filter-section">
              <h4 class="section-title">생일</h4>
              <div class="checkbox-group horizontal">
                <label class="checkbox-item"
                  ><input v-model="filterForm.birthday" type="checkbox" value="this_month" /><span
                    >이번달</span
                  ></label
                >
                <label class="checkbox-item"
                  ><input v-model="filterForm.birthday" type="checkbox" value="this_week" /><span
                    >이번주</span
                  ></label
                >
                <label class="checkbox-item"
                  ><input v-model="filterForm.birthday" type="checkbox" value="today" /><span
                    >오늘</span
                  ></label
                >
              </div>
            </div>

            <!-- 출생연도 -->
            <div class="filter-section">
              <h4 class="section-title">출생연도</h4>
              <div class="range-row">
                <PrimeDatePicker
                  v-model="filterForm.birthYearStart"
                  view="year"
                  date-format="yy"
                  placeholder="시작 연도"
                  :base-z-index="2001"
                />
                <span class="tilde">~</span>
                <PrimeDatePicker
                  v-model="filterForm.birthYearEnd"
                  view="year"
                  date-format="yy"
                  placeholder="종료 연도"
                  :base-z-index="2001"
                />
              </div>
            </div>

            <!-- 성별 -->
            <div class="filter-section">
              <h4 class="section-title">성별</h4>
              <div class="checkbox-group horizontal">
                <label class="checkbox-item"
                  ><input v-model="filterForm.gender" type="checkbox" value="여성" /><span
                    >여성</span
                  ></label
                >
                <label class="checkbox-item"
                  ><input v-model="filterForm.gender" type="checkbox" value="남성" /><span
                    >남성</span
                  ></label
                >
              </div>
            </div>

            <!-- 태그 -->
            <div class="filter-section">
              <h4 class="section-title">태그</h4>
              <div class="checkbox-group horizontal">
                <label v-for="tag in dummyTags" :key="tag.tagId" class="checkbox-item">
                  <input v-model="filterForm.tags" type="checkbox" :value="tag.tagId" />
                  <span>{{ tag.tagName }}</span>
                </label>
              </div>
            </div>

            <!-- 담당자 -->
            <div class="filter-section">
              <h4 class="section-title">담당자</h4>
              <div class="checkbox-group horizontal">
                <label v-for="staff in dummyStaff" :key="staff.id" class="checkbox-item">
                  <input v-model="filterForm.staff" type="checkbox" :value="staff.id" />
                  <span>{{ staff.name }}</span>
                </label>
              </div>
            </div>

            <!-- 등급 -->
            <div class="filter-section">
              <h4 class="section-title">등급</h4>
              <div class="checkbox-group horizontal">
                <label v-for="grade in dummyGrades" :key="grade.id" class="checkbox-item">
                  <input v-model="filterForm.grades" type="checkbox" :value="grade.id" />
                  <span>{{ grade.name }}</span>
                </label>
              </div>
            </div>

            <!-- 선불액 구매내역 -->
            <div class="filter-section">
              <h4 class="section-title">선불액 구매내역</h4>
              <div class="checkbox-group horizontal">
                <label class="checkbox-item"
                  ><input v-model="filterForm.prepaidHistory" type="checkbox" value="yes" /><span
                    >있음</span
                  ></label
                >
                <label class="checkbox-item"
                  ><input v-model="filterForm.prepaidHistory" type="checkbox" value="no" /><span
                    >없음</span
                  ></label
                >
              </div>
            </div>
            <!-- 횟수권 구매내역 -->
            <div class="filter-section">
              <h4 class="section-title">횟수권 구매내역</h4>
              <div class="checkbox-group horizontal">
                <label class="checkbox-item"
                  ><input
                    v-model="filterForm.sessionPassHistory"
                    type="checkbox"
                    value="yes"
                  /><span>있음</span></label
                >
                <label class="checkbox-item"
                  ><input v-model="filterForm.sessionPassHistory" type="checkbox" value="no" /><span
                    >없음</span
                  ></label
                >
              </div>
            </div>
            <!-- 사용가능 선불액 -->
            <div class="filter-section">
              <h4 class="section-title">사용가능 선불액</h4>
              <div class="checkbox-group horizontal">
                <label class="checkbox-item"
                  ><input v-model="filterForm.usablePrepaid" type="checkbox" value="yes" /><span
                    >있음</span
                  ></label
                >
                <label class="checkbox-item"
                  ><input v-model="filterForm.usablePrepaid" type="checkbox" value="no" /><span
                    >없음</span
                  ></label
                >
              </div>
            </div>
            <!-- 사용가능 횟수권 -->
            <div class="filter-section">
              <h4 class="section-title">사용가능 횟수권</h4>
              <div class="checkbox-group horizontal">
                <label class="checkbox-item"
                  ><input v-model="filterForm.usableSessionPass" type="checkbox" value="yes" /><span
                    >있음</span
                  ></label
                >
                <label class="checkbox-item"
                  ><input v-model="filterForm.usableSessionPass" type="checkbox" value="no" /><span
                    >없음</span
                  ></label
                >
              </div>
            </div>

            <!-- 누적 매출액 -->
            <div class="filter-section">
              <h4 class="section-title">누적 매출액</h4>
              <div class="range-row">
                <input
                  v-model.number="filterForm.totalRevenue.from"
                  type="number"
                  placeholder="최소 금액"
                />
                <span class="tilde">~</span>
                <input
                  v-model.number="filterForm.totalRevenue.to"
                  type="number"
                  placeholder="최대 금액"
                />
              </div>
            </div>
            <!-- 잔여 선불액 -->
            <!--
            <div class="filter-section">
              <h4 class="section-title">잔여 선불액</h4>
              <div class="range-row">
                <input
                  v-model.number="filterForm.remainingPrepaid.from"
                  type="number"
                  placeholder="최소 금액"
                />
                <span class="tilde">~</span>
                <input
                  v-model.number="filterForm.remainingPrepaid.to"
                  type="number"
                  placeholder="최대 금액"
                />
              </div>
            </div>
            -->
            <!-- 방문 횟수 -->
            <div class="filter-section">
              <h4 class="section-title">방문 횟수</h4>
              <div class="range-row">
                <input
                  v-model.number="filterForm.visitCount.from"
                  type="number"
                  placeholder="최소 횟수"
                />
                <span class="tilde">~</span>
                <input
                  v-model.number="filterForm.visitCount.to"
                  type="number"
                  placeholder="최대 횟수"
                />
              </div>
            </div>
            <!-- 노쇼 횟수 -->
            <div class="filter-section">
              <h4 class="section-title">노쇼 횟수</h4>
              <div class="range-row">
                <input
                  v-model.number="filterForm.noShowCount.from"
                  type="number"
                  placeholder="최소 횟수"
                />
                <span class="tilde">~</span>
                <input
                  v-model.number="filterForm.noShowCount.to"
                  type="number"
                  placeholder="최대 횟수"
                />
              </div>
            </div>

            <!-- 최초 등록일 -->
            <div class="filter-section">
              <h4 class="section-title">최초 등록일</h4>
              <div class="date-filter-group horizontal">
                <select v-model="filterForm.registrationDate.mode" class="date-filter-select">
                  <option value="none">필터 없음</option>
                  <optgroup label="달력에서 날짜 선택">
                    <option value="before">이전</option>
                    <option value="after">이후</option>
                    <option value="range">범위</option>
                  </optgroup>
                  <optgroup label="해당일로부터 기간">
                    <option value="within_days">최근 ~일 이내</option>
                    <option value="days_ago">~일 이상 경과</option>
                  </optgroup>
                </select>
                <PrimeDatePicker
                  v-if="['before', 'after'].includes(filterForm.registrationDate.mode)"
                  v-model="filterForm.registrationDate.date1"
                  placeholder="날짜 선택"
                  :base-z-index="2001"
                />
                <div v-if="filterForm.registrationDate.mode === 'range'" class="range-row">
                  <PrimeDatePicker
                    v-model="filterForm.registrationDate.date1"
                    placeholder="시작일"
                    :base-z-index="2001"
                  />
                  <span class="tilde">~</span>
                  <PrimeDatePicker
                    v-model="filterForm.registrationDate.date2"
                    placeholder="종료일"
                    :base-z-index="2001"
                  />
                </div>
                <input
                  v-if="['within_days', 'days_ago'].includes(filterForm.registrationDate.mode)"
                  v-model.number="filterForm.registrationDate.days"
                  type="number"
                  placeholder="일수 입력"
                  class="days-input"
                />
              </div>
            </div>

            <!-- 최근 방문일 -->
            <div class="filter-section">
              <h4 class="section-title">최근 방문일</h4>
              <div class="date-filter-group horizontal">
                <select v-model="filterForm.recentVisitDate.mode" class="date-filter-select">
                  <option value="none">필터 없음</option>
                  <optgroup label="달력에서 날짜 선택">
                    <option value="before">이전</option>
                    <option value="after">이후</option>
                    <option value="range">범위</option>
                  </optgroup>
                  <optgroup label="해당일로부터 기간">
                    <option value="within_days">최근 ~일 이내</option>
                    <option value="days_ago">~일 이상 경과</option>
                  </optgroup>
                </select>
                <PrimeDatePicker
                  v-if="['before', 'after'].includes(filterForm.recentVisitDate.mode)"
                  v-model="filterForm.recentVisitDate.date1"
                  placeholder="날짜 선택"
                  :base-z-index="2001"
                />
                <div v-if="filterForm.recentVisitDate.mode === 'range'" class="range-row">
                  <PrimeDatePicker
                    v-model="filterForm.recentVisitDate.date1"
                    placeholder="시작일"
                    :base-z-index="2001"
                  />
                  <span class="tilde">~</span>
                  <PrimeDatePicker
                    v-model="filterForm.recentVisitDate.date2"
                    placeholder="종료일"
                    :base-z-index="2001"
                  />
                </div>
                <input
                  v-if="['within_days', 'days_ago'].includes(filterForm.recentVisitDate.mode)"
                  v-model.number="filterForm.recentVisitDate.days"
                  type="number"
                  placeholder="일수 입력"
                  class="days-input"
                />
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <BaseButton type="secondary" outline @click="closeModal">닫기</BaseButton>
          <BaseButton type="primary" @click="applyFilters">저장</BaseButton>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
  import { ref, watch, computed } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import { RefreshCwIcon, AlertTriangleIcon } from 'lucide-vue-next';
  import { useMetadataStore } from '@/store/metadata.js';

  const props = defineProps({
    modelValue: { type: Boolean, required: true },
    initialFilters: { type: Object, default: () => ({}) },
  });
  const emit = defineEmits(['update:modelValue', 'apply-filters']);

  const metadataStore = useMetadataStore();
  const dummyTags = computed(() => metadataStore.tags);
  const dummyStaff = computed(() => metadataStore.staff);
  const dummyGrades = computed(() => metadataStore.grades);

  const initialFormState = () => ({
    searchType: 'and',
    registrationDate: { mode: 'none', date1: null, date2: null, days: null },
    recentVisitDate: { mode: 'none', date1: null, date2: null, days: null },
    birthday: [],
    birthYearStart: null,
    birthYearEnd: null,
    gender: [],
    tags: [],
    staff: [],
    grades: [],
    prepaidHistory: [],
    sessionPassHistory: [],
    usablePrepaid: [],
    usableSessionPass: [],
    totalRevenue: { from: null, to: null },
    remainingPrepaid: { from: null, to: null },
    visitCount: { from: null, to: null },
    noShowCount: { from: null, to: null },
  });

  const filterForm = ref(initialFormState());

  watch(
    () => props.modelValue,
    newValue => {
      if (newValue) {
        updateFormWithInitialValues();
      }
    }
  );

  function updateFormWithInitialValues() {
    const newForm = initialFormState();
    const initial = props.initialFilters;

    for (const key in initial) {
      if (Object.prototype.hasOwnProperty.call(newForm, key)) {
        if (key === 'birthYearStart' && initial[key]) {
          newForm[key] = new Date(initial[key], 0, 1);
        } else if (key === 'birthYearEnd' && initial[key]) {
          newForm[key] = new Date(initial[key], 0, 1);
        } else if ((key === 'registrationDate' || key === 'recentVisitDate') && initial[key]) {
          newForm[key] = {
            ...initial[key],
            date1: initial[key].date1 ? new Date(initial[key].date1) : null,
            date2: initial[key].date2 ? new Date(initial[key].date2) : null,
          };
        } else {
          newForm[key] = JSON.parse(JSON.stringify(initial[key]));
        }
      }
    }
    filterForm.value = newForm;
  }

  function resetFilters() {
    filterForm.value = initialFormState();
  }

  function applyFilters() {
    const filtersToEmit = {};
    for (const key in filterForm.value) {
      const value = filterForm.value[key];
      // Date 객체를 적절한 형식으로 변환하여 emit
      if (key === 'birthYearStart' && value) {
        filtersToEmit[key] = new Date(value).getFullYear();
      } else if (key === 'birthYearEnd' && value) {
        filtersToEmit[key] = new Date(value).getFullYear();
      } else if ((key === 'registrationDate' || key === 'recentVisitDate') && value) {
        filtersToEmit[key] = {
          ...value,
          date1: value.date1 ? new Date(value.date1).toISOString().split('T')[0] : null,
          date2: value.date2 ? new Date(value.date2).toISOString().split('T')[0] : null,
        };
      } else {
        filtersToEmit[key] = value;
      }
    }
    emit('apply-filters', filtersToEmit);
    closeModal();
  }

  function closeModal() {
    emit('update:modelValue', false);
  }
</script>

<style scoped>
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 2000;
  }
  .modal-content {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
    width: 480px;
    max-width: 95vw;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem 1.5rem;
    border-bottom: 1px solid #eee;
    flex-shrink: 0;
  }
  .modal-title {
    font-size: 18px;
    font-weight: 700;
    margin: 0;
  }
  .modal-close {
    background: none;
    border: none;
    font-size: 24px;
    cursor: pointer;
    color: #888;
  }
  .modal-body {
    padding: 1.5rem;
    max-height: 75vh;
    overflow-y: auto;
    flex-grow: 1;
  }
  .modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding: 1rem 1.5rem;
    border-top: 1px solid #eee;
    flex-shrink: 0;
  }
  .filter-header-actions {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 1.5rem;
  }
  .filter-form {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }
  .filter-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .section-title {
    font-size: 15px;
    font-weight: 600;
    color: #333;
    margin: 0;
  }
  .search-type-desc {
    font-size: 13px;
    color: #555;
    display: flex;
    align-items: center;
    gap: 6px;
    margin: -4px 0 0 0;
  }
  .checkbox-group.horizontal,
  .radio-group.horizontal,
  .date-filter-group.horizontal {
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
    gap: 1rem;
  }
  .range-row {
    display: flex;
    align-items: center;
    width: 100%;
    gap: 8px;
  }
  .range-row > *:not(.tilde) {
    flex: 1 1 0;
    min-width: 0;
  }
  .range-row .tilde {
    flex: 0 0 auto;
    width: 24px;
    text-align: center;
    color: #888;
    font-weight: bold;
    font-size: 18px;
    padding-bottom: 2px;
  }
  .checkbox-item,
  .radio-item {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
  }
  input[type='checkbox'],
  input[type='radio'] {
    width: 18px;
    height: 18px;
    accent-color: #364f6b;
  }
  .date-filter-select,
  .range-row input,
  .days-input,
  .range-row :deep(.p-inputtext),
  .date-filter-inputs :deep(.p-inputtext) {
    width: 100%;
    height: 40px;
    border: 1px solid #ddd;
    border-radius: 8px;
    padding: 0 12px;
    font-size: 14px;
    background-color: #fff;
    box-sizing: border-box;
  }
</style>
