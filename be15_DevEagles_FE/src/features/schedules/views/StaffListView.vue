<template>
  <div class="staff-list-page">
    <h1 class="page-title">
      <img src="@/images/suri/team_logo-cutout.png" class="logo-img" alt="로고" />
      예약할 디자이너를 선택해주세요
    </h1>

    <div class="staff-list-wrapper">
      <div v-if="staffs.length > 0" class="staff-list">
        <StaffCard
          v-for="(staff, index) in staffs"
          :id="staff.staffId"
          :key="index"
          :name="staff.name"
          :image="staff.image"
          :shop-id="shopId"
        />
      </div>
      <div v-else class="no-staff">재직 중인 직원이 없습니다.</div>
    </div>
  </div>
</template>

<script setup>
  import { ref, onMounted } from 'vue';
  import StaffCard from '@/features/schedules/components/StaffCard.vue';
  import { useRoute } from 'vue-router';
  import { getStaffList, getStaffDetail } from '@/features/schedules/api/schedules.js';

  const route = useRoute();
  const shopId = route.params.shopId;
  const staffs = ref([]);

  const fetchStaffList = async () => {
    try {
      const res = await getStaffList({ isActive: true });

      const staffList = res.map(staff => ({
        staffId: staff.staffId,
        name: staff.staffName,
        image: '',
      }));

      const detailPromises = staffList.map(async item => {
        try {
          const detailRes = await getStaffDetail(item.staffId);
          item.image = detailRes.profileUrl;
        } catch (err) {
          console.error(`스태프(${item.staffId}) 상세 조회 실패:`, err);
        }
        return item;
      });

      staffs.value = await Promise.all(detailPromises);
    } catch (e) {
      console.error('담당자 목록 조회 실패:', e);
    }
  };

  onMounted(() => {
    fetchStaffList();
  });
</script>

<style scoped>
  .staff-list-page {
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

  .staff-list-wrapper {
    background-color: var(--color-neutral-white);
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
    padding: 32px;
    max-width: 1400px;
    margin: 0 auto;
  }

  .staff-list {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 24px 32px;
    justify-items: center;
  }
</style>
