<script setup>
  import { ref, computed, onMounted, onUnmounted } from 'vue';
  import { Phone, Link, Check } from 'lucide-vue-next';
  import { useAuthStore } from '@/store/auth.js';
  import { getShopInfo } from '@/features/profilelink/api/shop.js';

  const authStore = useAuthStore();
  const shop = ref(null);
  const copied = ref(false);
  const isLoading = ref(true);

  const particles = ref([]);
  let particleInterval = null;
  const beautyEmojis = ['💄', '💅', '✨', '💖', '🎀', '💎'];

  function createParticle() {
    const id = Date.now() + Math.random();
    const emoji = beautyEmojis[Math.floor(Math.random() * beautyEmojis.length)];
    const duration = Math.random() * 5 + 5; // 5~10초 사이
    const particle = {
      id,
      emoji,
      style: {
        left: `${Math.random() * 100}%`,
        animationDuration: `${duration}s`,
        fontSize: `${Math.random() * 10 + 10}px`, // 10~20px 사이 크기
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
    if (reservation_url.value) window.open(reservation_url.value, '_blank');
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
    return currentShopId
      ? `${import.meta.env.VITE_RESERVE_BASE_URL}/reserve/${currentShopId}/staff`
      : '';
  });

  const profile_url = computed(() => {
    const currentShopId = authStore.shopId;
    return currentShopId ? `${import.meta.env.VITE_PROFILE_LINK_BASE_URL}/p/${currentShopId}` : '';
  });

  function copyProfileUrl() {
    if (!profile_url.value) return;
    navigator.clipboard.writeText(profile_url.value);
    copied.value = true;
    setTimeout(() => {
      copied.value = false;
    }, 2000);
  }

  onMounted(async () => {
    try {
      const response = await getShopInfo();
      shop.value = response.data.data;
      particleInterval = setInterval(createParticle, 500);
    } catch (error) {
      console.error('매장 정보 조회에 실패했습니다:', error);
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
  <div v-if="isLoading" class="loading-container">
    <div class="spinner-emoji">✨</div>
  </div>
  <div v-else-if="shop" class="profile-link-layout">
    <div class="background-blob"></div>
    <div class="profile-url-box">
      <div class="profile-url-label">내 프로필 링크</div>
      <div class="profile-url-row">
        <input class="profile-url-input" :value="profile_url" readonly />
        <button class="copy-btn" :class="{ success: copied }" @click="copyProfileUrl">
          <span class="copy-btn-content">
            <Check v-if="copied" :size="16" />
            <Link v-else :size="16" />
            <span>{{ copied ? '복사 완료!' : '프로필 링크 복사' }}</span>
          </span>
        </button>
      </div>
    </div>
    <div class="profile-link-view">
      <!-- 흩날리는 이모지 효과 컨테이너 -->
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
          <button class="icon-btn" aria-label="전화 걸기" @click="callShop">
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
  </div>
  <div v-else class="error-container">매장 정보를 불러올 수 없습니다.</div>
</template>

<style scoped>
  @import url('https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css');

  .loading-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    background-color: #f4f7ff;
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

  .profile-link-layout {
    font-family: 'Pretendard', sans-serif;
    position: relative;
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    align-items: start;
    gap: 32px;
    padding: 60px 24px;
    width: 100%;
    min-height: 100vh;
    max-width: 1200px;
    margin: 0 auto;
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

  .profile-url-box {
    grid-column: 1 / 2;
    justify-self: end;
    width: 100%;
    max-width: 320px;
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    padding: 24px;
    display: flex;
    flex-direction: column;
    z-index: 2;
    border: 1px solid rgba(255, 255, 255, 0.2);
  }
  .profile-url-label {
    font-size: 16px;
    font-weight: 600;
    color: #333d4b;
    margin-bottom: 12px;
  }
  .profile-url-row {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .profile-url-input {
    width: 100%;
    font-size: 14px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 10px 14px;
    color: #4c5a69;
    background: #f8fafc;
    outline: none;
    transition: box-shadow 0.2s;
  }
  .profile-url-input:focus {
    box-shadow: 0 0 0 2px rgba(123, 97, 255, 0.4);
    border-color: #a493ff;
  }
  .copy-btn {
    background: #7b61ff;
    color: #fff;
    border: none;
    border-radius: 8px;
    padding: 10px 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease-out;
    position: relative;
  }
  .copy-btn:hover {
    background: #6a50e0;
    transform: translateY(-2px);
    box-shadow: 0 4px 15px rgba(123, 97, 255, 0.3);
  }
  .copy-btn.success {
    background: #10b981;
  }
  .copy-btn-content {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
  }

  .profile-link-view {
    position: relative;
    overflow: hidden;
    grid-column: 2 / 3;
    width: 100%;
    max-width: 400px;
    min-width: 340px;
    background: rgba(255, 255, 255, 0.65);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 28px;
    box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.1);
    padding: 40px 28px;
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
  }

  .shop-header {
    text-align: center;
    margin-bottom: 20px;
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
    padding: 10px 16px;
    border-radius: 12px;
    white-space: pre-wrap;
    text-align: center;
    line-height: 1.6;
    border: 1px solid rgba(0, 0, 0, 0.05);
  }
  .shop-address-group {
    margin-bottom: 24px;
    text-align: center;
    color: #6a7482;
    font-size: 15px;
    line-height: 1.5;
  }
  .icon-row {
    margin-bottom: 28px;
    display: flex;
    justify-content: center;
  }
  .icon-btn {
    background: #fff;
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 50%;
    width: 56px;
    height: 56px;
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
    transition: transform 0.3s;
  }
  .icon-btn:hover .phone-icon {
    animation: wiggle 0.5s ease-in-out;
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
    gap: 16px;
    width: 100%;
  }
  .main-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    width: 100%;
    padding: 15px 0;
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
    .profile-link-layout {
      grid-template-columns: 1fr;
      gap: 32px;
      padding: 40px 16px;
    }
    .profile-url-box,
    .profile-link-view {
      grid-column: 1 / -1;
      justify-self: center;
    }
    .background-blob {
      width: 300px;
      height: 300px;
      filter: blur(80px);
    }
  }
</style>
