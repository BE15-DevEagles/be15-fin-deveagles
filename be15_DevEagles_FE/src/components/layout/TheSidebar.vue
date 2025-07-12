<template>
  <aside
    class="sidebar"
    :class="{
      collapsed: isCollapsed,
      'hover-expanded': isCollapsed && isHovered,
    }"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <div class="sidebar-header">
      <div class="logo">
        <div v-if="!isCollapsed || isHovered" class="logo-expanded">
          <img src="../../images/logo_negative.png" alt="Logo" class="logo-image" />
          <span class="logo-text">Beautifly</span>
        </div>
        <img
          v-else
          src="../../images/logo_negative.png"
          alt="Logo"
          class="logo-image logo-collapsed"
        />
      </div>
      <div v-if="!isCollapsed || isHovered" class="tooltip-container">
        <button class="icon-button dark" @click="toggleSidebar">
          <PinOffIcon v-if="!isCollapsed" :size="16" />
          <PinIcon v-else :size="16" />
        </button>
        <div class="tooltip tooltip-left">
          {{ !isCollapsed ? '고정 해제' : '메뉴 고정' }}
        </div>
      </div>
    </div>

    <nav class="sidebar-nav">
      <ul class="nav-list">
        <li class="nav-item">
          <router-link
            to="/"
            class="nav-link"
            :class="{ active: isActiveRoute('/') }"
            data-tooltip="홈"
          >
            <HomeIcon class="nav-icon" />
            <span v-if="!isCollapsed || isHovered" class="nav-text">홈</span>
          </router-link>
        </li>

        <li class="nav-item">
          <div class="nav-group">
            <button
              class="nav-link nav-toggle"
              :class="{ active: isCollapsed && isGroupActive('reservation') }"
              data-tooltip="예약"
              @click="toggleGroup('reservation')"
            >
              <CalendarIcon class="nav-icon" />
              <span v-if="!isCollapsed || isHovered" class="nav-text">예약</span>
              <ChevronRightIcon
                v-if="!isCollapsed || isHovered"
                class="nav-arrow"
                :class="{ expanded: activeGroups.includes('reservation') }"
              />
            </button>
            <transition name="submenu">
              <ul
                v-if="(!isCollapsed || isHovered) && activeGroups.includes('reservation')"
                class="nav-sublist"
              >
                <li>
                  <router-link to="/schedule/calendar" class="nav-sublink">캘린더</router-link>
                </li>
                <li>
                  <router-link to="/reservation/list" class="nav-sublink">예약 목록</router-link>
                </li>
                <li>
                  <router-link to="/schedule/plan" class="nav-sublink">일정 목록</router-link>
                </li>
                <!-- <li>
                <router-link to="/schedule/leave" class="nav-sublink">휴무 목록</router-link>
              </li> -->
                <li>
                  <router-link to="/reservation/requests" class="nav-sublink"
                    >예약 신청 목록</router-link
                  >
                </li>
                <li>
                  <router-link to="/reservation/history" class="nav-sublink"
                    >예약 변경 이력</router-link
                  >
                </li>
                <li>
                  <router-link to="/settings/reservation" class="nav-sublink"
                    >예약 설정</router-link
                  >
                </li>
              </ul>
            </transition>
          </div>
        </li>

        <li class="nav-item">
          <router-link
            to="/customer/list"
            class="nav-link"
            :class="{ active: isActiveRoute('/customer/list') }"
            data-tooltip="고객 관리"
          >
            <UsersIcon class="nav-icon" />
            <span v-if="!isCollapsed || isHovered" class="nav-text">고객 관리</span>
          </router-link>
        </li>

        <li class="nav-item">
          <div class="nav-group">
            <button
              class="nav-link nav-toggle"
              :class="{ active: isCollapsed && isGroupActive('sales') }"
              data-tooltip="매출 관리"
              @click="toggleGroup('sales')"
            >
              <DollarIcon class="nav-icon" />
              <span v-if="!isCollapsed || isHovered" class="nav-text">매출 관리</span>
              <ChevronRightIcon
                v-if="!isCollapsed || isHovered"
                class="nav-arrow"
                :class="{ expanded: activeGroups.includes('sales') }"
              />
            </button>
            <transition name="submenu">
              <ul
                v-if="(!isCollapsed || isHovered) && activeGroups.includes('sales')"
                class="nav-sublist"
              >
                <li>
                  <router-link to="/sales/management" class="nav-sublink">매출 관리</router-link>
                </li>
                <li><router-link to="/sales/staff" class="nav-sublink">직원결산</router-link></li>
              </ul>
            </transition>
          </div>
        </li>

        <li class="nav-item">
          <div class="nav-group">
            <button
              class="nav-link nav-toggle"
              :class="{ active: isCollapsed && isGroupActive('item') }"
              data-tooltip="상품 관리"
              @click="toggleGroup('item')"
            >
              <ShoppingBagIcon class="nav-icon" />
              <span v-if="!isCollapsed || isHovered" class="nav-text">상품 관리</span>
              <ChevronRightIcon
                v-if="!isCollapsed || isHovered"
                class="nav-arrow"
                :class="{ expanded: activeGroups.includes('item') }"
              />
            </button>
            <transition name="submenu">
              <ul
                v-if="(!isCollapsed || isHovered) && activeGroups.includes('item')"
                class="nav-sublist"
              >
                <li>
                  <router-link to="/item/service" class="nav-sublink">시술/상품 관리</router-link>
                </li>
                <li>
                  <router-link to="/item/membership" class="nav-sublink">회원권 관리</router-link>
                </li>
              </ul>
            </transition>
          </div>
        </li>

        <li class="nav-item">
          <div class="nav-group">
            <button
              class="nav-link nav-toggle"
              :class="{ active: isCollapsed && isGroupActive('analytics') }"
              data-tooltip="통계"
              @click="toggleGroup('analytics')"
            >
              <BarChartIcon class="nav-icon" />
              <span v-if="!isCollapsed || isHovered" class="nav-text">통계</span>
              <ChevronRightIcon
                v-if="!isCollapsed || isHovered"
                class="nav-arrow"
                :class="{ expanded: activeGroups.includes('analytics') }"
              />
            </button>
            <transition name="submenu">
              <ul
                v-if="(!isCollapsed || isHovered) && activeGroups.includes('analytics')"
                class="nav-sublist"
              >
                <li>
                  <router-link to="/analytics/usage" class="nav-sublink"
                    >고객 이용 통계</router-link
                  >
                </li>
                <li>
                  <router-link to="/analytics/sales" class="nav-sublink">매출 통계</router-link>
                </li>
              </ul>
            </transition>
          </div>
        </li>

        <li class="nav-item">
          <div class="nav-group">
            <button
              class="nav-link nav-toggle"
              :class="{ active: isCollapsed && isGroupActive('message') }"
              data-tooltip="문자"
              @click="toggleGroup('message')"
            >
              <MessageCircleIcon class="nav-icon" />
              <span v-if="!isCollapsed || isHovered" class="nav-text">문자</span>
              <ChevronRightIcon
                v-if="!isCollapsed || isHovered"
                class="nav-arrow"
                :class="{ expanded: activeGroups.includes('message') }"
              />
            </button>
            <transition name="submenu">
              <ul
                v-if="(!isCollapsed || isHovered) && activeGroups.includes('message')"
                class="nav-sublist"
              >
                <li>
                  <router-link to="/message/history" class="nav-sublink"
                    >보낸 메시지 목록</router-link
                  >
                </li>
                <li>
                  <router-link to="/message/templates" class="nav-sublink"
                    >템플릿 보관함</router-link
                  >
                </li>
                <li>
                  <router-link to="/message/settings" class="nav-sublink">메시지 설정</router-link>
                </li>
                <!-- todo 개발 후 주석 제거
                <li>
                  <router-link to="/message/ab-test" class="nav-sublink">A/B테스트</router-link>
                </li>-->
              </ul>
            </transition>
          </div>
        </li>

        <li class="nav-item">
          <div class="nav-group">
            <button
              class="nav-link nav-toggle"
              :class="{ active: isCollapsed && isGroupActive('marketing') }"
              data-tooltip="마케팅"
              @click="toggleGroup('marketing')"
            >
              <MegaphoneIcon class="nav-icon" />
              <span v-if="!isCollapsed || isHovered" class="nav-text">마케팅</span>
              <ChevronRightIcon
                v-if="!isCollapsed || isHovered"
                class="nav-arrow"
                :class="{ expanded: activeGroups.includes('marketing') }"
              />
            </button>
            <transition name="submenu">
              <ul
                v-if="(!isCollapsed || isHovered) && activeGroups.includes('marketing')"
                class="nav-sublist"
              >
                <li>
                  <router-link to="/workflows" class="nav-sublink">자동화</router-link>
                </li>
                <li>
                  <router-link to="/campaigns" class="nav-sublink">캠페인</router-link>
                </li>
                <li>
                  <router-link to="/coupons" class="nav-sublink">쿠폰 관리</router-link>
                </li>
              </ul>
            </transition>
          </div>
        </li>

        <li class="nav-item">
          <div class="nav-group">
            <button
              class="nav-link nav-toggle"
              :class="{ active: isCollapsed && isGroupActive('settings') }"
              data-tooltip="매장 설정"
              @click="toggleGroup('settings')"
            >
              <SettingsIcon class="nav-icon" />
              <span v-if="!isCollapsed || isHovered" class="nav-text">매장 설정</span>
              <ChevronRightIcon
                v-if="!isCollapsed || isHovered"
                class="nav-arrow"
                :class="{ expanded: activeGroups.includes('settings') }"
              />
            </button>
            <transition name="submenu">
              <ul
                v-if="(!isCollapsed || isHovered) && activeGroups.includes('settings')"
                class="nav-sublist"
              >
                <li>
                  <router-link to="/settings/store" class="nav-sublink">매장 기본 설정</router-link>
                </li>
                <li>
                  <router-link to="/settings/staff" class="nav-sublink">직원 관리</router-link>
                </li>
                <li>
                  <router-link to="/profile-link" class="nav-sublink">프로필 링크</router-link>
                </li>
              </ul>
            </transition>
          </div>
        </li>
      </ul>
    </nav>
  </aside>
</template>

<script setup>
  import { ref, watch, onMounted } from 'vue';
  import { useRoute } from 'vue-router';
  import {
    HomeIcon,
    CalendarIcon,
    UsersIcon,
    DollarIcon,
    ShoppingBagIcon,
    BarChartIcon,
    MessageCircleIcon,
    SettingsIcon,
    MegaphoneIcon,
    ChevronRightIcon,
    PinIcon,
    PinOffIcon,
  } from '../icons/index.js';

  const route = useRoute();

  const isCollapsed = ref(false);
  const isHovered = ref(false);
  const activeGroup = ref(null);

  const loadSidebarState = () => {
    try {
      const savedState = localStorage.getItem('sidebar-state');
      if (savedState) {
        const state = JSON.parse(savedState);
        isCollapsed.value = state.isCollapsed || false;
        activeGroup.value = state.activeGroup || null;
      }
    } catch (error) {
      console.warn('사이드바 상태 복원 실패:', error);
    }
  };

  const saveSidebarState = () => {
    try {
      const state = {
        isCollapsed: isCollapsed.value,
        activeGroup: activeGroup.value,
      };
      localStorage.setItem('sidebar-state', JSON.stringify(state));
    } catch (error) {
      console.warn('사이드바 상태 저장 실패:', error);
    }
  };

  const setActiveGroupByRoute = () => {
    const path = route.path;

    if (path.startsWith('/schedule/') || path.startsWith('/reservation/')) {
      activeGroup.value = 'reservation';
    } else if (path.startsWith('/sales/')) {
      activeGroup.value = 'sales';
    } else if (path.startsWith('/item/')) {
      activeGroup.value = 'item';
    } else if (path.startsWith('/analytics/')) {
      activeGroup.value = 'analytics';
    } else if (path.startsWith('/message/')) {
      activeGroup.value = 'message';
    } else if (
      path.startsWith('/workflows') ||
      path.startsWith('/campaigns') ||
      path.startsWith('/coupons')
    ) {
      activeGroup.value = 'marketing';
    } else if (path.startsWith('/settings/') || path.startsWith('/profile-link')) {
      activeGroup.value = 'settings';
    }
  };

  const toggleSidebar = () => {
    isCollapsed.value = !isCollapsed.value;
    if (isCollapsed.value) {
      activeGroup.value = null;
    }
    saveSidebarState();
  };

  const handleMouseEnter = () => {
    if (isCollapsed.value) {
      isHovered.value = true;
    }
  };

  const handleMouseLeave = () => {
    isHovered.value = false;
  };

  const toggleGroup = groupName => {
    if (isCollapsed.value && !isHovered.value) return;

    if (activeGroup.value === groupName) {
      activeGroup.value = null;
    } else {
      activeGroup.value = groupName;
    }
    saveSidebarState();
  };

  const isActiveRoute = path => {
    return route.path === path;
  };

  // 현재 라우트가 해당 그룹에 속하는지 확인
  const isGroupActive = groupName => {
    const path = route.path;

    switch (groupName) {
      case 'reservation':
        return path.startsWith('/schedule/') || path.startsWith('/reservation/');
      case 'sales':
        return path.startsWith('/sales/');
      case 'item':
        return path.startsWith('/item/');
      case 'analytics':
        return path.startsWith('/analytics/');
      case 'message':
        return path.startsWith('/message/');
      case 'marketing':
        return (
          path.startsWith('/workflows') ||
          path.startsWith('/campaigns') ||
          path.startsWith('/coupons')
        );
      case 'settings':
        return path.startsWith('/settings/') || path.startsWith('/profile-link');
      default:
        return false;
    }
  };

  const activeGroups = ref([]);
  watch(activeGroup, newValue => {
    activeGroups.value = newValue ? [newValue] : [];
  });

  const emit = defineEmits(['sidebar-toggle']);

  watch(isCollapsed, newValue => {
    emit('sidebar-toggle', newValue);
  });

  watch(
    () => route.path,
    () => {
      if (!activeGroup.value) {
        setActiveGroupByRoute();
        saveSidebarState();
      }
    }
  );

  onMounted(() => {
    loadSidebarState();
    if (!activeGroup.value) {
      setActiveGroupByRoute();
      saveSidebarState();
    }
  });
</script>

<style scoped>
  @import '@/assets/css/buttons.css';
  @import '@/assets/css/tooltip.css';
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    width: 200px;
    background-color: var(--color-primary-main);
    color: var(--color-neutral-white);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
    transition: width 300ms ease;
    z-index: 1000;
  }

  .sidebar.collapsed {
    width: 50px;
  }

  .sidebar.collapsed .sidebar-header {
    justify-content: center;
    padding: 1rem 0.5rem;
  }

  .sidebar.collapsed .logo {
    justify-content: center;
  }

  .sidebar.hover-expanded {
    width: 200px;
    box-shadow: 4px 0 12px rgba(0, 0, 0, 0.15);
  }

  .sidebar-header {
    padding: 1rem;
    height: 80px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    overflow: visible;
  }

  .logo {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .logo-expanded {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    height: 32px;
  }

  .logo-image {
    height: 24px;
    width: auto;
    object-fit: contain;
    flex-shrink: 0;
  }

  .logo-text {
    font-size: 16px;
    font-weight: 700;
    color: var(--color-neutral-white);
    letter-spacing: 0.5px;
    white-space: nowrap;
  }

  .logo-image.logo-collapsed {
    height: 24px;
    width: 24px;
    object-fit: contain;
  }

  .sidebar-nav {
    flex: 1;
    padding: 0.5rem 0;
    overflow-y: auto;
    overflow-x: hidden;
    min-height: 0;
  }

  .nav-list {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  .nav-item {
    margin-bottom: 1px;
  }

  .nav-link {
    display: flex;
    align-items: center;
    padding: 0.75rem 1rem;
    color: rgba(255, 255, 255, 0.8);
    text-decoration: none;
    transition: all 200ms ease;
    cursor: pointer;
    border: none;
    background: none;
    width: 100%;
    text-align: left;
    font-family: 'Noto Sans KR', sans-serif;
    font-size: 14px;
    position: relative;
  }

  .collapsed .nav-link {
    justify-content: center;
    padding: 0.75rem;
  }

  .hover-expanded .nav-link {
    justify-content: flex-start;
    padding: 0.75rem 1rem;
  }

  .nav-link:hover {
    background-color: rgba(255, 255, 255, 0.1);
    color: var(--color-neutral-white);
  }

  .nav-link.active {
    background-color: rgba(255, 255, 255, 0.15);
    color: var(--color-neutral-white);
    border-right: 3px solid var(--color-secondary-main);
  }

  .nav-icon {
    width: 20px;
    height: 20px;
    margin-right: 0.75rem;
    color: currentColor;
    flex-shrink: 0;
  }

  .collapsed .nav-icon {
    margin-right: 0;
  }

  .hover-expanded .nav-icon {
    margin-right: 0.75rem;
  }

  .nav-text {
    flex: 1;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
  }

  .nav-arrow {
    width: 16px;
    height: 16px;
    transition: transform 200ms ease;
    color: rgba(255, 255, 255, 0.6);
    margin-left: auto;
  }

  .nav-arrow.expanded {
    transform: rotate(90deg);
  }

  .nav-toggle {
    justify-content: flex-start;
  }

  .collapsed .nav-toggle {
    justify-content: center;
  }

  .hover-expanded .nav-toggle {
    justify-content: flex-start;
  }

  .nav-sublist {
    list-style: none;
    margin: 0;
    padding: 0;
    background-color: rgba(0, 0, 0, 0.15);
    border-left: 2px solid rgba(0, 0, 0, 0.1);
  }

  .nav-sublink {
    display: block;
    padding: 0.5rem 1rem 0.5rem 3rem;
    color: rgba(255, 255, 255, 0.8);
    text-decoration: none;
    font-size: 13px;
    transition: all 200ms ease;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .nav-sublink:hover {
    background-color: rgba(255, 255, 255, 0.1);
    color: var(--color-neutral-white);
  }

  .nav-sublink.router-link-exact-active {
    background-color: rgba(255, 255, 255, 0.15);
    color: var(--color-neutral-white);
    border-right: 3px solid var(--color-secondary-main);
  }

  /* 스크롤바 스타일링 */
  .sidebar-nav::-webkit-scrollbar {
    width: 6px;
  }

  .sidebar-nav::-webkit-scrollbar-track {
    background: transparent;
  }

  .sidebar-nav::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.2);
    border-radius: 3px;
  }

  .sidebar-nav::-webkit-scrollbar-thumb:hover {
    background: rgba(255, 255, 255, 0.4);
  }

  /* 접힌 상태에서는 스크롤바 숨김 */
  .sidebar.collapsed .sidebar-nav::-webkit-scrollbar {
    width: 0;
  }

  .sidebar.collapsed .sidebar-nav {
    scrollbar-width: none; /* Firefox */
    -ms-overflow-style: none; /* IE and Edge */
  }

  /* 툴크 효과 (접힌 상태에서 hover 시 메뉴명 표시) */
  .collapsed .nav-link {
    position: relative;
  }

  .collapsed .nav-link:hover::after {
    content: attr(data-tooltip);
    position: absolute;
    left: 100%;
    top: 50%;
    transform: translateY(-50%);
    background-color: var(--color-neutral-dark);
    color: var(--color-neutral-white);
    padding: 0.375rem 0.75rem;
    border-radius: 0.25rem;
    font-size: 12px;
    white-space: nowrap;
    z-index: 1000;
    margin-left: 0.5rem;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }

  /* 서브메뉴 슬라이드 애니메이션 - Vue transition 클래스들 (런타임에 동적 적용) */
  /* stylelint-disable-next-line selector-class-pattern */
  .submenu-enter-active {
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    overflow: hidden;
  }

  /* stylelint-disable-next-line selector-class-pattern */
  .submenu-leave-active {
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    overflow: hidden;
  }

  /* stylelint-disable-next-line selector-class-pattern */
  .submenu-enter-from {
    opacity: 0;
    max-height: 0;
    transform: translateY(-8px);
  }

  /* stylelint-disable-next-line selector-class-pattern */
  .submenu-enter-to {
    opacity: 1;
    max-height: 300px;
    transform: translateY(0);
  }

  /* stylelint-disable-next-line selector-class-pattern */
  .submenu-leave-from {
    opacity: 1;
    max-height: 300px;
    transform: translateY(0);
  }

  /* stylelint-disable-next-line selector-class-pattern */
  .submenu-leave-to {
    opacity: 0;
    max-height: 0;
    transform: translateY(-4px);
  }
</style>
