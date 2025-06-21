<template>
  <div class="designer-list-page">
    <h1 class="page-title">
      <img src="@/images/suri/team_logo-cutout.png" class="logo-img" alt="로고" />
      예약할 디자이너를 선택해주세요
    </h1>

    <div class="designer-list-wrapper">
      <div class="designer-list">
        <DesignerCard
          v-for="(designer, index) in designers"
          :key="index"
          :name="designer.name"
          :image="designer.image"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
  import img1 from '@/images/suri/designer-profile1.png';
  import img2 from '@/images/suri/designer-profile5.png';
  import img3 from '@/images/suri/designer-profile3.png';
  import img4 from '@/images/suri/designer-profile4.png';
  import img5 from '@/images/suri/designer-profile2.png';
  import img6 from '@/images/suri/designer-profile6.png';
  import DesignerCard from '@/features/schedules/components/DesignerCard.vue';

  const designers = [
    { name: '김이슬 1', image: img1 },
    { name: '김이슬 2', image: img2 },
    { name: '김이슬 3', image: img3 },
    { name: '김이슬 4', image: img4 },
    { name: '김이슬 5', image: img5 },
    { name: '김이슬 6', image: img6 },
  ];
</script>

<style scoped>
  .designer-list-page {
    padding: 32px 40px;
    background-color: var(--color-gray-50);
    min-height: 100vh;
    box-sizing: border-box;
  }

  .page-title {
    font-size: 40px;
    font-weight: bold;
    margin-bottom: 30px;
    color: var(--color-text-primary);
    text-align: left;
    display: flex;
    align-items: center;
    gap: 12px;
    padding-left: 30px;
  }

  .logo-img {
    height: 60px;
    width: auto;
  }

  .designer-list-wrapper {
    background-color: var(--color-neutral-white);
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
    padding: 32px;
    max-width: 1400px;
    margin: 0 auto;
  }

  .designer-list {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 24px 32px;
    justify-items: center;
  }
</style>
