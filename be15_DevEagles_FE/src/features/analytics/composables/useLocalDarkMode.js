import { ref, nextTick } from 'vue';

// 전역 상태 (analytics 페이지 내에서만 상태 공유)
const isDarkMode = ref(false);
const isTransitioning = ref(false);

export function useLocalDarkMode() {
  const toggleDarkMode = async () => {
    isTransitioning.value = true;

    await new Promise(resolve => setTimeout(resolve, 50));

    isDarkMode.value = !isDarkMode.value;

    localStorage.setItem('analytics-darkMode', isDarkMode.value.toString());

    await nextTick();

    setTimeout(() => {
      isTransitioning.value = false;
    }, 300);
  };

  const setDarkMode = enabled => {
    isDarkMode.value = enabled;
    localStorage.setItem('analytics-darkMode', enabled.toString());
  };

  const initializeLocalDarkMode = () => {
    const savedDarkMode = localStorage.getItem('analytics-darkMode');
    if (savedDarkMode === 'true') {
      setDarkMode(true);
    } else if (savedDarkMode === 'false') {
      setDarkMode(false);
    } else {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      setDarkMode(prefersDark);
    }
  };

  return {
    isDarkMode,
    isTransitioning,

    toggleDarkMode,
    setDarkMode,
    initializeLocalDarkMode,
  };
}
