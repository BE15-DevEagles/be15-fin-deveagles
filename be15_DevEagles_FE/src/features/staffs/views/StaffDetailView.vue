<template>
  <div class="staff-detail-view">
    <h2>직원 정보 설정</h2>

    <div v-if="staff" class="staff-detail-layout">
      <div class="form-column">
        <StaffForm
          ref="staffFormRef"
          v-model="staff"
          v-model:file="profileFile"
          v-model:preview="profilePreview"
        />
      </div>
      <div v-if="permission" class="permission-column">
        <StaffPermission v-model="staff.permissions" />
      </div>
    </div>

    <!-- 저장 버튼 -->
    <div class="button-area">
      <BaseButton @click="handleSave">저장하기</BaseButton>
    </div>
  </div>
  <BaseToast ref="toastRef" />
</template>

<script setup>
  import { onMounted, ref } from 'vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import StaffForm from '@/features/staffs/components/StaffForm.vue';
  import StaffPermission from '@/features/staffs/components/StaffPermission.vue';
  import { useRouter } from 'vue-router';
  import { getStaffDetail, putStaffDetail } from '@/features/staffs/api/staffs.js';
  import BaseToast from '@/components/common/BaseToast.vue';
  import dayjs from 'dayjs';

  const props = defineProps({
    staffId: [String, Number],
  });

  const router = useRouter();
  const toastRef = ref();

  const staffFormRef = ref();
  const originalStaff = ref(null);
  const staff = ref(null);
  const permission = ref(null);
  const profileFile = ref(null);
  const profilePreview = ref('');

  const fetchStaffDetail = async () => {
    try {
      const res = await getStaffDetail(props.staffId);
      staff.value = res.data.data;
      originalStaff.value = JSON.parse(JSON.stringify(res.data.data)); // 깊은 복사
      profilePreview.value = res.data.data.profileUrl;
    } catch (err) {
      const message = err?.message || '존재하지 않는 직원입니다.';
      toastRef.value?.error?.(message);
      setTimeout(() => {
        router.push('/settings/staff');
      }, 1000);
    }
  };

  onMounted(() => {
    fetchStaffDetail();
  });

  const handleSave = async () => {
    const isValid = staffFormRef.value?.validate?.();
    if (!isValid) {
      toastRef.value?.error?.('입력 값을 확인해주세요.');
      return;
    }

    const current = staff.value;
    const origin = originalStaff.value;
    const change = {};

    const isDifferent = (key, formatter = v => v) =>
      formatter(current[key]) !== formatter(origin[key]) ? current[key] : origin[key];

    change.staffName = isDifferent('staffName');
    change.email = isDifferent('email');
    change.phoneNumber = isDifferent('phoneNumber');
    change.grade = isDifferent('grade');
    change.description = isDifferent('description');
    change.colorCode = isDifferent('colorCode');

    const formatDate = date => {
      const d = dayjs(date);
      return d.isValid() ? d.format('YYYY-MM-DD') : null;
    };

    change.joinedDate =
      formatDate(current.joinedDate) !== formatDate(origin.joinedDate)
        ? formatDate(current.joinedDate)
        : formatDate(origin.joinedDate);

    change.leftDate = current.working === 'true' ? null : formatDate(current.leftDate);

    change.permissions = current.permissions || [];

    const formData = new FormData();
    formData.append(
      'staffRequest',
      new Blob([JSON.stringify(change)], { type: 'application/json' })
    );

    if (profileFile.value !== null) {
      formData.append('profile', profileFile.value);
    } else if (!profilePreview.value && originalStaff.value.profileUrl) {
      const emptyFile = new File([''], 'delete.png', { type: 'image/png' });
      formData.append('profile', emptyFile);
    }

    try {
      const { staffId } = props;
      await putStaffDetail({
        staffId,
        formData,
      });
      toastRef.value?.success?.('직원 정보가 수정되었습니다.');
      await fetchStaffDetail();
    } catch (err) {
      const message = err?.message || '직원 정보 수정에 실패했습니다.';
      toastRef.value?.error?.(message);
    }
  };
</script>

<style scoped>
  .staff-detail-layout {
    display: flex;
    gap: 24px;
    align-items: flex-start;
  }
  .form-column {
    flex: 1;
    min-width: 0;
  }
  .permission-column {
    width: 300px; /* or 350px, 너네 권한 칸 사이즈에 따라 */
    min-width: 280px;
  }
  .staff-detail-view {
    max-width: 720px;
    margin: 0 auto;
    padding: 32px 24px;
    background-color: #fff;
    border-radius: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  }
  h2 {
    font-size: 20px;
    margin-bottom: 24px;
  }
  .button-area {
    margin-top: 32px;
    display: flex;
    justify-content: flex-end;
  }
</style>
