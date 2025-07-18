<template>
  <div class="mypage-container">
    <div v-if="isLoading">로딩 중 ...</div>
    <div v-else class="profile-card-vertical">
      <div class="form-fields">
        <div class="profile-section">
          <ProfileUpload v-model="profilePreview" v-model:file="profileFile" />

          <div class="color-picker-inline">
            <PickColors v-model:value="staff.colorCode" class="color-picker" />

            <div class="tooltip-wrapper">
              <InfoTooltipIcon />
              <div class="tooltip-bubble">대표 색상을 변경할 수 있어요</div>
            </div>
          </div>
        </div>

        <div class="label-row">
          <label for="userName">이름</label>
        </div>
        <BaseForm id="userName" v-model="staff.staffName" type="text" />

        <div class="label-row">
          <label for="grade">직급</label>
        </div>
        <BaseForm id="grade" v-model="staff.grade" type="text" />

        <div class="label-row">
          <label for="grade">메모</label>
        </div>
        <BaseForm v-model="staff.description" type="textarea" :rows="4" />

        <div class="button-row">
          <BaseButton class="edit-button" type="primary" @click="handleEdit">
            변경사항 저장
          </BaseButton>
        </div>
      </div>
    </div>
    <BaseToast ref="toastRef" />
  </div>
</template>
<script setup>
  import ProfileUpload from '@/features/users/components/ProfileUpload.vue';
  import BaseForm from '@/components/common/BaseForm.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import { onMounted, ref } from 'vue';
  import PickColors from 'vue-pick-colors';
  import { getProfile, patchProfile } from '@/features/users/api/users.js';
  import { useAuthStore } from '@/store/auth.js';
  import BaseToast from '@/components/common/BaseToast.vue';
  import InfoTooltipIcon from '@/components/icons/InfoTooltipIcon.vue';

  const authStore = useAuthStore();
  const toastRef = ref();
  const isLoading = ref(true);
  const originalStaff = ref({});
  const staff = ref({
    staffName: '',
    grade: '',
    profileUrl: '',
    colorCode: '',
    description: '',
  });
  const profilePreview = ref(staff.value.profileUrl || '');
  const profileFile = ref(null);

  onMounted(async () => {
    try {
      const res = await getProfile();
      const { profileUrl, colorCode, description } = res.data.data;
      profilePreview.value = profileUrl;
      originalStaff.value = { profileUrl, colorCode, description };
      staff.value = {
        staffName: authStore.staffName,
        grade: authStore.grade,
        profileUrl,
        colorCode,
        description,
      };
    } catch (err) {
      handleError(err);
    } finally {
      isLoading.value = false;
    }
  });

  const handleEdit = async () => {
    const request = {
      staffName: staff.value.staffName,
      grade: staff.value.grade,
      description: staff.value.description,
      colorCode: staff.value.colorCode,
    };

    const formData = new FormData();
    formData.append(
      'profileRequest',
      new Blob([JSON.stringify(request)], { type: 'application/json' })
    );

    if (profileFile.value !== null) {
      formData.append('profile', profileFile.value);
    } else if (!profilePreview.value) {
      const emptyFile = new File([''], 'delete.png', { type: 'image/png' });
      formData.append('profile', emptyFile);
    }

    try {
      await patchProfile(formData);
      toastRef.value?.success?.('프로필이 성공적으로 수정되었어요!');
    } catch (err) {
      handleError(err);
    }
  };

  const handleError = err => {
    const res = err.response?.data;
    if (!res || !res.message) {
      toastRef.value?.error?.('존재하지 않는 회원입니다');
      return;
    }
    toastRef.value?.error?.(res.message);
  };
</script>
<style scoped>
  .profile-card-vertical {
    display: flex;
    flex-direction: column;
    align-items: center;
    border: 1px solid var(--color-gray-200);
    padding: 40px 24px;
    border-radius: 16px;
    background-color: #fff;
    width: 100%;
    max-width: 400px;
    margin: 0 auto;
    font-family: 'Noto Sans KR', sans-serif;
    color: var(--color-gray-800);
  }
  .form-fields {
    width: 360px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .label-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 4px;
    font-weight: 600;
    font-size: 14px;
    color: var(--color-gray-900);
  }
  .edit-button {
    margin-top: 32px;
    width: 100%;
  }
  .mypage-container {
    padding: 40px 20px;
    max-width: 800px;
    margin: 0 auto;
  }

  .profile-section {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .color-picker {
    margin-top: 4px;
    z-index: 10;
  }
  .color-picker-inline {
    display: flex;
    align-items: center;
    gap: 0;
  }

  .tooltip-wrapper {
    position: relative;
    display: flex;
    align-items: center;
    margin-left: 0px;
    transform: translateX(-0.5px);
  }

  .tooltip-icon {
    width: 16px;
    height: 16px;
    color: #9ca3af;
    cursor: default;
  }

  .tooltip-bubble {
    position: absolute;
    bottom: 125%;
    left: 50%;
    transform: translateX(-50%);
    background-color: #333;
    color: white;
    padding: 6px 8px;
    border-radius: 6px;
    font-size: 12px;
    white-space: nowrap;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.2s ease;
    z-index: 10;
  }

  .tooltip-wrapper:hover .tooltip-bubble {
    opacity: 1;
    pointer-events: auto;
  }
</style>
