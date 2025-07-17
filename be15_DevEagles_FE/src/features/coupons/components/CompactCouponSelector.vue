<template>
  <div class="compact-coupon-selector">
    <!-- 라벨과 버튼이 한 행에 배치 -->
    <div class="selector-row">
      <label class="selector-label">{{ label }}</label>
      <div class="selector-buttons">
        <BaseButton
          type="secondary"
          size="sm"
          outline
          :disabled="readonly"
          class="select-button"
          @click="openSelector"
        >
          <TicketIcon :size="14" />
          {{ selectedCoupons.length > 0 ? '변경' : '선택' }}
        </BaseButton>
      </div>
    </div>

    <!-- 선택된 쿠폰 표시 -->
    <div v-if="selectedCoupons.length > 0" class="selected-display">
      <div class="selected-items">
        <div v-for="coupon in displayCoupons" :key="coupon.id" class="selected-chip">
          <div class="chip-content">
            <span class="chip-name">{{ coupon.couponTitle || coupon.name }}</span>
            <span class="chip-discount">{{ coupon.discountRate || coupon.discount }}%</span>
          </div>
          <BaseButton
            v-if="!readonly"
            type="ghost"
            size="xs"
            class="chip-remove"
            @click="removeFromSelection(coupon)"
          >
            <XIcon :size="12" />
          </BaseButton>
        </div>

        <!-- 더 많은 쿠폰이 있을 때 표시 -->
        <div v-if="selectedCoupons.length > maxDisplay" class="more-indicator">
          +{{ selectedCoupons.length - maxDisplay }}개 더
        </div>
      </div>

      <!-- 전체 해제 버튼 -->
      <BaseButton
        v-if="!readonly && multiple && selectedCoupons.length > 1"
        type="ghost"
        size="xs"
        class="clear-all"
        @click="clearSelection"
      >
        전체 해제
      </BaseButton>
    </div>

    <!-- 빈 상태 표시 -->
    <div v-else class="empty-display">
      <span class="empty-text">{{ placeholder || '쿠폰을 선택하세요' }}</span>
    </div>

    <!-- 쿠폰 선택 모달 -->
    <BaseWindow v-model="showSelectorModal" title="쿠폰 선택" width="700px" :min-height="'450px'">
      <CouponSelectorModal
        :selected-coupons="selectedCoupons"
        :multiple="multiple"
        :filter-options="filterOptions"
        :exclude-ids="excludeIds"
        @select="handleCouponSelect"
        @create="openCreateModal"
        @close="closeSelectorModal"
      />
    </BaseWindow>

    <!-- 쿠폰 생성 모달 -->
    <BaseWindow v-model="showCreateModal" title="쿠폰 생성" width="600px" :min-height="'580px'">
      <CouponForm @save="handleCreateCoupon" @cancel="closeCreateModal" />
    </BaseWindow>
  </div>
</template>

<script>
  import { ref, computed, watch } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseWindow from '@/components/common/BaseWindow.vue';
  import XIcon from '@/components/icons/XIcon.vue';
  import TicketIcon from '@/components/icons/TicketIcon.vue';
  import CouponSelectorModal from './CouponSelectorModal.vue';
  import CouponForm from './CouponForm.vue';

  export default {
    name: 'CompactCouponSelector',
    components: {
      BaseButton,
      BaseWindow,
      XIcon,
      TicketIcon,
      CouponSelectorModal,
      CouponForm,
    },
    props: {
      // 선택된 쿠폰들 (v-model) - 다중 선택 시 Array, 단일 선택 시 Object 또는 null
      modelValue: {
        validator: value => {
          return value === null || Array.isArray(value) || typeof value === 'object';
        },
        default: null,
      },
      // 라벨 텍스트
      label: {
        type: String,
        default: '쿠폰',
      },
      // 플레이스홀더 텍스트
      placeholder: {
        type: String,
        default: '',
      },
      // 다중 선택 허용 여부
      multiple: {
        type: Boolean,
        default: false,
      },
      // 쿠폰 생성 버튼 표시 여부
      allowCreate: {
        type: Boolean,
        default: true,
      },
      // 읽기 전용 모드
      readonly: {
        type: Boolean,
        default: false,
      },
      // 필터 옵션
      filterOptions: {
        type: Object,
        default: () => ({}),
      },
      // 제외할 쿠폰 ID 목록
      excludeIds: {
        type: Array,
        default: () => [],
      },
      // 최대 표시 개수
      maxDisplay: {
        type: Number,
        default: 3,
      },
    },
    emits: ['update:modelValue', 'coupon-selected', 'coupon-created', 'selection-changed'],
    setup(props, { emit }) {
      // State
      const showSelectorModal = ref(false);
      const showCreateModal = ref(false);

      // Computed
      const selectedCoupons = computed({
        get: () => {
          if (props.multiple) {
            return Array.isArray(props.modelValue) ? props.modelValue : [];
          } else {
            return props.modelValue ? [props.modelValue] : [];
          }
        },
        set: value => {
          const emitValue = props.multiple ? value : value.length > 0 ? value[0] : null;
          emit('update:modelValue', emitValue);
          emit('selection-changed', emitValue);
        },
      });

      const displayCoupons = computed(() => {
        return selectedCoupons.value.slice(0, props.maxDisplay);
      });

      // Watch for external changes - simplified to avoid infinite loops
      watch(
        () => props.modelValue,
        newValue => {},
        { immediate: true }
      );

      // Methods
      const openSelector = () => {
        if (!props.readonly) {
          showSelectorModal.value = true;
        }
      };

      const closeSelectorModal = () => {
        showSelectorModal.value = false;
      };

      const openCreateModal = () => {
        showCreateModal.value = true;
      };

      const closeCreateModal = () => {
        showCreateModal.value = false;
      };

      const handleCouponSelect = coupons => {
        const couponsArray = Array.isArray(coupons) ? coupons : [coupons];
        selectedCoupons.value = couponsArray;

        const emitValue = props.multiple
          ? couponsArray
          : couponsArray.length > 0
            ? couponsArray[0]
            : null;
        emit('coupon-selected', emitValue);
        closeSelectorModal();
      };

      const handleCreateCoupon = couponData => {
        emit('coupon-created', couponData);
        closeCreateModal();
      };

      const removeFromSelection = coupon => {
        if (props.readonly) return;

        if (props.multiple) {
          selectedCoupons.value = selectedCoupons.value.filter(c => c.id !== coupon.id);
        } else {
          selectedCoupons.value = [];
        }
      };

      const clearSelection = () => {
        if (props.readonly) return;
        selectedCoupons.value = [];
      };

      return {
        // State
        showSelectorModal,
        showCreateModal,
        selectedCoupons,
        displayCoupons,

        // Methods
        openSelector,
        closeSelectorModal,
        openCreateModal,
        closeCreateModal,
        handleCouponSelect,
        handleCreateCoupon,
        removeFromSelection,
        clearSelection,
      };
    },
  };
</script>

<style scoped>
  .compact-coupon-selector {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  /* 라벨과 버튼 행 */
  .selector-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
  }

  .selector-label {
    font-size: 13px;
    font-weight: 600;
    color: var(--color-gray-700);
    flex-shrink: 0;
    min-width: fit-content;
  }

  .selector-buttons {
    display: flex;
    gap: 0.5rem;
    align-items: center;
    flex-shrink: 0;
  }

  .select-button {
    display: flex;
    align-items: center;
    gap: 0.375rem;
    min-width: 60px;
    justify-content: center;
  }

  .create-button {
    min-width: 32px;
    padding: 0.375rem;
    justify-content: center;
  }

  /* 선택된 쿠폰 표시 영역 */
  .selected-display {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    padding: 0.75rem;
    background-color: var(--color-gray-50);
    border: 1px solid var(--color-gray-200);
    border-radius: 6px;
  }

  .selected-items {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    align-items: center;
  }

  .selected-chip {
    display: flex;
    align-items: center;
    gap: 0.375rem;
    padding: 0.375rem 0.5rem;
    background-color: white;
    border: 1px solid var(--color-primary-200);
    border-radius: 16px;
    font-size: 12px;
    color: var(--color-primary-600);
    max-width: 200px;
  }

  .chip-content {
    display: flex;
    align-items: center;
    gap: 0.25rem;
    overflow: hidden;
    flex: 1;
    min-width: 0;
  }

  .chip-name {
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .chip-discount {
    font-weight: 600;
    color: var(--color-primary-600);
    flex-shrink: 0;
  }

  .chip-remove {
    padding: 0.125rem;
    min-width: unset;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    color: var(--color-gray-500);
    background-color: transparent;
  }

  .chip-remove:hover {
    background-color: var(--color-error-50);
    color: var(--color-error-500);
  }

  .more-indicator {
    padding: 0.375rem 0.5rem;
    background-color: var(--color-gray-100);
    border-radius: 16px;
    font-size: 11px;
    font-weight: 500;
    color: var(--color-gray-600);
  }

  .clear-all {
    align-self: flex-start;
    font-size: 11px;
    color: var(--color-gray-500);
    text-decoration: underline;
  }

  /* 빈 상태 */
  .empty-display {
    padding: 0.5rem 0.75rem;
    border: 1px dashed var(--color-gray-300);
    border-radius: 6px;
    background-color: var(--color-gray-25);
  }

  .empty-text {
    font-size: 12px;
    color: var(--color-gray-500);
    font-style: italic;
  }

  /* 반응형 */
  @media (max-width: 768px) {
    .selector-row {
      flex-direction: column;
      align-items: flex-start;
      gap: 0.5rem;
    }

    .selector-buttons {
      width: 100%;
      justify-content: flex-end;
    }

    .selected-items {
      flex-direction: column;
      align-items: stretch;
    }

    .selected-chip {
      max-width: none;
    }
  }

  /* 호버 효과 */
  .selected-chip:hover {
    border-color: var(--color-primary-300);
    background-color: var(--color-primary-50);
  }

  /* 포커스 상태 */
  .select-button:focus,
  .create-button:focus {
    outline: 2px solid var(--color-primary-200);
    outline-offset: 1px;
  }
</style>
