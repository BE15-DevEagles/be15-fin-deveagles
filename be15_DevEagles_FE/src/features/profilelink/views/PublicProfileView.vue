<script setup>
  import { ref, onMounted, onUnmounted } from 'vue';
  import { useRoute } from 'vue-router';
  import { Phone } from 'lucide-vue-next';
  import { getPublicShopInfo } from '@/features/profilelink/api/shop.js';

  const route = useRoute();
  const shop = ref(null);
  const isLoading = ref(true);

  const particles = ref([]);
  let particleInterval = null;
  const beautyEmojis = ['💄', '💅', '✨', '💖', '🎀', '💎'];

  function createParticle() {
    const id = Date.now() + Math.random();
    const emoji = beautyEmojis[Math.floor(Math.random() * beautyEmojis.length)];
    const duration = Math.random() * 5 + 5;
    const particle = {
      id,
      emoji,
      style: {
        left: `${Math.random() * 100}%`,
        animationDuration: `${duration}s`,
        fontSize: `${Math.random() * 10 + 10}px`,
      },
    };
    particles.value.push(particle);
    setTimeout(() => {
      particles.value = particles.value.filter(p => p.id !== id);
    }, duration * 1000);
  }

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
      const baseUrl = import.meta.env.VITE_RESERVE_BASE_URL || '';
      window.open(`${baseUrl}/reserve/${shopId}/staff`, '_blank');
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
      particleInterval = setInterval(createParticle, 500);
    } catch (error) {
      console.error('공개 매장 정보 조회에 실패했습니다:', error);
    } finally {
      isLoading.value = false;
    }
  });

  onUnmounted(() => {
    if (particleInterval) {
      clearInterval(particleInterval);
    }
  });
</script>

<template>
  <div class="public-profile-wrapper">
    <div class="background-blob"></div>

    <div v-if="isLoading" class="loading-state">
      <div class="spinner-emoji">✨</div>
    </div>
    <div v-else-if="shop" class="profile-link-view">
      <div class="emoji-fall-container">
        <span v-for="p in particles" :key="p.id" class="particle" :style="p.style">{{
          p.emoji
        }}</span>
      </div>
      <div class="profile-content">
        <div class="shop-header">
          <h2 class="shop-name">
            <span class="sparkle">✨</span>
            <span class="shop-name-text">{{ shop.shopName }}</span>
            <span class="sparkle">✨</span>
          </h2>
          <p v-if="shop.description" class="shop-description">{{ shop.description }}</p>
        </div>
        <div class="shop-address-group">
          <div class="shop-address">{{ shop.address }}</div>
          <div class="shop-detail-address">{{ shop.detailAddress }}</div>
        </div>
        <div class="icon-row">
          <button class="icon-btn" aria-label="전화" @click="callShop">
            <Phone class="phone-icon" :size="24" />
          </button>
        </div>
        <div class="btn-group">
          <button class="main-btn primary" @click="goReservation">
            <span class="btn-emoji">📅</span><span class="btn-text">예약하기</span>
          </button>
          <button class="main-btn secondary" @click="goMap">
            <span class="btn-emoji">📍</span><span class="btn-text">매장 위치</span>
          </button>
          <template v-for="(sns, index) in shop.snsList || []" :key="index">
            <button
              v-if="sns.snsAddress"
              class="main-btn secondary"
              @click="goToSns(sns.snsAddress)"
            >
              <span class="btn-emoji">{{ getSnsInfo(sns.type).emoji }}</span>
              <span class="btn-text">{{ getSnsInfo(sns.type).text }}</span>
            </button>
          </template>
        </div>
      </div>
    </div>
    <div v-else class="error-state">매장 정보를 찾을 수 없습니다.</div>
  </div>
</template>

<style scoped>
  @import url('https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css');

  .public-profile-wrapper {
    font-family: 'Pretendard', sans-serif;
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    padding: 24px;
    box-sizing: border-box;
    overflow: hidden;
  }
  .background-blob {
    position: absolute;
    width: 500px;
    height: 500px;
    background: linear-gradient(180deg, rgba(123, 97, 255, 0.4) 0%, rgba(255, 115, 136, 0.4) 100%);
    border-radius: 50%;
    filter: blur(120px);
    top: 10%;
    left: 50%;
    transform: translateX(-50%);
    z-index: 0;
    animation: blob-move 20s infinite alternate;
  }
  @keyframes blob-move {
    from {
      transform: translateX(-60%) translateY(-10%) scale(1);
    }
    to {
      transform: translateX(-40%) translateY(10%) scale(1.2);
    }
  }

  .loading-state,
  .error-state {
    font-size: 18px;
    font-weight: 500;
    color: #fff;
    text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    background: rgba(0, 0, 0, 0.3);
    padding: 12px 24px;
    border-radius: 12px;
    z-index: 1;
  }
  .spinner-emoji {
    font-size: 2rem;
    animation: spin 1.5s linear infinite;
  }
  @keyframes spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }

  .profile-link-view {
    position: relative;
    overflow: hidden;
    width: 100%;
    max-width: 400px;
    min-width: 320px;
    background: rgba(255, 255, 255, 0.65);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 28px;
    box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.1);
    padding: 36px 24px;
    display: flex;
    flex-direction: column;
    align-items: center;
    z-index: 1;
    opacity: 0;
    animation: fade-in-scale 0.6s ease-out forwards;
  }
  @keyframes fade-in-scale {
    from {
      opacity: 0;
      transform: scale(0.95);
    }
    to {
      opacity: 1;
      transform: scale(1);
    }
  }

  .emoji-fall-container {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 1;
  }
  .particle {
    position: absolute;
    top: -30px;
    animation-name: fall;
    animation-timing-function: linear;
    animation-iteration-count: 1;
    opacity: 0;
    text-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
  }
  @keyframes fall {
    0% {
      transform: translateY(0) rotate(0deg);
      opacity: 1;
    }
    100% {
      transform: translateY(100vh) rotate(360deg);
      opacity: 0;
    }
  }
  .profile-content {
    position: relative;
    z-index: 2;
    width: 100%;
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
  .shop-name-text {
    text-align: center;
    word-break: break-word;
  }
  .sparkle {
    display: inline-block;
    animation: sparkle-animation 1.8s infinite;
  }
  @keyframes sparkle-animation {
    0%,
    100% {
      transform: scale(1) rotate(0deg);
      opacity: 1;
    }
    50% {
      transform: scale(1.5) rotate(15deg);
      opacity: 0.7;
    }
  }
  .shop-description {
    font-size: 1rem;
    color: #5a6472;
    background-color: rgba(255, 255, 255, 0.5);
    padding: 8px 14px;
    border-radius: 12px;
    display: inline-block;
    white-space: pre-wrap;
    text-align: center;
    line-height: 1.6;
    border: 1px solid rgba(0, 0, 0, 0.05);
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
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 50%;
    width: 52px;
    height: 52px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #333;
    transition: all 0.2s ease-out;
  }
  .icon-btn:hover {
    transform: translateY(-3px) scale(1.05);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  }
  .icon-btn .phone-icon {
    animation: wiggle 0.5s ease-in-out;
    animation-play-state: paused;
  }
  .icon-btn:hover .phone-icon {
    animation-play-state: running;
  }
  @keyframes wiggle {
    0%,
    100% {
      transform: rotate(0);
    }
    25% {
      transform: rotate(-12deg);
    }
    75% {
      transform: rotate(12deg);
    }
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
    border-radius: 14px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease-out;
    border: none;
  }
  .main-btn:hover {
    transform: translateY(-2px);
  }
  .main-btn:active {
    transform: translateY(0);
  }
  .btn-emoji {
    font-size: 1.2rem;
    display: inline-block;
  }
  .main-btn:hover .btn-emoji {
    animation: bounce 0.4s ease-out;
  }
  @keyframes bounce {
    0%,
    100% {
      transform: translateY(0);
    }
    50% {
      transform: translateY(-6px) scale(1.1);
    }
  }

  /* 버튼 스타일: Primary vs Secondary */
  .main-btn.primary {
    background: #7b61ff;
    color: white;
    box-shadow: 0 4px 20px -5px rgba(123, 97, 255, 0.6);
  }
  .main-btn.primary:hover {
    box-shadow: 0 6px 25px -5px rgba(123, 97, 255, 0.8);
  }
  .main-btn.secondary {
    background: rgba(255, 255, 255, 0.7);
    color: #333;
    border: 1px solid rgba(0, 0, 0, 0.1);
  }
  .main-btn.secondary:hover {
    background: white;
    border-color: rgba(0, 0, 0, 0.15);
  }

  @media (max-width: 800px) {
    .background-blob {
      width: 300px;
      height: 300px;
      filter: blur(80px);
    }
  }
</style>
