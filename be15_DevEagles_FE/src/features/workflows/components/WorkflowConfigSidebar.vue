<!-- eslint-disable vue/no-mutating-props -->
<template>
  <transition name="sidebar-slide">
    <div v-show="show" class="config-sidebar">
      <div class="sidebar-header">
        <h3 class="sidebar-title">{{ getSidebarTitle() }}</h3>
        <BaseButton variant="icon" type="ghost" size="sm" @click="$emit('close')">✕</BaseButton>
      </div>

      <div class="sidebar-body">
        <!-- Basic Configuration -->
        <div v-if="currentSidebarType === 'basic'" class="config-section">
          <div class="config-group">
            <label class="config-label">워크플로우 제목 *</label>
            <input
              v-model="formData.title"
              class="config-input"
              type="text"
              placeholder="예: 장기 미방문 고객 리타겟팅"
            />
          </div>
          <div class="config-group">
            <label class="config-label">설명</label>
            <textarea
              v-model="formData.description"
              class="config-textarea"
              placeholder="워크플로우에 대한 간단한 설명"
              rows="3"
            ></textarea>
          </div>
        </div>

        <!-- Target Configuration -->
        <div v-if="currentSidebarType === 'target'" class="config-section">
          <div class="config-group">
            <BaseTagSelector
              v-model="formData.targetCustomerGrades"
              label="고객 등급"
              placeholder="등급을 선택하세요"
              :options="gradeOptions"
              :value-prop="'id'"
              size="sm"
            />
          </div>
          <div class="config-group">
            <BaseTagSelector
              v-model="formData.targetTags"
              label="고객 태그"
              placeholder="태그를 선택하세요"
              :options="tagOptions"
              :value-prop="'id'"
              size="sm"
            />
          </div>
          <div class="config-group">
            <label class="config-label">필터 옵션</label>
            <div class="checkbox-group">
              <label class="checkbox-label">
                <input
                  v-model="formData.excludeDormantCustomers"
                  type="checkbox"
                  class="config-checkbox"
                />
                <span class="filter-main">휴면 고객 제외</span>
                <span class="filter-detail">
                  (<input
                    v-model="formData.dormantPeriodMonths"
                    type="number"
                    min="1"
                    max="12"
                    class="filter-input"
                    :disabled="!formData.excludeDormantCustomers"
                  />개월 이상 미방문)
                </span>
              </label>
              <label class="checkbox-label">
                <input
                  v-model="formData.excludeRecentMessageReceivers"
                  type="checkbox"
                  class="config-checkbox"
                />
                <span class="filter-main">최근 마케팅 메시지 수신자 제외</span>
                <span class="filter-detail">
                  (<input
                    v-model="formData.recentMessagePeriodDays"
                    type="number"
                    min="1"
                    max="90"
                    class="filter-input"
                    :disabled="!formData.excludeRecentMessageReceivers"
                  />일 이내 발송 이력)
                </span>
              </label>
            </div>
          </div>
        </div>

        <!-- Trigger Configuration -->
        <div v-if="currentSidebarType === 'trigger'" class="config-section">
          <Transition name="trigger-view" mode="out-in">
            <!-- 카테고리 선택 화면 -->
            <div
              v-if="currentTriggerView === 'category'"
              key="category"
              class="config-group trigger-view"
            >
              <label class="config-label">트리거 카테고리</label>
              <div class="option-cards">
                <div
                  v-for="category in triggerCategories"
                  :key="category.value"
                  :class="[
                    'option-card',
                    { selected: formData.triggerCategory === category.value },
                  ]"
                  @click="$emit('selectTriggerCategory', category.value)"
                >
                  <div class="card-icon">{{ category.icon }}</div>
                  <div class="card-title">{{ category.title }}</div>
                  <div class="card-desc">{{ category.description }}</div>
                </div>
              </div>
            </div>

            <!-- 세부 트리거 선택 화면 -->
            <div
              v-else-if="currentTriggerView === 'detail'"
              key="detail"
              class="config-group trigger-view"
            >
              <div class="config-header">
                <BaseButton type="ghost" size="sm" @click="$emit('goBackToCategory')"
                  >← 뒤로가기</BaseButton
                >
                <label class="config-label">{{ getCurrentCategoryTitle() }}</label>
              </div>
              <div class="option-cards">
                <div
                  v-for="trigger in filteredTriggerOptions"
                  :key="trigger.value"
                  :class="['option-card', { selected: formData.trigger === trigger.value }]"
                  @click="$emit('selectTrigger', trigger.value)"
                >
                  <div class="card-icon">{{ trigger.icon }}</div>
                  <div class="card-title">{{ trigger.title }}</div>
                  <div class="card-desc">{{ trigger.description }}</div>
                </div>
              </div>
            </div>
          </Transition>

          <!-- Trigger Details -->
          <Transition name="fade-slide-down">
            <div v-if="formData.trigger" key="trigger-details" class="config-group">
              <label class="config-label">세부 설정</label>

              <div v-if="formData.trigger === 'visit-cycle'" class="config-details">
                <label class="detail-label">기준 일수 *</label>
                <input
                  v-model="formData.triggerConfig.visitCycleDays"
                  class="config-input"
                  type="number"
                  min="1"
                  placeholder="7"
                />
                <p class="detail-help">평균 방문주기 + 이 일수가 지나면 실행됩니다</p>
              </div>

              <div v-if="formData.trigger === 'specific-treatment'" class="config-details">
                <label class="detail-label">시술 선택 *</label>
                <select v-model="formData.triggerConfig.treatmentId" class="config-select">
                  <option value="">시술을 선택하세요</option>
                  <option
                    v-for="option in treatmentOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.text }}
                  </option>
                </select>
                <label class="detail-label">시술 후 경과 일수</label>
                <input
                  v-model="formData.triggerConfig.daysAfterTreatment"
                  class="config-input"
                  type="number"
                  min="0"
                  placeholder="7"
                />
              </div>

              <!-- 이벤트 기반 트리거 설정 -->
              <div v-if="formData.trigger === 'birthday'" class="config-details">
                <label class="detail-label">생일 며칠 전</label>
                <input
                  v-model="formData.triggerConfig.birthdayDaysBefore"
                  class="config-input"
                  type="number"
                  min="0"
                  placeholder="3"
                />
                <p class="detail-help">생일 며칠 전에 실행됩니다 (0은 당일)</p>
              </div>

              <div v-if="formData.trigger === 'first-visit-anniversary'" class="config-details">
                <p class="detail-help">고객의 첫 방문 기념일에 자동으로 실행됩니다</p>
              </div>

              <div v-if="formData.trigger === 'visit-milestone'" class="config-details">
                <label class="detail-label">목표 방문 횟수</label>
                <input
                  v-model="formData.triggerConfig.visitMilestone"
                  class="config-input"
                  type="number"
                  min="1"
                  placeholder="10"
                />
                <p class="detail-help">해당 방문 횟수 달성 시 실행됩니다</p>
              </div>

              <div v-if="formData.trigger === 'amount-milestone'" class="config-details">
                <label class="detail-label">목표 누적 금액 (원)</label>
                <input
                  v-model="formData.triggerConfig.amountMilestone"
                  class="config-input"
                  type="number"
                  min="0"
                  step="10000"
                  placeholder="100000"
                />
                <p class="detail-help">해당 누적 금액 달성 시 실행됩니다</p>
              </div>

              <div v-if="formData.trigger === 'first-visit-days-after'" class="config-details">
                <label class="detail-label">첫 방문 이후 경과 일수</label>
                <input
                  v-model="formData.triggerConfig.daysAfterFirstVisit"
                  class="config-input"
                  type="number"
                  min="1"
                  placeholder="30"
                />
                <p class="detail-help">첫 방문일로부터 해당 일수가 경과하면 실행됩니다</p>
              </div>

              <!-- 이탈 위험 기반 트리거 설정 -->
              <div v-if="formData.trigger === 'new-customer-followup'" class="config-details">
                <label class="detail-label">팔로업 일수</label>
                <input
                  v-model="formData.triggerConfig.followupDays"
                  class="config-input"
                  type="number"
                  min="1"
                  placeholder="7"
                />
                <p class="detail-help">신규 고객 등록 후 해당 일수가 지나면 자동으로 실행됩니다</p>
              </div>

              <div v-if="formData.trigger === 'new-customer-at-risk'" class="config-details">
                <p class="detail-help">신규 고객이 30일 이상 미방문 시 자동으로 실행됩니다</p>
              </div>

              <div v-if="formData.trigger === 'reactivation-needed'" class="config-details">
                <p class="detail-help">60일 이상 미방문 고객에게 자동으로 실행됩니다</p>
              </div>

              <div v-if="formData.trigger === 'growing-delayed'" class="config-details">
                <p class="detail-help">성장 고객의 예상 방문일이 지연된 경우 자동으로 실행됩니다</p>
              </div>

              <div v-if="formData.trigger === 'loyal-delayed'" class="config-details">
                <p class="detail-help">충성 고객의 예상 방문일이 지연된 경우 자동으로 실행됩니다</p>
              </div>

              <div v-if="formData.trigger === 'vip-attention-needed'" class="config-details">
                <p class="detail-help">
                  VIP 고객의 방문 패턴에 변화가 감지된 경우 자동으로 실행됩니다
                </p>
              </div>

              <div v-if="formData.trigger === 'churn-risk-high'" class="config-details">
                <p class="detail-help">
                  AI 모델이 높은 이탈 위험도를 예측한 고객에게 자동으로 실행됩니다
                </p>
              </div>
            </div>
          </Transition>
        </div>

        <!-- Action Configuration -->
        <div v-if="currentSidebarType === 'action'" class="config-section">
          <div class="config-group">
            <label class="config-label">액션 유형</label>
            <div class="option-cards">
              <div
                v-for="action in actionOptions"
                :key="action.value"
                :class="['option-card', { selected: formData.action === action.value }]"
                @click="$emit('selectAction', action.value)"
              >
                <div class="card-icon">{{ action.icon }}</div>
                <div class="card-title">{{ action.title }}</div>
                <div class="card-desc">{{ action.description }}</div>
              </div>
            </div>
          </div>

          <!-- Action Details -->
          <div v-if="formData.action" class="config-group">
            <label class="config-label">세부 설정</label>

            <div v-if="formData.action === 'message-only'" class="config-details">
              <CompactTemplateSelector
                v-model="formData.actionConfig.selectedTemplate"
                label="메시지 템플릿 *"
                placeholder="메시지 템플릿을 선택하세요"
                @template-selected="handleTemplateSelected"
                @update:model-value="handleTemplateModelValueUpdate"
              />

              <label class="detail-label">발송 시간 *</label>
              <PrimeDatePicker
                v-model="formData.actionConfig.sendTime"
                :time-only="true"
                :show-time="true"
                placeholder="발송 시간을 선택하세요"
                date-format="HH:mm"
                :show-icon="true"
                :clearable="true"
              />
              <p class="detail-help">메시지를 발송할 시간을 설정하세요</p>
            </div>

            <div v-if="formData.action === 'coupon-message'" class="config-details">
              <CompactCouponSelector
                v-model="formData.actionConfig.selectedCoupon"
                label="쿠폰 선택 *"
                placeholder="쿠폰을 선택하세요"
                :multiple="false"
                :filter-options="couponFilterOptions"
                @coupon-selected="handleCouponSelected"
                @update:model-value="handleCouponModelValueUpdate"
              />

              <CompactTemplateSelector
                v-model="formData.actionConfig.selectedTemplate"
                label="메시지 템플릿 *"
                placeholder="메시지 템플릿을 선택하세요"
                @template-selected="handleTemplateSelected"
                @update:model-value="handleTemplateModelValueUpdate"
              />

              <label class="detail-label">발송 시간 *</label>
              <PrimeDatePicker
                v-model="formData.actionConfig.sendTime"
                :time-only="true"
                :show-time="true"
                placeholder="발송 시간을 선택하세요"
                date-format="HH:mm"
                :show-icon="true"
                :clearable="true"
              />
              <p class="detail-help">쿠폰과 메시지를 발송할 시간을 설정하세요</p>
            </div>

            <div v-if="formData.action === 'system-notification'" class="config-details">
              <label class="detail-label">알림 제목 *</label>
              <input
                v-model="formData.actionConfig.notificationTitle"
                class="config-input"
                type="text"
                placeholder="알림 제목"
              />
              <label class="detail-label">알림 내용 *</label>
              <textarea
                v-model="formData.actionConfig.notificationContent"
                class="config-textarea"
                placeholder="알림 내용"
                rows="3"
              ></textarea>
            </div>
          </div>
        </div>
      </div>

      <div class="sidebar-footer">
        <BaseButton type="cancel" @click="$emit('close')">취소</BaseButton>
        <BaseButton type="primary" @click="$emit('apply')">적용</BaseButton>
      </div>
    </div>
  </transition>
</template>

<script>
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseTagSelector from '@/components/common/BaseTagSelector.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';
  import { CompactCouponSelector } from '@/features/coupons';
  import CompactTemplateSelector from '@/components/common/CompactTemplateSelector.vue';
  import {
    triggerCategories,
    actionOptions,
    treatmentOptions,
    messageTemplateOptions,
    couponOptions,
  } from '../constants/workflowOptions.js';

  import { ref, computed, onMounted } from 'vue';
  import { useAuthStore } from '@/store/auth.js';
  import GradesAPI from '@/features/customer/api/grades.js';
  import TagsAPI from '@/features/customer/api/tags.js';

  export default {
    name: 'WorkflowConfigSidebar',
    components: {
      BaseButton,
      BaseTagSelector,
      PrimeDatePicker,
      CompactCouponSelector,
      CompactTemplateSelector,
    },
    props: {
      show: {
        type: Boolean,
        default: false,
      },
      currentSidebarType: {
        type: String,
        required: true,
      },
      currentTriggerView: {
        type: String,
        required: true,
      },
      formData: {
        type: Object,
        required: true,
      },
      getSidebarTitle: {
        type: Function,
        required: true,
      },
      getCurrentCategoryTitle: {
        type: Function,
        required: true,
      },
      filteredTriggerOptions: {
        type: Array,
        required: true,
      },
    },
    emits: [
      'close',
      'apply',
      'selectTriggerCategory',
      'goBackToCategory',
      'selectTrigger',
      'selectAction',
      'updateActionConfig',
    ],
    setup(props, { emit }) {
      // Auth store for shopId
      const authStore = useAuthStore();

      // Dynamic options
      const gradeOptions = ref([]);
      const tagOptions = ref([]);

      onMounted(async () => {
        try {
          const grades = await GradesAPI.getGradesByShop();
          gradeOptions.value = grades.map(g => ({
            id: g.id,
            tagName: g.name,
            colorCode: '#6B7280',
          }));

          const tags = await TagsAPI.getTagsByShop();
          tagOptions.value = tags.map(t => ({
            id: t.tagId,
            tagName: t.tagName,
            colorCode: t.colorCode || '#6B7280',
          }));
        } catch (error) {
          console.error('워크플로우 사이드바 옵션 로드 실패', error);
        }

        // authStore.shopId가 없으면 fetchShopId 호출 (캠페인 폼과 동일)
        if (!authStore.shopId) {
          authStore.fetchShopId && authStore.fetchShopId();
        }
      });

      // Event handlers for new compact selectors
      const handleCouponSelected = coupon => {
        // Handle single coupon selection (not array)
        if (coupon) {
          emit('updateActionConfig', {
            updates: [
              {
                field: 'couponId',
                value: coupon.id || coupon.value,
              },
              {
                field: 'couponName',
                value: coupon.couponTitle || coupon.name || coupon.text,
              },
              {
                field: 'selectedCoupon',
                value: coupon,
              },
            ],
          });
        }
      };

      const handleCouponModelValueUpdate = coupon => {
        // Handle single coupon model value update (not array)
        if (coupon) {
          emit('updateActionConfig', {
            updates: [
              {
                field: 'couponId',
                value: coupon.id || coupon.value,
              },
              {
                field: 'couponName',
                value: coupon.couponTitle || coupon.name || coupon.text,
              },
              {
                field: 'selectedCoupon',
                value: coupon,
              },
            ],
          });
        }
      };

      const handleTemplateSelected = template => {
        if (template) {
          // Update formData directly and emit
          emit('updateActionConfig', {
            updates: [
              {
                field: 'messageTemplateId',
                value: template.value || template.id,
              },
              {
                field: 'messageTemplateName',
                value: template.name || template.text,
              },
              {
                field: 'selectedTemplate',
                value: template,
              },
            ],
          });
        }
      };

      const handleTemplateModelValueUpdate = template => {
        if (template) {
          // Update formData directly and emit
          emit('updateActionConfig', {
            updates: [
              {
                field: 'messageTemplateId',
                value: template.value || template.id,
              },
              {
                field: 'messageTemplateName',
                value: template.name || template.text,
              },
              {
                field: 'selectedTemplate',
                value: template,
              },
            ],
          });
        }
      };

      // Computed for filter options
      const couponFilterOptions = computed(() => {
        const options = {
          onlyActive: true,
          shopId: authStore.shopId,
        };
        console.log('WorkflowConfigSidebar - couponFilterOptions:', options);
        return options;
      });

      return {
        triggerCategories,
        actionOptions,
        treatmentOptions,
        messageTemplateOptions,
        couponOptions,
        gradeOptions,
        tagOptions,
        couponFilterOptions,
        handleCouponSelected,
        handleCouponModelValueUpdate,
        handleTemplateSelected,
        handleTemplateModelValueUpdate,
      };
    },
  };
</script>

<style scoped>
  /* Configuration Sidebar */
  .config-sidebar {
    width: 420px;
    background: var(--color-neutral-white);
    border-radius: 3px;
    border: 1px solid var(--color-gray-200);
    box-shadow: 0 8px 40px -10px rgba(0, 0, 0, 0.08);
    display: flex;
    flex-direction: column;
    height: fit-content;
    max-height: 80vh;
    flex-shrink: 0;
  }

  .sidebar-header {
    padding: 16px 20px;
    border-bottom: 1px solid var(--color-gray-200);
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: var(--color-gray-50);
  }

  .sidebar-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--color-neutral-dark);
    margin: 0;
  }

  .sidebar-body {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
  }

  .config-section {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .config-group {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .config-label {
    font-size: 12px;
    font-weight: 600;
    color: var(--color-neutral-dark);
    margin-bottom: 6px;
  }

  .config-input,
  .config-textarea,
  .config-select {
    padding: 8px;
    border: 1px solid var(--color-gray-200);
    border-radius: 3px;
    font-size: 13px;
    color: var(--color-neutral-dark);
    background: var(--color-neutral-white);
    transition: border-color 0.2s;
  }

  .config-input:focus,
  .config-textarea:focus,
  .config-select:focus {
    outline: none;
    border-color: var(--color-primary-main);
    box-shadow: 0 0 0 2px rgba(54, 79, 107, 0.2);
  }

  .config-textarea {
    resize: vertical;
    min-height: 80px;
  }

  .config-select {
    appearance: none;
    background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6,9 12,15 18,9'%3e%3c/polyline%3e%3c/svg%3e");
    background-repeat: no-repeat;
    background-position: right 12px center;
    background-size: 16px;
    padding-right: 40px;
  }

  /* Option Cards */
  .option-cards {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .option-card {
    padding: 12px;
    border: 1px solid var(--color-gray-200);
    border-radius: 3px;
    cursor: pointer;
    transition: all 0.25s ease;
    text-align: left;
    display: flex;
    align-items: center;
    gap: 12px;
    transform: translateY(0);
  }

  .option-card:hover {
    border-color: var(--color-primary-main);
    background: var(--color-gray-50);
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  .option-card.selected {
    border-color: var(--color-primary-main);
    background: var(--color-primary-50);
    color: var(--color-primary-main);
    transform: translateY(-1px);
    box-shadow: 0 2px 12px rgba(54, 79, 107, 0.15);
  }

  .card-icon {
    font-size: 16px;
    width: 20px;
    text-align: center;
  }

  .card-title {
    font-size: 13px;
    font-weight: 500;
    margin-bottom: 2px;
    color: var(--color-neutral-dark);
  }

  .card-desc {
    font-size: 11px;
    color: var(--color-gray-500);
    line-height: 1.3;
  }

  .config-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }

  /* 트리거 뷰 전환 애니메이션 */
  .trigger-view {
    min-height: 200px;
  }

  .trigger-view-enter-active,
  .trigger-view-leave-active {
    transition: all 0.3s ease;
  }

  .trigger-view-enter-from {
    opacity: 0;
    transform: translateX(20px);
  }

  .trigger-view-leave-to {
    opacity: 0;
    transform: translateX(-20px);
  }

  .trigger-view-enter-to,
  .trigger-view-leave-from {
    opacity: 1;
    transform: translateX(0);
  }

  /* 세부 설정 등장 애니메이션 */
  .fade-slide-down-enter-active {
    transition: all 0.4s ease;
  }

  .fade-slide-down-leave-active {
    transition: all 0.3s ease;
  }

  .fade-slide-down-enter-from {
    opacity: 0;
    transform: translateY(-10px);
  }

  .fade-slide-down-leave-to {
    opacity: 0;
    transform: translateY(-5px);
  }

  .config-details {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-top: 16px;
    padding: 16px;
    background: var(--color-gray-50);
    border-radius: 8px;
  }

  .detail-label {
    font-size: 13px;
    font-weight: 600;
    color: var(--color-gray-700);
    margin-bottom: 6px;
  }

  .detail-help {
    font-size: 12px;
    color: var(--color-gray-500);
    margin: 0;
    margin-top: 6px;
  }

  /* Checkbox styles */
  .checkbox-group {
    margin-top: 8px;
  }

  .checkbox-label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: var(--color-neutral-dark);
    cursor: pointer;
    padding: 8px 0;
  }

  .config-checkbox {
    width: 16px;
    height: 16px;
    border: 1px solid var(--color-gray-300);
    border-radius: 3px;
    background: var(--color-neutral-white);
    cursor: pointer;
    accent-color: var(--color-primary-main);
  }

  .config-checkbox:checked {
    background: var(--color-primary-main);
    border-color: var(--color-primary-main);
  }

  .config-checkbox:focus {
    outline: none;
    box-shadow: 0 0 0 2px rgba(54, 79, 107, 0.2);
  }

  .filter-main {
    color: var(--color-neutral-dark);
  }

  .filter-detail {
    color: var(--color-gray-500);
    font-size: 11px;
    margin-left: 4px;
  }

  .filter-input {
    width: 40px;
    padding: 2px 4px;
    border: 1px solid var(--color-gray-300);
    border-radius: 3px;
    font-size: 11px;
    text-align: center;
    background: var(--color-neutral-white);
    color: var(--color-neutral-dark);
    margin: 0 2px;
  }

  .filter-input:focus {
    outline: none;
    border-color: var(--color-primary-main);
    box-shadow: 0 0 0 1px rgba(54, 79, 107, 0.2);
  }

  .filter-input:disabled {
    background: var(--color-gray-100);
    color: var(--color-gray-400);
    cursor: not-allowed;
  }

  .sidebar-footer {
    padding: 16px 20px;
    border-top: 1px solid var(--color-gray-200);
    display: flex;
    gap: 8px;
    justify-content: flex-end;
    background: var(--color-gray-50);
  }

  /* Sidebar slide animation */
  .sidebar-slide-enter-active,
  .sidebar-slide-leave-active {
    transition: transform 0.3s ease;
  }

  .sidebar-slide-enter-from,
  .sidebar-slide-leave-to {
    transform: translateX(100%);
  }
</style>
