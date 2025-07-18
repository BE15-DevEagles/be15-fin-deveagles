<template>
  <header class="header">
    <div class="header-left">
      <div class="quick-menu-section">
        <span class="quick-menu-label">Quick Menu</span>
        <nav class="quick-menu">
          <div class="tooltip-container">
            <button class="quick-menu-item" @click="goToCalendar">
              <CalendarIcon :size="16" />
              <span>캘린더</span>
            </button>
            <span class="tooltip tooltip-bottom tooltip-primary">예약 캘린더</span>
          </div>
          <div class="tooltip-container">
            <button class="quick-menu-item" @click="goToReservationList">
              <ListIcon :size="16" />
              <span>예약 목록</span>
            </button>
            <span class="tooltip tooltip-bottom tooltip-primary">예약 신청 목록</span>
          </div>
          <div class="tooltip-container">
            <button class="quick-menu-item" @click="openReservationRegistModal">
              <PlusCircleIcon :size="16" />
              <span>예약 등록</span>
            </button>
            <span class="tooltip tooltip-bottom tooltip-primary">새 예약 등록</span>
          </div>
          <div class="tooltip-container">
            <button class="quick-menu-item" @click="openSalesRegistModal">
              <ReceiptIcon :size="16" />
              <span>매출 등록</span>
            </button>
            <span class="tooltip tooltip-bottom tooltip-primary">상품 매출 등록</span>
          </div>
          <div class="tooltip-container">
            <button class="quick-menu-item" @click="openMembershipSalesModal">
              <GiftIcon :size="16" />
              <span>회원권 판매</span>
            </button>
            <span class="tooltip tooltip-bottom tooltip-primary">회원권 판매 등록</span>
          </div>
        </nav>
      </div>
    </div>

    <div class="header-right">
      <div class="header-actions">
        <!-- 검색 -->
        <div class="search-box">
          <SearchIcon :size="16" class="search-icon" />
          <input
            ref="searchInputRef"
            v-model="searchQuery"
            type="text"
            placeholder="고객명, 연락처 검색..."
            class="search-input"
            @focus="onSearchFocus"
            @blur="onSearchBlur"
            @keyup.enter="executeSearch"
            @compositionstart="handleCompositionStart"
            @compositionend="handleCompositionEnd"
            @input="handleInput"
          />
          <div class="search-shortcut" :class="{ visible: !isSearchFocused && !searchQuery }">
            <span class="shortcut-key">Ctrl</span>
            <span class="shortcut-plus">+</span>
            <span class="shortcut-key">K</span>
          </div>
          <!-- 자동완성 제안 목록 -->
          <ul v-if="showSuggestions" ref="searchListRef" class="search-suggestions">
            <li
              v-for="(cust, index) in searchSuggestions"
              :key="cust.customer_id"
              :class="['suggestion-item', { active: index === activeIndex }]"
              @mousedown.prevent="selectSuggestion(cust)"
            >
              <span>{{ cust.customer_name }}</span>
              <span class="suggestion-phone">{{ cust.phone_number }}</span>
            </li>
          </ul>
        </div>

        <!-- 새 고객 등록 버튼 -->
        <div class="tooltip-container">
          <button class="action-btn" @click="createCustomer">
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
          </button>
          <span class="tooltip tooltip-bottom tooltip-primary">새 고객 등록</span>
        </div>

        <!-- 알림 버튼 -->
        <div class="tooltip-container relative">
          <button ref="bellButtonRef" class="quick-menu-item" @click="toggleNotifications">
            <BellIcon :size="16" />

            <span v-if="unreadCount > 0" class="notification-badge">
              {{ unreadCount }}
            </span>
          </button>
          <span class="tooltip tooltip-bottom tooltip-primary">알림</span>

          <NotificationList v-model="showNotifications" :trigger-element="bellButtonRef" />
        </div>
      </div>

      <!-- 사용자 메뉴 -->
      <div ref="userMenuRef" class="user-menu">
        <button class="user-btn" @click="toggleUserMenu">
          <span class="user-avatar">
            <span class="avatar-text">{{ userInitial }}</span>
          </span>
          <span class="user-info">
            <span class="user-name">{{ staffName }}</span>
            <span class="user-role">{{ grade }}</span>
          </span>
          <ChevronDownIcon :size="12" class="dropdown-arrow" :class="{ rotated: showUserMenu }" />
        </button>

        <Transition name="dropdown">
          <div v-if="showUserMenu" class="user-dropdown">
            <div class="dropdown-header">
              <div class="user-profile">
                <div class="user-avatar-large">
                  <span class="avatar-text">{{ userInitial }}</span>
                </div>
                <div class="user-details">
                  <div class="user-name">{{ staffName }}</div>
                  <div class="user-uid">{{ username }}</div>
                </div>
              </div>
            </div>

            <div class="dropdown-menu">
              <router-link to="/profile" class="dropdown-item">
                <UserIcon :size="16" class="item-icon" />
                <span>프로필</span>
              </router-link>
              <router-link to="/settings/account" class="dropdown-item">
                <SettingsIcon :size="16" class="item-icon" />
                <span>계정 설정</span>
              </router-link>
              <div class="dropdown-divider"></div>
              <button class="dropdown-item" @click="handleLogout">
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  class="item-icon"
                >
                  <path
                    d="M9 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H9"
                  ></path>
                  <polyline points="16,17 21,12 16,7"></polyline>
                  <line x1="21" y1="12" x2="9" y2="12"></line>
                </svg>
                <span>로그아웃</span>
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </div>

    <!-- 고객 상세 모달 -->
    <Teleport to="body">
      <CustomerDetailModal
        v-if="showCustomerModal"
        v-model="showCustomerModal"
        :customer="selectedCustomer"
        @request-delete="handleDeleteRequest"
        @request-edit="handleEditRequest"
        @request-reservation="handleReservationRequest"
        @request-sales="handleSalesRequest"
      />
      <CustomerEditDrawer
        v-model="showEditDrawer"
        :customer="selectedCustomer"
        @update="handleUpdateCustomer"
        @after-leave="handleEditDrawerAfterLeave"
      />
      <CustomerCreateDrawer v-model="showCreateDrawer" @create="handleCreateCustomer" />
      <ScheduleRegistModal
        v-if="showReservationRegistModal"
        v-model="showReservationRegistModal"
        :initial-customer="selectedCustomer"
        @submit="() => showSuccess('예약이 등록되었습니다.')"
        @error="err => showError(err?.message || '예약 등록에 실패했습니다.')"
      />
      <ItemsSalesRegistModal
        v-if="showSalesRegistModal"
        v-model="showSalesRegistModal"
        :initial-customer-id="selectedCustomer?.customerId || selectedCustomer?.customer_id"
        @close="showSalesRegistModal = false"
        @submit="() => showSuccess('매출이 등록되었습니다.')"
        @error="err => showError(err?.message || '매출 등록에 실패했습니다.')"
      />
      <MembershipSalesRegistModal
        v-if="showMembershipSalesModal"
        @close="showMembershipSalesModal = false"
        @submit="() => showSuccess('회원권 판매가 등록되었습니다.')"
        @error="err => showError(err?.message || '회원권 판매 등록에 실패했습니다.')"
      />
    </Teleport>
  </header>
</template>

<script setup>
  import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
  import {
    SearchIcon,
    BellIcon,
    ChevronDownIcon,
    UserIcon,
    SettingsIcon,
    CalendarIcon,
    ListIcon,
    PlusCircleIcon,
    ReceiptIcon,
    GiftIcon,
  } from '../icons/index.js';
  import NotificationList from '@/features/notifications/components/NotificationList.vue';
  import CustomerDetailModal from '@/features/customer/components/CustomerDetailModal.vue';
  import CustomerEditDrawer from '@/features/customer/components/CustomerEditDrawer.vue';
  import CustomerCreateDrawer from '@/features/customer/components/CustomerCreateDrawer.vue';
  import { useAuthStore } from '@/store/auth.js';
  import { storeToRefs } from 'pinia';
  import { useRouter } from 'vue-router';
  import { logout } from '@/features/users/api/users.js';
  import customersAPI from '@/features/customer/api/customers.js';
  import { useMetadataStore } from '@/store/metadata.js';
  import { useNotifications } from '@/features/notifications/composables/useNotifications.js';
  import ItemsSalesRegistModal from '@/features/sales/components/ItemsSalesRegistModal.vue';
  import MembershipSalesRegistModal from '@/features/sales/components/MembershipSalesRegistModal.vue';
  import ScheduleRegistModal from '@/features/schedules/components/ScheduleRegistModal.vue';
  import { useToast } from '@/composables/useToast.js';

  const router = useRouter();
  const searchListRef = ref(null);
  const userMenuRef = ref(null);
  const searchInputRef = ref(null);

  // 상태
  const searchQuery = ref('');
  const showUserMenu = ref(false);
  // 알림 관련 상태
  const showNotifications = ref(false);
  const { unreadCount } = useNotifications();
  const bellButtonRef = ref(null);
  const isSearchFocused = ref(false);

  // 사용자 정보
  const authStore = useAuthStore();
  const { staffName, username, grade } = storeToRefs(authStore);

  const userInitial = computed(() => {
    return staffName.value ? staffName.value.charAt(0).toUpperCase() : 'U';
  });

  // 검색 관련
  const onSearchFocus = () => {
    isSearchFocused.value = true;
  };

  const onSearchBlur = () => {
    isSearchFocused.value = false;
    setTimeout(() => {
      showSuggestions.value = false;
    }, 150);
  };

  const activeIndex = ref(-1);

  const sortSuggestions = (arr, keyword) => {
    const lower = keyword.toLowerCase();
    return arr.slice().sort((a, b) => {
      const aName = a.customer_name.toLowerCase();
      const bName = b.customer_name.toLowerCase();
      const aPhone = (a.phone_number || '').toLowerCase();
      const bPhone = (b.phone_number || '').toLowerCase();

      const aStarts = aName.startsWith(lower) || aPhone.startsWith(lower);
      const bStarts = bName.startsWith(lower) || bPhone.startsWith(lower);
      if (aStarts && !bStarts) return -1;
      if (!aStarts && bStarts) return 1;
      return aName.localeCompare(bName);
    });
  };

  watch(activeIndex, newIndex => {
    if (newIndex === -1 || !searchListRef.value) return;

    const listElement = searchListRef.value;
    const activeElement = listElement.children[newIndex];

    if (activeElement) {
      const containerRect = listElement.getBoundingClientRect();
      const elementRect = activeElement.getBoundingClientRect();

      if (elementRect.bottom > containerRect.bottom) {
        listElement.scrollTop += elementRect.bottom - containerRect.bottom;
      } else if (elementRect.top < containerRect.top) {
        listElement.scrollTop -= containerRect.top - elementRect.top;
      }
    }
  });

  const showCustomerModal = ref(false);
  const selectedCustomer = ref(null);

  const showCustomerDetail = async customer => {
    if (!customer) return;

    const rawId = customer.customerId || customer.customer_id;
    if (!rawId) return;

    if (typeof rawId === 'string' && rawId.startsWith('auto-')) {
      return;
    }

    try {
      const detail = await customersAPI.getCustomerDetail(rawId);
      selectedCustomer.value = detail || customer;
    } catch {
      selectedCustomer.value = {
        customerId: customer.customerId || customer.customer_id,
        customerName: customer.customerName || customer.customer_name,
        phoneNumber: customer.phoneNumber || customer.phone_number,
        customerGrade: customer.customerGradeId
          ? {
              customerGradeId: customer.customerGradeId,
              customerGradeName: customer.customerGradeName,
            }
          : undefined,
        staff: customer.staffId
          ? { staffId: customer.staffId, staffName: customer.staffName }
          : undefined,
        acquisitionChannelName:
          customer.acquisitionChannelName || customer.acquisition_channel_name,
      };
    }

    showCustomerModal.value = true;
    showSuggestions.value = false;
    searchQuery.value = '';
  };

  const handleDeleteRequest = async customerId => {
    try {
      const confirmed = await window.confirm('정말 이 고객을 삭제하시겠습니까?');
      if (!confirmed) return;

      await customersAPI.deleteCustomer(customerId);
      showCustomerModal.value = false;
      cachedCustomers.value = cachedCustomers.value.filter(c => c.customer_id !== customerId);
    } catch (error) {
      console.error('고객 삭제 실패:', error);
      alert('고객 삭제에 실패했습니다.');
    }
  };

  const handleEditRequest = () => {
    showCustomerModal.value = false;
    showEditDrawer.value = true;
  };

  const handleReservationRequest = customer => {
    showCustomerModal.value = false;
    selectedCustomer.value = {
      ...customer,
      customerId: customer.customerId || customer.customer_id || null,
      customer_id: customer.customer_id || customer.customerId || null,
    };
    showReservationRegistModal.value = true;
  };

  const handleSalesRequest = customer => {
    showCustomerModal.value = false;
    selectedCustomer.value = {
      ...customer,
      customerId: customer.customerId || customer.customer_id || null,
      customer_id: customer.customer_id || customer.customerId || null,
      id: customer.customerId || customer.customer_id || null,
      customer_name: customer.customerName || customer.customer_name || '',
      phone_number: customer.phoneNumber || customer.phone_number || '',
    };
    showSalesRegistModal.value = true;
  };

  const selectSuggestion = customer => {
    if (!customer) return;

    if (typeof customer === 'string') {
      searchQuery.value = customer;
      executeSearch();
    } else {
      showCustomerDetail(customer);
    }
  };

  // 키보드 이벤트 처리 개선
  const handleKeydown = event => {
    // Ctrl+K 또는 Cmd+K로 검색창 포커스
    if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
      event.preventDefault();
      searchInputRef.value?.focus();
    }

    // ESC로 검색창 블러
    if (event.key === 'Escape') {
      if (showSuggestions.value) {
        event.preventDefault();
        showSuggestions.value = false;
      } else if (isSearchFocused.value) {
        searchInputRef.value?.blur();
      }
    }

    // suggestion navigation
    if (showSuggestions.value && searchSuggestions.value.length) {
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        activeIndex.value = (activeIndex.value + 1) % searchSuggestions.value.length;
      } else if (event.key === 'ArrowUp') {
        event.preventDefault();
        activeIndex.value =
          activeIndex.value > 0 ? activeIndex.value - 1 : searchSuggestions.value.length - 1;
      } else if (event.key === 'Enter' && activeIndex.value >= 0) {
        event.preventDefault();
        selectSuggestion(searchSuggestions.value[activeIndex.value]);
      }
    }
  };

  // 상태 추가
  const showEditDrawer = ref(false);
  const showCreateDrawer = ref(false);
  const showReservationRegistModal = ref(false);
  const showSalesRegistModal = ref(false);
  const showMembershipSalesModal = ref(false);

  // 고객 수정 요청 처리
  const handleUpdateCustomer = async updatedCustomerPayload => {
    try {
      const originalCustomer = selectedCustomer.value;
      const originalTagIds = new Set(
        (originalCustomer.tags || []).map(t => t.tagId).filter(Boolean)
      );
      const newTagIdsArr = updatedCustomerPayload.tags || [];
      const newTagIds = new Set(newTagIdsArr);

      const customerPayload = { ...updatedCustomerPayload };
      delete customerPayload.tags;

      const customerId = originalCustomer.customerId || originalCustomer.customer_id;
      await customersAPI.updateCustomer(customerId, customerPayload);

      // Add new tags
      for (const tagId of newTagIds) {
        if (!originalTagIds.has(tagId)) {
          try {
            await customersAPI.addTagToCustomer(customerId, tagId);
            // 메타데이터 스토어 갱신
            await metadataStore.loadMetadata(true);
          } catch {
            // 오류 무시
          }
        }
      }

      // Remove old tags
      for (const tagId of originalTagIds) {
        if (!newTagIds.has(tagId)) {
          try {
            await customersAPI.removeTagFromCustomer(customerId, tagId);
            // 메타데이터 스토어 갱신
            await metadataStore.loadMetadata(true);
          } catch (e) {
            console.error(`태그 제거 실패: customerId=${customerId}, tagId=${tagId}`, e);
          }
        }
      }

      // 리스트 새로고침
      cachedCustomers.value = [];
      lastFetchTime.value = 0;
      await fetchCustomers();

      showEditDrawer.value = false;
    } catch (error) {
      console.error('고객 정보 업데이트 실패:', error);
      alert('고객 정보 업데이트에 실패했습니다.');
    }
  };

  const handleEditDrawerAfterLeave = () => {
    selectedCustomer.value = null;
  };

  const createCustomer = () => {
    showCreateDrawer.value = true;
  };

  const handleCreateCustomer = async newCustomerPayload => {
    try {
      await customersAPI.createCustomer(newCustomerPayload);

      cachedCustomers.value = [];
      lastFetchTime.value = 0;
      await fetchCustomers();

      showCreateDrawer.value = false;
    } catch (error) {
      console.error('고객 생성 실패:', error);
      alert('고객 생성에 실패했습니다.');
    }
  };

  const toggleUserMenu = () => {
    showUserMenu.value = !showUserMenu.value;
  };

  const toggleNotifications = async () => {
    await nextTick();
    const el = bellButtonRef.value;
    if (el && el.getBoundingClientRect().width > 0) {
      showNotifications.value = !showNotifications.value;
    } else {
      console.warn('bellButtonRef가 아직 화면에 렌더되지 않았습니다.');
    }
  };

  // 외부 클릭 감지
  const handleClickOutside = event => {
    if (userMenuRef.value && !userMenuRef.value.contains(event.target)) {
      showUserMenu.value = false;
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
    } catch (err) {
      console.warn(`[Header] 서버 로그아웃 실패 : ${err}`);
    } finally {
      // 웹소켓 연결 끊기
      const { disconnectSocket } = await import('@/features/chat/composables/socket.js');
      disconnectSocket();

      authStore.clearAuth();
      router.push('/login');
    }
  };

  const searchSuggestions = ref([]);
  const showSuggestions = ref(false);
  const isComposing = ref(false);

  // 캐시된 고객 목록
  const cachedCustomers = ref([]);
  const lastFetchTime = ref(0);
  const CACHE_DURATION = 5 * 60 * 1000; // 5분

  // 고객 목록 가져오기 (캐시 적용)
  const fetchCustomers = async () => {
    const now = Date.now();
    if (cachedCustomers.value.length > 0 && now - lastFetchTime.value < CACHE_DURATION) {
      return cachedCustomers.value;
    }

    try {
      const customers = await customersAPI.getCustomersByShop();
      cachedCustomers.value = customers;
      lastFetchTime.value = now;
      return customers;
    } catch (e) {
      console.warn('[Header] 고객 목록 조회 실패', e);
      return [];
    }
  };

  const handleCompositionStart = () => {
    isComposing.value = true;
  };

  const handleCompositionEnd = () => {
    isComposing.value = false;
    fetchAutocomplete(searchQuery.value);
  };

  const searchTimeout = ref(null);
  const isSearching = ref(false);

  const handleInput = async event => {
    const value = event.target.value;
    searchQuery.value = value;

    // 입력값이 없으면 검색 결과 초기화
    if (!value || !value.trim()) {
      searchSuggestions.value = [];
      showSuggestions.value = false;
      return;
    }

    // 이전 타이머 취소
    if (searchTimeout.value) {
      clearTimeout(searchTimeout.value);
    }

    // 새로운 타이머 설정 (300ms 디바운스)
    searchTimeout.value = setTimeout(() => {
      fetchAutocomplete(value);
    }, 300);
  };

  const fetchAutocomplete = async keyword => {
    if (!keyword || !keyword.trim()) {
      searchSuggestions.value = [];
      showSuggestions.value = false;
      return;
    }

    // 이미 검색 중이면 중복 요청 방지
    if (isSearching.value) {
      return;
    }

    isSearching.value = true;

    try {
      let results = [];

      // 키워드 검색 시도 (Elasticsearch)
      try {
        results = await customersAPI.searchByKeyword(keyword);
      } catch (err) {
        console.warn('[Header] 키워드 검색 실패', err);
      }

      // 결과가 없으면 자동완성 시도
      if (!results.length) {
        try {
          const strings = await customersAPI.autocomplete(keyword);
          results = strings.map((s, idx) => ({
            customer_id: `auto-${idx}`,
            customer_name: s,
            phone_number: '',
          }));
        } catch (err) {
          console.warn('[Header] 자동완성 실패', err);
        }
      }

      // 모든 서버 검색이 실패하면 클라이언트 사이드 필터링
      if (!results.length) {
        const customers = await fetchCustomers();
        const lowerKeyword = keyword?.toLowerCase() || '';
        results =
          customers?.filter(
            c =>
              (c?.customer_name || '').toLowerCase().includes(lowerKeyword) ||
              (c?.phone_number || '').includes(lowerKeyword)
          ) || [];
      }

      const sortedResults = sortSuggestions(results, keyword);
      searchSuggestions.value = sortedResults;
      if (!showSuggestions.value) {
        activeIndex.value = -1;
      }
      showSuggestions.value = sortedResults.length > 0;
    } catch (err) {
      console.warn('[Header] 검색 오류', err);
      searchSuggestions.value = [];
      showSuggestions.value = false;
    } finally {
      isSearching.value = false;
    }
  };

  const executeSearch = () => {
    if (activeIndex.value >= 0 && searchSuggestions.value.length > 0) {
      selectSuggestion(searchSuggestions.value[activeIndex.value]);
    } else {
      searchQuery.value = '';
      showSuggestions.value = false;
    }
  };

  const metadataStore = useMetadataStore();
  const { showSuccess, showError } = useToast();

  onMounted(async () => {
    document.addEventListener('click', handleClickOutside);
    document.addEventListener('keydown', handleKeydown);
  });

  onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside);
    document.removeEventListener('keydown', handleKeydown);
  });

  const goToCalendar = () => router.push('/schedule/calendar');
  const goToReservationList = () => router.push('/reservation/requests');
  const openReservationRegistModal = () => (showReservationRegistModal.value = true);
  const openSalesRegistModal = () => {
    selectedCustomer.value = null; // 고객 선택 없이 모달 열기
    showSalesRegistModal.value = true;
  };
  const openMembershipSalesModal = () => (showMembershipSalesModal.value = true);
</script>

<style scoped>
  @import '@/assets/css/styleguide.css';

  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 1.5rem;
    background-color: var(--color-neutral-white);
    border-bottom: 1px solid var(--color-gray-200);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    position: sticky;
    top: 0;
    z-index: 100;
    height: 56px;
    min-height: 56px;
  }

  .header-left {
    flex: 1;
    display: flex;
    align-items: center;
  }

  .quick-menu-section {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .quick-menu-label {
    font-size: 11px;
    font-weight: 600;
    color: var(--color-gray-500);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    white-space: nowrap;
  }

  .quick-menu {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .quick-menu-item {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0.75rem;
    border: none;
    border-radius: 0.5rem;
    background: none;
    color: var(--color-gray-600);
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 150ms ease;
    white-space: nowrap;
  }

  .quick-menu-item:hover {
    background-color: var(--color-gray-50);
    color: var(--color-gray-900);
    transform: translateY(-1px);
  }

  .header-right {
    display: flex;
    align-items: center;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .search-box {
    position: relative;
    display: flex;
    align-items: center;
  }

  .search-icon {
    position: absolute;
    left: 0.75rem;
    color: var(--color-gray-500);
    z-index: 1;
    transition: color 150ms ease;
  }

  .search-input {
    width: 240px;
    padding: 0.5rem 5rem 0.5rem 2.5rem;
    border: 1px solid var(--color-gray-300);
    border-radius: 0.5rem;
    font-size: 13px;
    background-color: var(--color-gray-50);
    transition: all 150ms ease;
    height: 36px;
  }

  .search-input:focus {
    outline: none;
    border-color: var(--color-primary-500);
    background-color: var(--color-neutral-white);
    box-shadow: 0 0 0 3px rgba(54, 79, 107, 0.08);
    width: 280px;
  }

  .search-input:focus + .search-icon,
  .search-input:focus ~ .search-icon {
    color: var(--color-primary-500);
  }

  .search-shortcut {
    position: absolute;
    right: 0.75rem;
    display: flex;
    align-items: center;
    gap: 0.125rem;
    opacity: 0;
    transition: opacity 150ms ease;
    pointer-events: none;
  }

  .search-shortcut.visible {
    opacity: 1;
  }

  .shortcut-key {
    background-color: var(--color-gray-100);
    border: 1px solid var(--color-gray-300);
    border-radius: 0.25rem;
    padding: 0.125rem 0.375rem;
    font-size: 10px;
    font-weight: 500;
    color: var(--color-gray-600);
    line-height: 1;
  }

  .shortcut-plus {
    font-size: 10px;
    color: var(--color-gray-500);
    font-weight: 500;
  }

  .action-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border: none;
    border-radius: 0.5rem;
    background-color: var(--color-gray-50);
    color: var(--color-gray-600);
    cursor: pointer;
    transition: all 150ms ease;
  }

  .action-btn:hover {
    background-color: var(--color-gray-100);
    color: var(--color-gray-900);
    transform: translateY(-1px);
  }

  .notification-btn {
    position: relative;
    background: none;
    border: none;
    padding: 0.25rem;
    cursor: pointer;
  }

  .notification-btn:hover {
    background-color: var(--color-gray-50);
    color: var(--color-gray-900);
    transform: translateY(-1px);
  }

  .notification-badge {
    position: absolute;
    top: -4px;
    right: -4px;
    background-color: var(--color-warning-300);
    color: var(--color-neutral-white);
    font-size: 10px;
    font-weight: bold;

    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 1rem; /* 16px */
    height: 1rem; /* 16px */
    padding: 0 4px;
    border-radius: 9999px;
    line-height: 1;
    box-sizing: border-box;
  }

  .user-menu {
    position: relative;
  }

  .user-btn {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.25rem 0.5rem;
    border: none;
    border-radius: 0.5rem;
    background: none;
    cursor: pointer;
    transition: all 150ms ease;
    height: 40px;
  }

  .user-btn:hover {
    background-color: var(--color-gray-50);
    transform: translateY(-1px);
  }

  .user-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-600));
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-neutral-white);
    font-weight: 600;
    font-size: 13px;
    box-shadow: 0 2px 8px rgba(54, 79, 107, 0.15);
  }

  .user-info {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    line-height: 1.2;
  }

  .user-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--color-gray-900);
  }

  .user-role {
    font-size: 11px;
    color: var(--color-gray-500);
    margin-top: 1px;
  }

  .dropdown-arrow {
    color: var(--color-gray-500);
    transition: transform 150ms ease;
    margin-left: 0.25rem;
  }

  .dropdown-arrow.rotated {
    transform: rotate(180deg);
  }

  .user-dropdown {
    position: absolute;
    top: calc(100% + 0.5rem);
    right: 0;
    background-color: var(--color-neutral-white);
    border: 1px solid var(--color-gray-200);
    border-radius: 0.75rem;
    box-shadow: 0 10px 40px -10px rgba(0, 0, 0, 0.12);
    min-width: 240px;
    z-index: 1000;
    overflow: hidden;
  }

  .dropdown-header {
    padding: 1rem;
    background: linear-gradient(135deg, var(--color-gray-50), var(--color-gray-25));
    border-bottom: 1px solid var(--color-gray-200);
  }

  .user-profile {
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }

  .user-avatar-large {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-600));
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-neutral-white);
    font-weight: 600;
    font-size: 16px;
    box-shadow: 0 4px 12px rgba(54, 79, 107, 0.2);
  }

  .user-details {
    flex: 1;
  }

  .user-details .user-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--color-gray-900);
  }

  .user-uid {
    font-size: 12px;
    color: var(--color-gray-500);
    margin-top: 2px;
  }

  .dropdown-menu {
    padding: 0.5rem;
  }

  .dropdown-item {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.625rem 0.75rem;
    border-radius: 0.5rem;
    color: var(--color-gray-700);
    text-decoration: none;
    font-size: 13px;
    font-weight: 500;
    transition: all 150ms ease;
    border: none;
    background: none;
    width: 100%;
    text-align: left;
    cursor: pointer;
  }

  .dropdown-item:hover {
    background-color: var(--color-gray-50);
    color: var(--color-gray-900);
    transform: translateY(-1px);
  }

  .item-icon {
    color: var(--color-gray-500);
    flex-shrink: 0;
  }

  .dropdown-divider {
    height: 1px;
    background-color: var(--color-gray-200);
    margin: 0.5rem 0;
  }

  /* 드롭다운 애니메이션 */
  .dropdown-enter-active,
  .dropdown-leave-active {
    transition: all 150ms ease;
    transform-origin: top right;
  }

  .dropdown-enter-from,
  .dropdown-leave-to {
    opacity: 0;
    transform: scale(0.95) translateY(-10px);
  }

  /* 반응형 */
  @media (max-width: 1024px) {
    .search-input {
      width: 200px;
    }

    .search-input:focus {
      width: 240px;
    }

    .quick-menu-item span {
      display: none;
    }

    .quick-menu-item {
      padding: 0.5rem;
      min-width: 36px;
      justify-content: center;
    }

    .quick-menu-label {
      display: none;
    }

    .quick-menu {
      gap: 0.25rem;
    }
  }

  @media (max-width: 768px) {
    .header {
      padding: 0 1rem;
      height: 52px;
      min-height: 52px;
    }

    .search-input {
      width: 160px;
      padding-right: 2.5rem;
    }

    .search-input:focus {
      width: 200px;
    }

    .search-shortcut {
      display: none;
    }

    .user-info {
      display: none;
    }

    .user-btn {
      gap: 0;
      padding: 0.25rem;
    }

    .quick-menu-section {
      gap: 0.5rem;
    }

    .header-actions {
      gap: 0.75rem;
    }
  }

  .search-suggestions {
    position: absolute;
    top: calc(100% + 4px);
    left: 0;
    width: 100%;
    background-color: var(--color-neutral-white);
    border: 1px solid var(--color-gray-200);
    border-radius: 0.5rem;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    max-height: 240px;
    overflow-y: auto;
    z-index: 1100;
    scroll-behavior: smooth;
  }

  .suggestion-item {
    padding: 0.75rem;
    font-size: 13px;
    cursor: pointer;
    display: flex;
    justify-content: space-between;
    align-items: center;
    transition: background-color 0.15s ease;
  }

  .suggestion-item:hover,
  .suggestion-item.active {
    background-color: var(--color-primary-50);
  }

  .suggestion-item.active {
    background-color: var(--color-primary-100);
  }

  .suggestion-phone {
    color: var(--color-gray-500);
    font-size: 12px;
  }
</style>
