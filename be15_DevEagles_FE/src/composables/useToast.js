// 1. 단순한 이벤트 버스(Event Bus) 구현
// 앱 전체에서 'show-toast' 이벤트를 주고받기 위한 통신 채널입니다.
const events = new Map();

export const toastBus = {
  on(event, callback) {
    if (!events.has(event)) {
      events.set(event, []);
    }
    events.get(event).push(callback);
  },
  off(event, callback) {
    if (events.has(event)) {
      const callbacks = events.get(event).filter(cb => cb !== callback);
      events.set(event, callbacks);
    }
  },
  emit(event, data) {
    if (events.has(event)) {
      events.get(event).forEach(callback => callback(data));
    }
  },
};

// 2. useToast 훅 (이제 글로벌 이벤트를 발생시키는 역할)
export function useToast() {
  const showToast = (message, type = 'success', options = {}) => {
    toastBus.emit('show-toast', { message, type, options });
  };

  // 기존 코드와의 호환성을 위해 showSuccess, showError 등은 그대로 유지합니다.
  const showSuccess = (message, options) => showToast(message, 'success', options);
  const showError = (message, options) => showToast(message, 'error', options);
  const showWarning = (message, options) => showToast(message, 'warning', options);
  const showInfo = (message, options) => showToast(message, 'info', options);

  return { showToast, showSuccess, showError, showWarning, showInfo };
}
