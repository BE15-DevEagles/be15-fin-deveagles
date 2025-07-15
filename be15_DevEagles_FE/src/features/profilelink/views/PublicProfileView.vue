<script setup>
  import { ref, onMounted } from 'vue';
  import { useRoute } from 'vue-router';
  import { Phone } from 'lucide-vue-next';
  import { getPublicShopInfo } from '@/features/profilelink/api/shop.js';

  const route = useRoute();
  const shop = ref(null);
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
    const shopId = route.params.shopId;
    if (shopId) {
      window.open(`/reserve/${shopId}/staff`, '_blank');
    } else {
      console.error('shopId를 찾을 수 없습니다.');
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

  onMounted(async () => {
    try {
      const shopId = route.params.shopId;
      if (!shopId) throw new Error('매장 ID가 없습니다.');
      const response = await getPublicShopInfo(shopId);
      shop.value = response.data.data;
    } catch (error) {
      console.error('공개 매장 정보 조회에 실패했습니다:', error);
    } finally {
      isLoading.value = false;
    }
  });
</script>

<template>
  <div class="public-profile-wrapper">
    <div v-if="isLoading" class="loading-state">로딩 중...</div>
    <div v-else-if="shop" class="profile-link-view">
      <div class="shop-header">
        <h2 class="shop-name">
          <span class="sparkle">✨</span>
          {{ shop.shopName }}
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
    <div v-else class="error-state">매장 정보를 찾을 수 없습니다.</div>
  </div>
</template>

<style scoped>
  @import url('https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css');

  .public-profile-wrapper {
    font-family: 'Pretendard', sans-serif;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background-color: #f7f8fc;
    padding: 24px;
    box-sizing: border-box;
  }
  .loading-state,
  .error-state {
    font-size: 16px;
    font-weight: 500;
    color: #5a6472;
  }
  .profile-link-view {
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
    width: 100%;
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
  .shop-address,
  .shop-detail-address {
    margin-bottom: 2px;
  }
  .icon-row {
    display: flex;
    justify-content: center;
    gap: 18px;
    margin-bottom: 24px;
    width: 100%;
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
</style>
