<template>
  <div class="designer-card">
    <div class="tags">
      <span class="tag">디자이너</span>
      <span class="tag">{{ name }}</span>
    </div>
    <div class="content">
      <img class="profile" :src="image" alt="디자이너 이미지" />
      <div class="description">
        <p>정성껏 시술하는 {{ name }} 디자이너입니다.</p>
      </div>
    </div>
    <BaseButton class="reserve-button" @click="onReserveClick">예약</BaseButton>
  </div>
</template>

<script setup>
  import { useRouter } from 'vue-router';
  import BaseButton from '@/components/common/BaseButton.vue';

  const props = defineProps({
    id: [String, Number],
    shopId: [String, Number],
    name: String,
    image: String,
  });

  const router = useRouter();

  const onReserveClick = () => {
    router.push(`/reserve/${props.shopId}/staff/${props.id}`);
  };
</script>
<style scoped>
  .designer-card {
    width: 400px;
    height: 250px;
    border: 1px solid var(--color-gray-300);
    border-radius: 8px;
    padding: 12px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
    background-color: var(--color-neutral-white);
  }

  .tags {
    display: flex;
    gap: 6px;
  }

  .tag {
    background-color: var(--color-gray-100);
    padding: 4px 8px;
    border-radius: 6px;
    color: var(--color-text-primary);
    font-weight: 600;
    font-size: 18px;
  }

  .content {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    flex: 1;
  }

  .profile {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    object-fit: cover;
    border: 1px solid var(--color-gray-300);
  }

  .description {
    flex: 1;
    background-color: var(--color-gray-50);
    padding: 12px;
    border-radius: 6px;
    font-size: 15px;
    color: var(--color-text-primary);
    border: 1px solid var(--color-gray-200);
    box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.03);
  }

  .reserve-button {
    background-color: var(--color-primary-600);
    color: var(--color-neutral-white);
    border: none;
    border-radius: 6px;
    margin-top: auto;
    padding: 12px 0;
    font-size: 15px;
    font-weight: bold;
    cursor: pointer;
  }
</style>
