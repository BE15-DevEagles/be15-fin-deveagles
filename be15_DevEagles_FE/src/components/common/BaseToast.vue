<template>
  <Teleport to="body">
    <transition-group name="toast" tag="div" class="toast-container" :class="`toast-${position}`">
      <div v-for="toast in toasts" :key="toast.id" :class="['toast-item', `toast-${toast.type}`]">
        <!-- Toast Icon -->
        <div v-if="toast.showIcon" class="toast-icon">
          <component :is="getIcon(toast.type)" :size="20" />
        </div>

        <!-- Toast Content -->
        <div class="toast-content">
          <div v-if="toast.title" class="toast-title">{{ toast.title }}</div>
          <div class="toast-message">{{ toast.message }}</div>
        </div>

        <!-- Close Button -->
        <button v-if="toast.closable" class="toast-close" @click="removeToast(toast.id)">
          &times;
        </button>
      </div>
    </transition-group>
  </Teleport>
</template>

<script>
  export default {
    name: 'BaseToast',
    props: {
      position: {
        type: String,
        default: 'top-right',
        validator: value =>
          [
            'top-left',
            'top-center',
            'top-right',
            'bottom-left',
            'bottom-center',
            'bottom-right',
          ].includes(value),
      },
    },
    data() {
      return {
        toasts: [],
        nextId: 1,
      };
    },
    methods: {
      addToast({
        type = 'info',
        title = '',
        message = '',
        duration = 4000,
        closable = true,
        showIcon = true,
      }) {
        const toast = {
          id: this.nextId++,
          type,
          title,
          message,
          closable,
          showIcon,
        };

        this.toasts.push(toast);

        if (duration > 0) {
          setTimeout(() => {
            this.removeToast(toast.id);
          }, duration);
        }

        return toast.id;
      },

      removeToast(id) {
        const index = this.toasts.findIndex(toast => toast.id === id);
        if (index > -1) {
          this.toasts.splice(index, 1);
        }
      },

      clearAll() {
        this.toasts = [];
      },

      getIcon(type) {
        const iconMap = {
          success: 'CheckCircleIcon',
          error: 'XCircleIcon',
          warning: 'AlertTriangleIcon',
          info: 'InfoIcon',
        };
        return iconMap[type] || 'InfoIcon';
      },

      // 편의 메서드들
      success(message, options = {}) {
        return this.addToast({ ...options, type: 'success', message });
      },

      error(message, options = {}) {
        return this.addToast({ ...options, type: 'error', message });
      },

      warning(message, options = {}) {
        return this.addToast({ ...options, type: 'warning', message });
      },

      info(message, options = {}) {
        return this.addToast({ ...options, type: 'info', message });
      },
    },
  };
</script>

<style scoped>
  .toast-container {
    position: fixed;
    z-index: 10000;
    pointer-events: none;
  }

  .toast-top-left {
    top: 1rem;
    left: 1rem;
  }

  .toast-top-center {
    top: 1rem;
    left: 50%;
    transform: translateX(-50%);
  }

  .toast-top-right {
    top: 1rem;
    right: 1rem;
  }

  .toast-bottom-left {
    bottom: 1rem;
    left: 1rem;
  }

  .toast-bottom-center {
    bottom: 1rem;
    left: 50%;
    transform: translateX(-50%);
  }

  .toast-bottom-right {
    bottom: 1rem;
    right: 1rem;
  }

  .toast-item {
    display: flex;
    align-items: flex-start;
    gap: 0.75rem;
    padding: 1rem 1.25rem;
    margin-bottom: 0.5rem;
    border-radius: 0.5rem;
    box-shadow: 0 8px 40px -10px rgba(0, 0, 0, 0.15);
    pointer-events: auto;
    min-width: 300px;
    max-width: 500px;
  }

  .toast-success {
    background: var(--color-success-50);
    border-left: 4px solid var(--color-success-500);
    color: var(--color-success-600);
  }

  .toast-error {
    background: var(--color-error-50);
    border-left: 4px solid var(--color-error-300);
    color: var(--color-error-600);
  }

  .toast-warning {
    background: var(--color-warning-50);
    border-left: 4px solid var(--color-warning-300);
    color: var(--color-warning-600);
  }

  .toast-info {
    background: var(--color-info-50);
    border-left: 4px solid var(--color-info-300);
    color: var(--color-info-500);
  }

  .toast-icon {
    flex-shrink: 0;
    margin-top: 0.125rem;
  }

  .toast-content {
    flex: 1;
  }

  .toast-title {
    font-weight: 600;
    margin-bottom: 0.25rem;
    font-size: 14px;
  }

  .toast-message {
    font-size: 14px;
    line-height: 1.5;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 100%;
  }

  .toast-close {
    flex-shrink: 0;
    background: none;
    border: none;
    font-size: 1.25rem;
    line-height: 1;
    color: currentColor;
    opacity: 0.7;
    cursor: pointer;
    padding: 0;
    margin-left: 0.5rem;
  }

  .toast-close:hover {
    opacity: 1;
  }

  /* Transitions */
  .toast-enter-active,
  .toast-leave-active {
    transition: all 0.3s ease;
  }

  .toast-enter-from {
    opacity: 0;
    transform: translateX(100%);
  }

  .toast-leave-to {
    opacity: 0;
    transform: translateX(100%);
  }

  .toast-move {
    transition: transform 0.3s ease;
  }

  @media (max-width: 768px) {
    .toast-container {
      left: 1rem !important;
      right: 1rem !important;
      transform: none !important;
    }

    .toast-item {
      min-width: auto;
      max-width: none;
    }
  }
</style>
