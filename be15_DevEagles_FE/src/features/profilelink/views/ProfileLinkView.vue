<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { Phone } from 'lucide-vue-next';
  import { useAuthStore } from '@/store/auth.js';
  import { getShopInfo } from '@/features/profilelink/api/shop.js';

  const authStore = useAuthStore();
  const shop = ref(null);
  const copied = ref(false);
  const isLoading = ref(true);

  function getSnsInfo(type) {
    switch (type) {
      case 'INSTA':
        return { text: '인스타그램', emoji: '📸' };
      case 'BLOG':
        return { text: '네이버 블로그', emoji: '✍️' };
      case 'ETC':
        return { text: '웹사이트', emoji: '🔗' };
      default:
        return { text: '방문하기', emoji: '➡️' };
    }
  }

  function goToSns(url) {
    if (url) window.open(url, '_blank');
  }

  function callShop() {
    if (shop.value?.phoneNumber) window.open(`tel:${shop.value.phoneNumber}`);
  }

  function goReservation() {
    if (reservation_url.value) {
      window.open(reservation_url.value, '_blank');
    }
  }

  function goMap() {
    if (shop.value?.address) {
      const fullAddress = `${shop.value.address} ${shop.value.detailAddress || ''}`.trim();
      const encodedAddress = encodeURIComponent(fullAddress);
      const kakaoMapUrl = `https://map.kakao.com/link/search/${encodedAddress}`;
      window.open(kakaoMapUrl, '_blank');
    }
  }

  const reservation_url = computed(() => {
    const currentShopId = authStore.shopId;
    if (currentShopId) {
      const baseUrl = import.meta.env.VITE_RESERVE_BASE_URL;
      return `${baseUrl}/reserve/${currentShopId}/staff`;
    }
    return '';
  });

  const profile_url = computed(() => {
    const currentShopId = authStore.shopId;
    if (currentShopId) {
      const baseUrl = import.meta.env.VITE_PROFILE_LINK_BASE_URL;
      return `${baseUrl}/p/${currentShopId}`;
    }
    return '';
  });

  function copyProfileUrl() {
    if (profile_url.value) {
      navigator.clipboard.writeText(profile_url.value);
      copied.value = true;
      setTimeout(() => {
        copied.value = false;
      }, 2000);
    }
  }

  onMounted(async () => {
    try {
      const response = await getShopInfo();
      shop.value = response.data.data;
    } catch (error) {
      console.error('매장 정보 조회에 실패했습니다:', error);
    } finally {
      isLoading.value = false;
    }
  });
</script>

<template>
  <div v-if="isLoading" class="loading-container">로딩 중...</div>
  <div v-else-if="shop" class="profile-link-layout">
    <div class="profile-url-box">
      <div class="profile-url-label">나의 멀티 프로필 링크</div>
      <div class="profile-url-row">
        <input class="profile-url-input" :value="profile_url" readonly />
        <button class="copy-btn" @click="copyProfileUrl">
          {{ copied ? '복사 완료!' : '복사' }}
        </button>
      </div>
    </div>
    <div class="profile-link-view">
      <div class="shop-header">
        <h2 class="shop-name">
          <span class="sparkle">✨</span>
          <span class="shop-name-text">{{ shop.shopName }}</span>
          <span class="sparkle">✨</span>
        </h2>
        <p v-if="shop.description" class="shop-description">
          {{ shop.description }}
        </p>
      </div>
      <div class="shop-address-group">
        <div class="shop-address">{{ shop.address }}</div>
        <div class="shop-detail-address">{{ shop.detailAddress }}</div>
      </div>
      <div class="icon-row">
        <button class="icon-btn" aria-label="전화" @click="callShop"><Phone :size="24" /></button>
      </div>
      <div class="btn-group">
        <button class="main-btn" @click="goReservation">
          <span class="btn-emoji">📅</span><span class="btn-text">예약하기</span>
        </button>
        <button class="main-btn" @click="goMap">
          <span class="btn-emoji">📍</span><span class="btn-text">매장 위치</span>
        </button>
        <template v-for="(sns, index) in shop.snsList || []" :key="index">
          <button v-if="sns.snsAddress" class="main-btn" @click="goToSns(sns.snsAddress)">
            <span class="btn-emoji">{{ getSnsInfo(sns.type).emoji }}</span>
            <span class="btn-text">{{ getSnsInfo(sns.type).text }}</span>
          </button>
        </template>
      </div>
    </div>
  </div>
  <div v-else class="error-container">매장 정보를 불러올 수 없습니다.</div>
</template>

<style scoped>
  @import url('https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css');

  .profile-link-layout {
    font-family: 'Pretendard', sans-serif;
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    align-items: start;
    gap: 32px;
    padding: 40px 16px;
    width: 100%;
    max-width: 1200px;
    margin: 0 auto;
    background-color: #f7f8fc;
  }
  .profile-url-box {
    grid-column: 1 / 2;
    justify-self: end;
    min-width: 240px;
    background: #ffffff;
    border-radius: 16px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
    padding: 24px;
    display: flex;
    flex-direction: column;
    align-items: stretch;
    height: fit-content;
    border: 1px solid #eef0f7;
  }
  .profile-url-label {
    font-size: 16px;
    font-weight: 600;
    color: #333d4b;
    margin-bottom: 12px;
  }
  .profile-url-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .profile-url-input {
    flex: 1;
    font-size: 14px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 8px 12px;
    color: #4c5a69;
    background: #f8fafc;
    outline: none;
    min-width: 0;
  }
  .copy-btn {
    background: #2f466c;
    color: #fff;
    border: none;
    border-radius: 8px;
    padding: 8px 16px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;
  }
  .copy-btn:hover {
    background: #1e3250;
    transform: translateY(-1px);
  }
  .profile-link-view {
    grid-column: 2 / 3;
    width: 100%;
    max-width: 400px;
    min-width: 320px;
    background: #fdfdff;
    border: 2px solid #1e1e1e;
    border-radius: 28px;
    box-shadow: 6px 6px 0px #d1d9e6;
    padding: 36px 24px;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  .shop-header {
    text-align: center;
    margin-bottom: 16px;
  }
  .shop-name {
    font-family: 'Gasoek One', 'Pretendard', sans-serif;
    font-size: 28px;
    font-weight: 700;
    color: #333;
    margin-bottom: 8px;
    letter-spacing: 0.5px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
  }
  .shop-name-text {
    text-align: center;
    word-break: break-word;
  }
  .sparkle {
    display: inline-block;
    animation: sparkle-animation 1.5s infinite;
  }
  @keyframes sparkle-animation {
    0%,
    100% {
      transform: scale(1);
      opacity: 1;
    }
    50% {
      transform: scale(1.4);
      opacity: 0.7;
    }
  }
  .shop-description {
    font-size: 1rem;
    color: #5a6472;
    background-color: #f3f4f6;
    padding: 8px 14px;
    border-radius: 12px;
    display: inline-block;
    white-space: pre-wrap;
    text-align: center;
    line-height: 1.6;
  }
  .shop-address-group {
    width: 100%;
    margin-bottom: 24px;
    text-align: center;
    color: #6a7482;
    font-size: 15px;
    line-height: 1.5;
  }
  .icon-row {
    display: flex;
    justify-content: center;
    gap: 18px;
    margin-bottom: 24px;
  }
  .icon-btn {
    background: #fff;
    border: 2px solid #333;
    border-radius: 50%;
    width: 52px;
    height: 52px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.1s ease-out;
    color: #333;
    box-shadow: 3px 3px 0px #e2e8f0;
  }
  .icon-btn:hover {
    background: #fff;
    transform: translate(-2px, -2px);
    box-shadow: 5px 5px 0px #e2e8f0;
  }
  .icon-btn:active {
    transform: translate(2px, 2px);
    box-shadow: 1px 1px 0px #e2e8f0;
  }
  .btn-group {
    display: flex;
    flex-direction: column;
    gap: 14px;
    width: 100%;
  }
  .main-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    width: 100%;
    padding: 14px 0;
    border: 2px solid #1e1e1e;
    border-radius: 16px;
    background: #ffefff;
    color: #1e1e1e;
    font-size: 16px;
    font-weight: 700;
    cursor: pointer;
    box-shadow: 4px 4px 0px #1e1e1e;
    transition: all 0.1s ease-out;
  }
  .main-btn:hover {
    background: #fde8ff;
    transform: translate(-2px, -2px);
    box-shadow: 6px 6px 0px #1e1e1e;
  }
  .main-btn:active {
    transform: translate(2px, 2px);
    box-shadow: 2px 2px 0px #1e1e1e;
  }
  .btn-emoji {
    font-size: 1.2rem;
  }
  @media (max-width: 800px) {
    .profile-link-layout {
      grid-template-columns: 1fr;
      gap: 24px;
      padding: 24px 16px;
    }
    .profile-url-box,
    .profile-link-view {
      grid-column: 1 / -1;
      justify-self: stretch;
    }
  }
</style>
