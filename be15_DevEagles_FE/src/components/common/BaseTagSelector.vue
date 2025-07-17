<template>
  <div class="tag-selector-wrapper">
    <label v-if="label" class="form-label">{{ label }}</label>
    <Multiselect
      v-model="selectedTags"
      :options="tagOptions"
      mode="tags"
      :close-on-select="false"
      :searchable="true"
      :create-option="false"
      label="tagName"
      :value-prop="valueProp"
      track-by="tagName"
      :placeholder="placeholder"
      :class="['multiselect-custom', `multiselect-${size}`]"
      @change="handleChange"
    />
    <div v-if="error" class="error-message">{{ error }}</div>
  </div>
</template>

<script>
  import Multiselect from '@vueform/multiselect';
  import '@vueform/multiselect/themes/default.css';

  export default {
    name: 'BaseTagSelector',
    components: {
      Multiselect,
    },
    props: {
      modelValue: {
        type: Array,
        default: () => [],
      },
      label: {
        type: String,
        default: '',
      },
      placeholder: {
        type: String,
        default: '태그 선택',
      },
      error: {
        type: String,
        default: '',
      },
      size: {
        type: String,
        default: 'md',
        validator: value => ['sm', 'md', 'lg'].includes(value),
      },
      options: {
        type: Array,
        default: () => [
          { tagName: 'VIP', colorCode: '#FFD700' },
          { tagName: '신규', colorCode: '#00BFFF' },
          { tagName: '단골', colorCode: '#90ee90' },
          { tagName: 'VVIP', colorCode: '#FF69B4' },
          { tagName: '재방문', colorCode: '#FFB347' },
          { tagName: '이벤트', colorCode: '#B0E0E6' },
        ],
      },
      valueProp: {
        type: String,
        default: 'tagName',
      },
    },
    emits: ['update:modelValue'],
    computed: {
      tagOptions() {
        return this.options;
      },
      selectedTags: {
        get() {
          return this.modelValue;
        },
        set(value) {
          this.$emit('update:modelValue', value);
        },
      },
    },
    methods: {
      handleChange(value) {
        this.$emit('update:modelValue', value);
      },
    },
  };
</script>

<style>
  .multiselect-custom {
    --ms-border-color: #e5e7eb;
    --ms-radius: 8px;
    --ms-ring-width: 0px;
    --ms-ring-color: transparent;
    --ms-tag-bg: #e8ecf1;
    --ms-tag-color: #364f6b;
    --ms-tag-radius: 14px;
    --ms-border-color-active: #364f6b;
    --ms-border-color-focus: #364f6b;
    --ms-bg: #ffffff;
    --ms-color: #1f1f1f;
    --ms-option-font-size: 13px;
    --ms-option-line-height: 1.4;
    --ms-option-py: 6px;
    --ms-option-px: 10px;
  }

  /* Size variants */
  .multiselect-sm {
    --ms-font-size: 12px;
    --ms-line-height: 1.3;
    --ms-py: 6px;
    --ms-px: 10px;
    --ms-tag-font-size: 10px;
    --ms-tag-py: 2px;
    --ms-tag-px: 6px;
    --ms-option-font-size: 11px;
    --ms-option-py: 4px;
    --ms-option-px: 8px;
  }

  .multiselect-md {
    --ms-font-size: 14px;
    --ms-line-height: 1.5;
    --ms-py: 8px;
    --ms-px: 12px;
    --ms-tag-font-size: 12px;
    --ms-tag-py: 3px;
    --ms-tag-px: 8px;
    --ms-option-font-size: 13px;
    --ms-option-py: 6px;
    --ms-option-px: 10px;
  }

  .multiselect-lg {
    --ms-font-size: 16px;
    --ms-line-height: 1.5;
    --ms-py: 12px;
    --ms-px: 16px;
    --ms-tag-font-size: 14px;
    --ms-tag-py: 4px;
    --ms-tag-px: 10px;
    --ms-option-font-size: 15px;
    --ms-option-py: 8px;
    --ms-option-px: 12px;
  }
</style>

<style scoped>
  .tag-selector-wrapper {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .form-label {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-neutral-dark);
  }

  .error-message {
    color: var(--color-error-300);
    font-size: 12px;
    line-height: 15.6px;
    margin-top: 2px;
  }
</style>
