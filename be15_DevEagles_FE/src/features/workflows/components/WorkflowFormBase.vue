<template>
  <div class="automation-builder">
    <!-- Header -->
    <div class="builder-header">
      <div class="header-info">
        <h1 class="builder-title">{{ title }}</h1>
        <p class="builder-subtitle">{{ subtitle }}</p>
      </div>
      <div class="header-actions">
        <div class="activation-toggle">
          <label class="toggle-label">활성화</label>
          <BaseToggleSwitch v-model="formData.isActive" />
        </div>
        <BaseButton type="cancel" @click="onCancel">취소</BaseButton>
        <BaseButton type="primary" :disabled="!isFormValid" @click="onSave">{{
          saveButtonText
        }}</BaseButton>
      </div>
    </div>

    <div class="builder-container" :class="{ 'sidebar-open': sidebarOpen }">
      <!-- Workflow Canvas -->
      <div class="workflow-canvas">
        <!-- Basic Info -->
        <div
          class="workflow-step step-basic"
          :class="{ 'step-valid': isBasicValid, 'step-invalid': !isBasicValid && formData.title }"
        >
          <div class="step-content">
            <div class="step-info">
              <div class="step-icon">
                <EditIcon :size="16" />
              </div>
              <div class="step-text">
                <div class="step-name">워크플로우 정보</div>
                <div class="step-desc">이름과 설명을 입력하세요</div>
              </div>
            </div>
            <button class="btn-configure" @click="openSidebar('basic')">
              {{ formData.title ? '수정' : '설정' }}
            </button>
          </div>
          <div v-if="formData.title" class="step-summary">
            <div class="summary-item">
              <span class="summary-label">제목:</span>
              <span class="summary-value">{{ formData.title }}</span>
            </div>
            <div v-if="formData.description" class="summary-item">
              <span class="summary-label">설명:</span>
              <span class="summary-value">{{ formData.description }}</span>
            </div>
          </div>
        </div>

        <!-- Flow Arrow -->
        <div class="flow-arrow">
          <div class="arrow-line"></div>
          <div class="arrow-head">▼</div>
        </div>

        <!-- Target (IF) -->
        <div class="workflow-step step-condition">
          <div class="step-content">
            <div class="step-info">
              <div class="step-icon">
                <UsersIcon :size="16" />
              </div>
              <div class="step-text">
                <div class="step-name">대상 고객</div>
                <div class="step-desc">조건에 맞는 고객 선택 (🚨 미선택시 전체 고객 발송)</div>
              </div>
            </div>
            <button class="btn-configure" @click="openSidebar('target')">
              {{ hasTargetConfig ? '수정' : '설정' }}
            </button>
          </div>
          <div v-if="hasTargetConfig" class="step-summary">
            <div v-if="formData.targetCustomerGrades.length" class="summary-item">
              <span class="summary-label">등급:</span>
              <div class="summary-tags">
                <span
                  v-for="gradeName in getGradeNames(formData.targetCustomerGrades)"
                  :key="gradeName"
                  class="tag"
                  >{{ gradeName }}</span
                >
              </div>
            </div>
            <div v-if="formData.targetTags.length" class="summary-item">
              <span class="summary-label">태그:</span>
              <div class="summary-tags">
                <span
                  v-for="tagName in getTagNames(formData.targetTags)"
                  :key="tagName"
                  class="tag"
                  >{{ tagName }}</span
                >
              </div>
            </div>
          </div>
        </div>

        <!-- Flow Arrow -->
        <div class="flow-arrow">
          <div class="arrow-line"></div>
          <div class="arrow-head">▼</div>
        </div>

        <!-- Trigger (WHEN) -->
        <div
          class="workflow-step step-trigger"
          :class="{
            'step-valid': isTriggerValid,
            'step-invalid': !isTriggerValid && formData.trigger,
          }"
        >
          <div class="step-content">
            <div class="step-info">
              <div class="step-icon">
                <BellIcon :size="16" />
              </div>
              <div class="step-text">
                <div class="step-name">트리거 선택</div>
                <div class="step-desc">워크플로우 실행 조건</div>
              </div>
            </div>
            <button class="btn-configure" @click="openSidebar('trigger')">
              {{ formData.trigger ? '수정' : '설정' }}
            </button>
          </div>
          <div v-if="formData.trigger" class="step-summary">
            <div class="summary-item">
              <span class="summary-icon">{{ getTriggerIcon(formData.trigger) }}</span>
              <div class="summary-info">
                <div class="summary-title">{{ getTriggerText(formData.trigger) }}</div>
                <div class="summary-details">
                  <span v-if="formData.trigger === 'visit-cycle'">
                    평균 방문주기 + {{ formData.triggerConfig.visitCycleDays }}일
                  </span>
                  <span v-if="formData.trigger === 'specific-treatment'">
                    {{ getTreatmentText(formData.triggerConfig.treatmentId) }}
                    <span v-if="formData.triggerConfig.daysAfterTreatment > 0">
                      ({{ formData.triggerConfig.daysAfterTreatment }}일 후)
                    </span>
                  </span>
                  <span v-if="formData.trigger === 'customer-registration'">
                    등록 후 {{ formData.triggerConfig.daysAfterRegistration }}일
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Flow Arrow -->
        <div class="flow-arrow">
          <div class="arrow-line"></div>
          <div class="arrow-head">▼</div>
        </div>

        <!-- Action (THEN) -->
        <div
          class="workflow-step step-action"
          :class="{
            'step-valid': isActionValid,
            'step-invalid': !isActionValid && formData.action,
          }"
        >
          <div class="step-content">
            <div class="step-info">
              <div class="step-icon">
                <MegaphoneIcon :size="16" />
              </div>
              <div class="step-text">
                <div class="step-name">실행 작업</div>
                <div class="step-desc">실행할 작업 선택</div>
              </div>
            </div>
            <button class="btn-configure" @click="openSidebar('action')">
              {{ formData.action ? '수정' : '설정' }}
            </button>
          </div>
          <div v-if="formData.action" class="step-summary">
            <div class="summary-item">
              <span class="summary-icon">{{ getActionIcon(formData.action) }}</span>
              <div class="summary-info">
                <div class="summary-title">{{ getActionText(formData.action) }}</div>
                <div class="summary-details">
                  <span v-if="formData.action === 'message-only'">
                    {{
                      getMessageTemplateText(formData.actionConfig.messageTemplateId) ||
                      '메시지 템플릿 미선택'
                    }}
                    <span v-if="formData.actionConfig.sendTime">
                      • {{ formatSendTime(formData.actionConfig.sendTime) }}</span
                    >
                  </span>
                  <span v-if="formData.action === 'coupon-message'">
                    {{ getCouponText(formData.actionConfig.couponId) || '쿠폰 미선택' }} +
                    {{
                      getMessageTemplateText(formData.actionConfig.messageTemplateId) ||
                      '메시지 템플릿 미선택'
                    }}
                    <span v-if="formData.actionConfig.sendTime">
                      • {{ formatSendTime(formData.actionConfig.sendTime) }}</span
                    >
                  </span>
                  <span v-if="formData.action === 'system-notification'">
                    {{ formData.actionConfig.notificationTitle || '알림 제목 미입력' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Configuration Sidebar -->
      <WorkflowConfigSidebar
        :show="sidebarOpen"
        :current-sidebar-type="currentSidebarType"
        :current-trigger-view="currentTriggerView"
        :form-data="formData"
        :get-sidebar-title="getSidebarTitle"
        :get-current-category-title="getCurrentCategoryTitle"
        :filtered-trigger-options="filteredTriggerOptions"
        @close="closeSidebar"
        @apply="applySidebarConfig"
        @select-trigger-category="selectTriggerCategory"
        @go-back-to-category="goBackToCategory"
        @select-trigger="selectTrigger"
        @select-action="selectAction"
        @update-action-config="handleActionConfigUpdate"
      />
    </div>

    <!-- Toast -->
    <BaseToast ref="toast" />
  </div>
</template>

<script>
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseToggleSwitch from '@/components/common/BaseToggleSwitch.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import EditIcon from '@/components/icons/EditIcon.vue';
  import UsersIcon from '@/components/icons/UsersIcon.vue';
  import BellIcon from '@/components/icons/BellIcon.vue';
  import MegaphoneIcon from '@/components/icons/MegaphoneIcon.vue';
  import WorkflowConfigSidebar from './WorkflowConfigSidebar.vue';
  import { useWorkflowForm } from '../composables/useWorkflowForm.js';
  import { onMounted } from 'vue';

  export default {
    name: 'WorkflowFormBase',
    components: {
      BaseButton,
      BaseToggleSwitch,
      BaseToast,
      EditIcon,
      UsersIcon,
      BellIcon,
      MegaphoneIcon,
      WorkflowConfigSidebar,
    },
    props: {
      title: {
        type: String,
        required: true,
      },
      subtitle: {
        type: String,
        required: true,
      },
      saveButtonText: {
        type: String,
        default: '저장',
      },
      initialData: {
        type: Object,
        default: null,
      },
    },
    emits: ['save', 'cancel'],
    setup(props, { emit }) {
      const {
        formData,
        sidebarOpen,
        currentSidebarType,
        currentTriggerView,
        hasTargetConfig,
        filteredTriggerOptions,
        isBasicValid,
        isTriggerValid,
        isActionValid,
        isFormValid,
        selectTriggerCategory,
        goBackToCategory,
        getCurrentCategoryTitle,
        selectTrigger,
        selectAction,
        openSidebar,
        closeSidebar,
        applySidebarConfig,
        getSidebarTitle,
        getTriggerIcon,
        getTriggerText,
        getActionIcon,
        getActionText,
        getTreatmentText,
        getMessageTemplateText,
        getCouponText,
        formatSendTime,
        loadGradesAndTags,
        getGradeNames,
        getTagNames,
      } = useWorkflowForm(props.initialData);

      const onSave = () => {
        emit('save', formData);
      };

      const onCancel = () => {
        emit('cancel');
      };

      const handleActionConfigUpdate = payload => {
        // Support both single field update and multiple field updates
        if (payload.updates && Array.isArray(payload.updates)) {
          // Multiple field updates
          payload.updates.forEach(({ field, value }) => {
            formData.actionConfig[field] = value;
          });
        } else if (payload.field && payload.value !== undefined) {
          // Single field update (backward compatibility)
          formData.actionConfig[payload.field] = payload.value;
        }
      };

      // Load grades and tags on mount
      onMounted(() => {
        loadGradesAndTags();
      });

      return {
        // State
        formData,
        sidebarOpen,
        currentSidebarType,
        currentTriggerView,

        // Computed
        hasTargetConfig,
        filteredTriggerOptions,
        isBasicValid,
        isTriggerValid,
        isActionValid,
        isFormValid,

        // Methods
        selectTriggerCategory,
        goBackToCategory,
        getCurrentCategoryTitle,
        selectTrigger,
        selectAction,
        openSidebar,
        closeSidebar,
        applySidebarConfig,
        getSidebarTitle,
        getTriggerIcon,
        getTriggerText,
        getActionIcon,
        getActionText,
        getTreatmentText,
        getMessageTemplateText,
        getCouponText,
        formatSendTime,
        getGradeNames,
        getTagNames,
        onSave,
        onCancel,
        handleActionConfigUpdate,
      };
    },
  };
</script>

<style scoped>
  .automation-builder {
    padding: 24px;
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    max-width: 1200px;
    margin: 0 auto;
    width: 100%;
  }

  /* Header */
  .builder-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 32px;
    padding: 24px;
    background: var(--color-neutral-white);
    border-radius: 3px;
    border: 1px solid var(--color-gray-200);
    box-shadow: 0 8px 40px -10px rgba(0, 0, 0, 0.08);
  }

  .header-info {
    flex: 1;
  }

  .builder-title {
    font-size: 24px;
    font-weight: 700;
    color: var(--color-neutral-dark);
    margin: 0;
  }

  .builder-subtitle {
    font-size: 14px;
    color: var(--color-gray-500);
    margin: 4px 0 0 0;
  }

  .header-actions {
    display: flex;
    gap: 24px;
    align-items: center;
  }

  .activation-toggle {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .toggle-label {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-neutral-dark);
    margin: 0;
  }

  /* Main Container */
  .builder-container {
    display: flex;
    gap: 24px;
    flex: 1;
    justify-content: flex-start;
    position: relative;
    align-items: flex-start;
    padding-left: calc(50% - 300px);
    transition: padding-left 0.3s ease;
  }

  .builder-container.sidebar-open {
    padding-left: calc(50% - 522px);
  }

  /* Workflow Canvas */
  .workflow-canvas {
    padding: 32px;
    width: 600px;
    flex-shrink: 0;
    height: fit-content;
  }

  /* Workflow Steps */
  .workflow-step {
    background: var(--color-neutral-white);
    border: 1px solid var(--color-gray-200);
    border-radius: 6px;
    margin-bottom: 8px;
    position: relative;
    transition: all 0.2s;
  }

  .workflow-step:hover {
    border-color: var(--color-primary-main);
    box-shadow: 0 0 0 1px var(--color-primary-main);
  }

  .workflow-step.step-valid {
    border-color: var(--color-success-500);
  }

  .workflow-step.step-invalid {
    border-color: var(--color-error-300);
  }

  .workflow-step.step-valid:hover {
    border-color: var(--color-success-500);
    box-shadow: 0 0 0 1px var(--color-success-500);
  }

  .workflow-step.step-invalid:hover {
    border-color: var(--color-error-300);
    box-shadow: 0 0 0 1px var(--color-error-300);
  }

  .step-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
  }

  .step-info {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
  }

  .step-icon {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-neutral-dark);
    flex-shrink: 0;
  }

  .step-text {
    flex: 1;
  }

  .step-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-neutral-dark);
    margin: 0;
  }

  .step-desc {
    font-size: 12px;
    color: var(--color-gray-500);
    margin: 2px 0 0 0;
  }

  .btn-configure {
    padding: 4px 8px;
    background: none;
    border: 1px solid var(--color-gray-200);
    border-radius: 3px;
    color: var(--color-gray-500);
    font-size: 12px;
    font-weight: 400;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-configure:hover {
    background: var(--color-gray-50);
    border-color: var(--color-gray-400);
  }

  /* Step Summary */
  .step-summary {
    padding: 8px 16px;
    background: var(--color-gray-50);
    border-top: 1px solid var(--color-gray-200);
    font-size: 12px;
    color: var(--color-gray-500);
  }

  .summary-item {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
  }

  .summary-item:last-child {
    margin-bottom: 0;
  }

  .summary-label {
    font-size: 12px;
    font-weight: 500;
    color: var(--color-gray-400);
    min-width: 40px;
  }

  .summary-value {
    font-size: 12px;
    color: var(--color-neutral-dark);
    flex: 1;
  }

  .summary-icon {
    font-size: 12px;
    margin-right: 4px;
  }

  .summary-info {
    flex: 1;
  }

  .summary-title {
    font-size: 12px;
    font-weight: 500;
    color: var(--color-neutral-dark);
    margin-bottom: 2px;
  }

  .summary-details {
    font-size: 11px;
    color: var(--color-gray-500);
  }

  .summary-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }

  .tag {
    padding: 1px 6px;
    background: var(--color-gray-200);
    color: var(--color-gray-600);
    border-radius: 8px;
    font-size: 10px;
    font-weight: 400;
  }

  /* Flow Arrows */
  .flow-arrow {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin: 4px 0;
  }

  .arrow-line {
    width: 1px;
    height: 12px;
    background: var(--color-gray-200);
  }

  .arrow-head {
    font-size: 10px;
    color: var(--color-gray-400);
    margin-top: 2px;
  }

  /* Responsive */
  @media (max-width: 1024px) {
    .builder-container {
      flex-direction: column;
      justify-content: center;
    }

    .workflow-canvas {
      width: 100%;
      max-width: 600px;
      margin: 0 auto;
    }
  }

  @media (max-width: 768px) {
    .automation-builder {
      padding: 16px;
    }

    .builder-header {
      flex-direction: column;
      align-items: stretch;
      gap: 16px;
    }

    .header-actions {
      justify-content: space-between;
      flex-wrap: wrap;
    }

    .activation-toggle {
      order: -1;
    }

    .workflow-canvas {
      padding: 24px;
    }
  }
</style>
