<script setup>
  import { ref, watch, computed, nextTick, onMounted, onBeforeUnmount } from 'vue';
  import BaseModal from '@/components/common/BaseModal.vue';
  import BaseButton from '@/components/common/BaseButton.vue';

  const props = defineProps({
    modelValue: Boolean,
    templateContent: {
      type: Object,
      default: () => ({ content: '', link: '', coupon: null, grades: [], tags: [] }),
    },
    customers: {
      type: Array,
      default: () => [],
    },
    availableCoupons: {
      type: Array,
      default: () => [],
    },
    grades: {
      type: Array,
      default: () => [],
    },
    tags: {
      type: Array,
      default: () => [],
    },
  });

  const emit = defineEmits([
    'update:modelValue',
    'request-send',
    'request-reserve',
    'open-template',
    'open-customer',
  ]);

  const messageContent = ref('');
  const selectedGrade = ref([]);
  const selectedTags = ref([]);
  const linkUrl = ref('');
  const selectedCoupon = ref(null);
  const showVariableDropdown = ref(false);
  const textareaWrapper = ref(null);
  const variableButtonRef = ref(null);
  const selectedKind = ref('advertising');

  const variables = [
    '#{고객명}',
    '#{예약날짜}',
    '#{횟수권잔여횟수}',
    '#{선불권잔여금액}',
    '#{프로필링크}',
    '#{인스타url}',
    '#{상점명}',
  ];

  watch(
    () => props.templateContent,
    newVal => {
      if (!newVal) return;
      messageContent.value = newVal.content || '';
      linkUrl.value = newVal.link || '';
      selectedCoupon.value = newVal.coupon || null;
      selectedGrade.value = newVal.grades || [];
      selectedTags.value = newVal.tags || [];
    },
    { immediate: true }
  );

  watch(selectedCoupon, (newCoupon, oldCoupon) => {
    if (!newCoupon || !newCoupon.couponTitle || newCoupon === oldCoupon) return;

    const regex = /\[쿠폰\].*?\(\s*할인율:\s*\d+%\)(\n코드:\s*\w+)?/g;
    messageContent.value = messageContent.value.replace(regex, '').trim();

    const couponText = `\n[쿠폰] ${newCoupon.couponTitle} (할인율: ${newCoupon.discountRate}%)\n코드: ${newCoupon.couponCode}`;
    messageContent.value = (messageContent.value + couponText).trim();
  });

  function insertVariable(variable) {
    if (variable === '#{인스타url}') {
      linkUrl.value = variable;
      return;
    }

    const textarea = textareaWrapper.value?.querySelector('textarea');
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const before = messageContent.value.slice(0, start);
    const after = messageContent.value.slice(end);

    messageContent.value = before + variable + after;

    nextTick(() => {
      textarea.focus();
      textarea.selectionStart = textarea.selectionEnd = start + variable.length;
    });
  }

  function handleClickOutside(e) {
    const btn = variableButtonRef.value?.$el ?? null;
    const dropdown = document.querySelector('.variable-insert-wrap .dropdown');

    if (
      showVariableDropdown.value &&
      btn &&
      !btn.contains(e.target) &&
      !dropdown?.contains(e.target)
    ) {
      showVariableDropdown.value = false;
    }
  }

  onMounted(() => {
    window.addEventListener('click', handleClickOutside);
  });
  onBeforeUnmount(() => {
    window.removeEventListener('click', handleClickOutside);
  });

  const visibleCustomers = computed(() => (props.customers || []).slice(0, 5));
  const hiddenCustomerCount = computed(() => Math.max((props.customers?.length || 0) - 5, 0));

  function toggleGrade(grade) {
    if (grade === '전체') {
      selectedGrade.value = selectedGrade.value.includes('전체') ? [] : ['전체'];
    } else {
      if (selectedGrade.value.includes('전체')) {
        selectedGrade.value = [grade];
      } else if (selectedGrade.value.includes(grade)) {
        selectedGrade.value = selectedGrade.value.filter(g => g !== grade);
      } else {
        selectedGrade.value.push(grade);
      }
    }
  }

  function close() {
    messageContent.value = '';
    selectedGrade.value = [];
    selectedTags.value = [];
    linkUrl.value = '';
    selectedCoupon.value = null;
    showVariableDropdown.value = false;

    emit('update:modelValue', false);
  }

  function confirmSend(type) {
    if (!messageContent.value.trim()) return;

    const payload = {
      messageContent: messageContent.value,
      messageType: 'SMS',
      link: linkUrl.value || null,
      coupon: selectedCoupon.value,
      grades: selectedGrade.value,
      tags: selectedTags.value,
      customerIds: props.customers.map(c => c.id),
      messageKind: selectedKind.value,
    };

    if (type === 'IMMEDIATE') {
      emit('request-send', { ...payload, messageSendingType: 'IMMEDIATE' });
    } else {
      emit('request-reserve', { ...payload, messageSendingType: 'RESERVATION' });
    }

    close();
  }
</script>

<template>
  <BaseModal :model-value="modelValue" title="메시지 보내기" @update:model-value="close">
    <div class="form-group">
      <label for="message-kind">메시지 종류</label>
      <select id="message-kind" v-model="selectedKind" class="form-control">
        <option value="advertising">광고</option>
        <option value="announcement">공지</option>
        <option value="etc">기타</option>
      </select>
    </div>

    <div class="template-button">
      <BaseButton size="sm" @click="$emit('open-template')">템플릿 선택</BaseButton>
    </div>

    <div class="label-with-button">
      <label>고객</label>
      <BaseButton size="sm" @click="$emit('open-customer')">고객 선택하기</BaseButton>
    </div>
    <div class="selected-chip-wrap">
      <span v-for="customer in visibleCustomers" :key="customer.id" class="customer-chip">
        {{ customer.name }}
      </span>
      <span v-if="hiddenCustomerCount > 0" class="customer-chip more-chip">
        +{{ hiddenCustomerCount }}
      </span>
    </div>

    <div class="label-with-button">
      <label>등급 선택</label>
    </div>
    <div class="selected-chip-wrap">
      <span
        v-for="grade in props.grades"
        :key="grade.id"
        class="customer-chip"
        :class="{ 'more-chip': selectedGrade.includes(grade.name) }"
        @click="toggleGrade(grade.name)"
      >
        {{ grade.name }}
      </span>
    </div>

    <div class="label-with-button">
      <label>태그 선택</label>
    </div>
    <div class="selected-chip-wrap">
      <span
        v-for="tag in props.tags"
        :key="tag.tagId"
        class="customer-chip"
        :class="{ 'more-chip': selectedTags.includes(tag.tagName) }"
        @click="
          selectedTags.includes(tag.tagName)
            ? (selectedTags = selectedTags.filter(t => t !== tag.tagName))
            : selectedTags.push(tag.tagName)
        "
      >
        {{ tag.tagName }}
      </span>
    </div>

    <div class="label-with-button">
      <label>쿠폰 선택</label>
    </div>
    <select v-model="selectedCoupon" class="input input--md">
      <option disabled value="">쿠폰을 선택하세요</option>
      <option v-for="coupon in props.availableCoupons" :key="coupon.id" :value="coupon">
        {{ coupon.couponTitle }}
      </option>
    </select>

    <div class="label-with-button">
      <label>내용 작성</label>
      <BaseButton
        ref="variableButtonRef"
        size="sm"
        @click="showVariableDropdown = !showVariableDropdown"
      >
        변수 추가
      </BaseButton>
    </div>
    <div class="textarea-variable-wrapper">
      <div ref="textareaWrapper" class="textarea-container">
        <textarea
          v-model="messageContent"
          class="input input--md w-full"
          rows="6"
          placeholder="메시지를 입력하세요"
        ></textarea>
      </div>
      <div v-if="showVariableDropdown" class="variable-insert-wrap">
        <div class="dropdown">
          <div
            v-for="item in variables"
            :key="item"
            class="insert-item"
            @click="insertVariable(item)"
          >
            {{ item }}
          </div>
        </div>
      </div>
    </div>

    <div class="footer-buttons">
      <BaseButton type="primary" @click="confirmSend('IMMEDIATE')">바로 발송</BaseButton>
      <BaseButton type="secondary" @click="confirmSend('RESERVATION')">예약 발송</BaseButton>
    </div>
  </BaseModal>
</template>

<style scoped>
  .insert-item {
    padding: 4px 8px;
    font-size: 13px;
    cursor: pointer;
    border-radius: 4px;
    transition: background-color 0.2s ease;
  }
  .insert-item:hover {
    background-color: var(--color-gray-100);
  }
  .selected-chip-wrap {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 6px;
  }
  .customer-chip {
    background-color: var(--color-gray-100);
    color: var(--color-gray-800);
    font-size: 13px;
    padding: 4px 8px;
    border-radius: 12px;
  }
  .more-chip {
    background-color: var(--color-primary-main);
    color: var(--color-primary-100);
  }
  .footer-buttons {
    display: flex;
    justify-content: center;
    gap: 12px;
    margin-top: 12px;
  }
  .template-button {
    margin-bottom: 16px;
  }
  .label-with-button {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
  }
  .textarea-variable-wrapper {
    display: flex;
    gap: 8px;
    position: relative;
  }
  .textarea-container {
    flex: 1;
  }
  .variable-insert-wrap {
    position: relative;
  }
  .variable-insert-wrap .dropdown {
    position: absolute;
    right: 0;
    background-color: white;
    border: 1px solid var(--color-gray-200);
    border-radius: 6px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    padding: 6px;
    width: 160px;
    z-index: 10;
  }
</style>
