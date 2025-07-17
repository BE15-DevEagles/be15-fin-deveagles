<template>
  <div class="coupon-selector-modal">
    <!-- 검색 및 생성 -->
    <div class="search-section">
      <input
        v-model="searchText"
        type="text"
        placeholder="쿠폰명으로 검색..."
        class="search-input"
      />
      <BaseButton type="primary" outline @click="handleCreate">
        <PlusIcon :size="16" />
        새 쿠폰 생성
      </BaseButton>
    </div>

    <!-- 선택 정보 -->
    <div v-if="multiple && selectedCoupons.length > 0" class="selection-info">
      <span class="selection-count">{{ selectedCoupons.length }}개 선택됨</span>
      <BaseButton type="success" size="sm" @click="clearSelection"> 선택 해제 </BaseButton>
    </div>

    <!-- 쿠폰 목록 -->
    <div class="coupon-list">
      <div
        v-for="coupon in filteredCoupons"
        :key="coupon.id"
        class="coupon-item"
        :class="{
          selected: isSelected(coupon),
          disabled: isDisabled(coupon),
        }"
        @click="handleCouponClick(coupon)"
      >
        <div v-if="multiple" class="coupon-checkbox">
          <input
            type="checkbox"
            :checked="isSelected(coupon)"
            :disabled="isDisabled(coupon)"
            @click.stop
            @change="handleCheckboxChange(coupon, $event)"
          />
        </div>

        <div class="coupon-content">
          <div class="coupon-header">
            <div class="coupon-name">{{ coupon.couponTitle }}</div>
            <div class="coupon-badges">
              <BaseBadge :type="coupon.isActive ? 'success' : 'warning'" size="sm">
                {{ coupon.isActive ? '활성' : '비활성' }}
              </BaseBadge>
              <BaseBadge type="primary" size="sm"> {{ coupon.discountRate }}% </BaseBadge>
            </div>
          </div>

          <div class="coupon-details">
            <div class="detail-item">
              <span class="detail-label">상품:</span>
              <span class="detail-value">
                {{ coupon.primaryItemInfo?.name }}
                <span v-if="coupon.primaryItemInfo?.name && coupon.secondaryItemInfo?.name"
                  >,
                </span>
                {{ coupon.secondaryItemInfo?.name }}
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">카테고리:</span>
              <span class="detail-value">{{ coupon.primaryItemInfo?.category }}</span>
            </div>
            <div v-if="coupon.designerInfo?.staffName" class="detail-item">
              <span class="detail-label">디자이너:</span>
              <span class="detail-value">{{ coupon.designerInfo?.staffName }}</span>
            </div>
            <div v-if="coupon.expirationDate" class="detail-item">
              <span class="detail-label">만료일:</span>
              <span class="detail-value">{{ formatDate(coupon.expirationDate) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="filteredCoupons.length === 0" class="empty-state">
        <p class="empty-message">조건에 맞는 쿠폰이 없습니다.</p>
        <BaseButton type="primary" @click="handleCreate"> 새 쿠폰 생성하기 </BaseButton>
      </div>
    </div>
  </div>
</template>

<script>
  import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
  import { getLogger, getErrorLogger, getPerformanceLogger } from '@/plugins/LoggerManager.js';
  import { useToast } from '@/composables/useToast';
  import couponsAPI from '../api/coupons.js';
  import ApiErrorHandler from '../utils/ApiErrorHandler.js';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseBadge from '@/components/common/BaseBadge.vue';
  import PlusIcon from '@/components/icons/PlusIcon.vue';

  const logger = getLogger('CouponSelectorModal');
  const errorLogger = getErrorLogger();
  const perfLogger = getPerformanceLogger();

  export default {
    name: 'CouponSelectorModal',
    components: {
      BaseButton,
      BaseBadge,
      PlusIcon,
    },
    props: {
      selectedCoupons: {
        type: Array,
        default: () => [],
      },
      multiple: {
        type: Boolean,
        default: true,
      },
      filterOptions: {
        type: Object,
        default: () => ({}),
      },
      excludeIds: {
        type: Array,
        default: () => [],
      },
    },
    emits: ['select', 'create', 'close'],
    setup(props, { emit }) {
      const { showToast } = useToast();

      const executeWithErrorHandling = async (apiCall, context, successMessage = null) => {
        const startTime = performance.now();

        try {
          loading.value = true;
          logger.info(`${context} 시작`);

          const result = await apiCall();

          const duration = performance.now() - startTime;
          perfLogger.measure(context, Math.round(duration));
          logger.info(`${context} 완료`);

          if (successMessage) {
            showToast(successMessage, 'success');
          }

          return result;
        } catch (error) {
          const handledError = ApiErrorHandler.handleCouponError(error, context.toLowerCase());

          errorLogger.apiError(context, error, {
            duration: Math.round(performance.now() - startTime),
          });

          showToast(handledError.message, 'error');
          throw handledError;
        } finally {
          loading.value = false;
        }
      };

      // State
      const searchText = ref('');
      const localSelectedCoupons = ref([]);
      const coupons = ref([]);
      const loading = ref(false);

      // Computed
      const filteredCoupons = computed(() => {
        let filtered = coupons.value.filter(coupon => {
          // 제외할 ID 필터링
          if (props.excludeIds.includes(coupon.id)) return false;

          // 검색어 필터링
          if (
            searchText.value &&
            !coupon.couponTitle.toLowerCase().includes(searchText.value.toLowerCase())
          ) {
            return false;
          }

          return true;
        });

        // 추가 필터 옵션 적용
        if (props.filterOptions.onlyActive) {
          filtered = filtered.filter(coupon => coupon.isActive);
        }

        return filtered;
      });

      const canConfirm = computed(() => {
        return localSelectedCoupons.value.length > 0;
      });

      // Watch for external changes
      watch(
        () => props.selectedCoupons,
        newValue => {
          localSelectedCoupons.value = [...(newValue || [])];
        },
        { immediate: true }
      );

      // Methods
      const isSelected = coupon => {
        return localSelectedCoupons.value.some(c => c.id === coupon.id);
      };

      const isDisabled = coupon => {
        return !coupon.isActive && props.filterOptions.onlyActive;
      };

      const handleCouponClick = coupon => {
        if (isDisabled(coupon)) return;

        if (props.multiple) {
          toggleSelection(coupon);
        } else {
          localSelectedCoupons.value = [coupon];
          handleConfirm();
        }
      };

      const handleCheckboxChange = (coupon, event) => {
        if (event.target.checked) {
          addToSelection(coupon);
        } else {
          removeFromSelection(coupon);
        }
      };

      const toggleSelection = coupon => {
        if (isSelected(coupon)) {
          removeFromSelection(coupon);
        } else {
          addToSelection(coupon);
        }
      };

      const addToSelection = coupon => {
        if (!isSelected(coupon)) {
          localSelectedCoupons.value.push(coupon);
        }
      };

      const removeFromSelection = coupon => {
        const index = localSelectedCoupons.value.findIndex(c => c.id === coupon.id);
        if (index > -1) {
          localSelectedCoupons.value.splice(index, 1);
        }
      };

      const clearSelection = () => {
        localSelectedCoupons.value = [];
      };

      const resetFilters = () => {
        searchText.value = '';
      };

      const handleConfirm = () => {
        emit('select', props.multiple ? localSelectedCoupons.value : localSelectedCoupons.value[0]);
      };

      const handleCreate = () => {
        emit('create');
      };

      const handleClose = () => {
        emit('close');
      };

      const formatDate = dateString => {
        if (!dateString) return '';
        try {
          const date = new Date(dateString);
          return date.toLocaleDateString('ko-KR');
        } catch {
          return dateString;
        }
      };

      // API Methods
      const loadCoupons = async () => {
        const searchParams = {
          page: 0,
          size: 100, // 선택 모달에서는 많은 데이터 로드
          isActive: props.filterOptions.onlyActive ? true : undefined,
        };

        // shopId가 있으면 추가
        if (props.filterOptions.shopId) {
          searchParams.shopId = props.filterOptions.shopId;
        }

        console.log('CouponSelectorModal - loadCoupons searchParams:', searchParams);
        console.log('CouponSelectorModal - filterOptions:', props.filterOptions);

        const response = await executeWithErrorHandling(
          () => couponsAPI.getCoupons(searchParams),
          '쿠폰 선택 모달에서 쿠폰 목록 로드'
        );

        console.log('CouponSelectorModal - API response:', response);
        coupons.value = response.content;
        logger.info('쿠폰 목록 로드 완료', { count: response.content.length });
      };

      const handleKeyDown = event => {
        if (event.key === 'Escape') {
          handleClose();
        }
      };

      onMounted(() => {
        document.addEventListener('keydown', handleKeyDown);
        loadCoupons();
      });

      onUnmounted(() => {
        document.removeEventListener('keydown', handleKeyDown);
      });

      return {
        // State
        searchText,
        localSelectedCoupons,
        coupons,
        loading,

        // Computed
        filteredCoupons,
        canConfirm,

        // Methods
        isSelected,
        isDisabled,
        handleCouponClick,
        handleCheckboxChange,
        clearSelection,
        resetFilters,
        handleConfirm,
        handleCreate,
        handleClose,
        formatDate,
        loadCoupons,
      };
    },
  };
</script>

<style scoped>
  .coupon-selector-modal {
    display: flex;
    flex-direction: column;
    height: 100%;
    gap: 0.75rem;
  }

  .search-section {
    display: flex;
    gap: 0.75rem;
    align-items: center;
    padding-bottom: 0.75rem;
    border-bottom: 1px solid var(--color-gray-200);
  }

  .search-input {
    flex: 1;
    height: 38px;
    padding: 0.375rem 0.75rem;
    border: 1px solid var(--color-gray-300);
    border-radius: 6px;
    font-size: 14px;
    line-height: 1.5;
    background-color: white;
    box-sizing: border-box;
    transition:
      border-color 0.15s ease-in-out,
      box-shadow 0.15s ease-in-out;
  }

  .search-input:focus {
    outline: none;
    border-color: var(--color-primary-500);
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
  }

  .search-input::placeholder {
    color: var(--color-gray-500);
  }

  .selection-info {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.75rem;
    background-color: var(--color-primary-50);
    border-radius: 6px;
  }

  .selection-count {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-primary-600);
  }

  .coupon-list {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    max-height: 350px;
  }

  .coupon-item {
    display: flex;
    align-items: flex-start;
    gap: 0.75rem;
    padding: 0.75rem;
    border: 1px solid var(--color-gray-200);
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .coupon-item:hover:not(.disabled) {
    border-color: var(--color-primary-300);
    background-color: var(--color-primary-50);
  }

  .coupon-item.selected {
    border-color: var(--color-primary-500);
    background-color: var(--color-primary-50);
  }

  .coupon-item.disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .coupon-checkbox input[type='checkbox'] {
    margin: 0;
    accent-color: var(--color-primary-500);
  }

  .coupon-content {
    flex: 1;
    min-width: 0;
  }

  .coupon-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 0.375rem;
    gap: 1rem;
  }

  .coupon-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--color-gray-900);
    line-height: 1.3;
  }

  .coupon-badges {
    display: flex;
    gap: 0.375rem;
    flex-shrink: 0;
  }

  .coupon-details {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem 1rem;
  }

  .detail-item {
    display: flex;
    align-items: center;
    gap: 0.25rem;
    font-size: 12px;
  }

  .detail-label {
    color: var(--color-gray-500);
    font-weight: 500;
    flex-shrink: 0;
  }

  .detail-value {
    color: var(--color-gray-700);
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3rem 1rem;
    text-align: center;
  }

  .empty-message {
    color: var(--color-gray-500);
    margin-bottom: 1rem;
  }

  /* 반응형 */
  @media (max-width: 768px) {
    .search-row,
    .filter-row {
      flex-direction: column;
      align-items: stretch;
    }

    .coupon-details {
      grid-template-columns: 1fr;
    }

    .modal-actions {
      flex-direction: column;
    }
  }
</style>
