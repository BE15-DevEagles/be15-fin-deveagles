<script setup>
  import { ref, onMounted } from 'vue';
  import BaseCard from '@/components/common/BaseCard.vue';
  import BaseButton from '@/components/common/BaseButton.vue';
  import BaseToast from '@/components/common/BaseToast.vue';
  import SenderInfoModal from '@/features/messages/components/modal/SenderInfoModal.vue';
  import AlimtalkConfirmModal from '@/features/messages/components/modal/AlimtalkConfirmModal.vue';
  import PointChargeModal from '@/features/messages/components/modal/PointChargeModal.vue';
  import AutoSendSettingModal from '@/features/messages/components/modal/AutoSendSettingModal.vue';

  import messagesAPI from '@/features/messages/api/message.js';

  const sender = ref('');
  const useAlimtalk = ref(false);
  const messagePoints = ref(0);

  const showSenderModal = ref(false);
  const showAlimtalkModal = ref(false);
  const showChargeModal = ref(false);
  const showAutoSendModal = ref(false);

  const toastRef = ref();

  const autoSendItems = ref([
    {
      label: '신규 고객 등록',
      messages: [
        {
          message: '환영합니다 고객님',
          enabled: true,
          type: '알림톡',
          sendTime: 'immediate',
        },
      ],
    },
    {
      label: '예약 등록',
      messages: [
        {
          message: '예약이 등록되었습니다',
          enabled: false,
          type: '알림톡',
          sendTime: '1min',
        },
      ],
    },
    {
      label: '횟수권 차감',
      messages: [
        {
          message: '횟수권이 차감되었습니다',
          enabled: true,
          type: 'SMS',
          sendTime: 'immediate',
        },
      ],
    },
    {
      label: '선불권 차감',
      messages: [
        {
          message: '선불권이 사용되었습니다',
          enabled: false,
          type: 'SMS',
          sendTime: '5min',
        },
      ],
    },
  ]);

  async function fetchSettings() {
    try {
      const settings = await messagesAPI.getMessageSettings();

      if (!settings) {
        toastRef.value?.error?.('문자 설정이 존재하지 않습니다. 먼저 등록해주세요.');

        return;
      }

      sender.value = settings.senderNumber || '';
      useAlimtalk.value = settings.canAlimtalk || false;
      messagePoints.value = settings.point || 0;
    } catch (err) {
      const errorMessage =
        err?.response?.data?.message || err?.message || '알 수 없는 오류가 발생했습니다.';

      toastRef.value?.error?.(errorMessage);
    }
  }
  async function updateSettings(payload) {
    try {
      await messagesAPI.updateMessageSettings(payload);
      toastRef.value?.addToast({ message: '설정이 저장되었습니다.', type: 'success' });
    } catch (err) {
      toastRef.value?.addToast({ message: err.message, type: 'error' });
    }
  }

  function confirmSender(number) {
    sender.value = number;
    updateSettings({ senderNumber: number });
    showSenderModal.value = false;
  }

  function confirmAlimtalk() {
    useAlimtalk.value = true;
    updateSettings({ canAlimtalk: true });
    showAlimtalkModal.value = false;
  }

  function handleConfirmCharge(amount) {
    messagePoints.value += amount;
    updateSettings({ point: amount });
    showChargeModal.value = false;
  }

  onMounted(fetchSettings);
</script>

<template>
  <section class="settings-page">
    <BaseCard class="card-wrapper shadow-drop">
      <div class="setting-header">
        <h2 class="page-title">문자 설정</h2>
        <p class="page-sub">서비스 이용에 필요한 기본 정보를 설정하세요.</p>
      </div>

      <div class="setting-group">
        <!-- 발신 번호 -->
        <div class="setting-item">
          <div class="setting-status-row">
            <div class="setting-title-group">
              <h3 class="setting-title with-underline">발신 번호</h3>
              <span class="status-chip" :class="sender ? 'success' : 'error'">
                {{ sender || '미등록' }}
              </span>
            </div>
            <BaseButton class="action-button sm" type="primary" @click="showSenderModal = true">
              번호 등록
            </BaseButton>
          </div>
          <div class="setting-info">
            <p class="setting-description text-sub">
              문자 또는 알림톡 발신 시 사용될 기본 번호입니다.
            </p>
          </div>
        </div>

        <hr class="section-divider" />

        <!-- 알림톡 신청 -->
        <div class="setting-item">
          <div class="setting-status-row">
            <div class="setting-title-group">
              <h3 class="setting-title with-underline">알림톡 신청</h3>
              <span class="status-chip" :class="useAlimtalk ? 'success' : 'error'">
                {{ useAlimtalk ? '신청 완료' : '미신청' }}
              </span>
            </div>
            <BaseButton
              class="action-button sm"
              type="primary"
              :disabled="useAlimtalk"
              @click="showAlimtalkModal = true"
            >
              알림톡 신청
            </BaseButton>
          </div>
        </div>

        <hr class="section-divider" />

        <!-- 문자 포인트 -->
        <div class="setting-item">
          <div class="setting-status-row">
            <div class="setting-title-group">
              <h3 class="setting-title with-underline">문자 포인트</h3>
              <span class="status-chip neutral">{{ messagePoints.toLocaleString() }}P</span>
            </div>
            <BaseButton class="action-button sm" type="primary" @click="showChargeModal = true">
              포인트 충전
            </BaseButton>
          </div>
        </div>

        <hr class="section-divider" />

        <!-- 자동 발신 설정 -->
        <div class="setting-item no-border">
          <div class="setting-status-row">
            <div class="setting-title-group">
              <h3 class="setting-title with-underline">자동 발신 설정</h3>
            </div>
            <BaseButton class="action-button sm" type="primary" @click="showAutoSendModal = true">
              설정 변경
            </BaseButton>
          </div>
        </div>
      </div>
    </BaseCard>

    <!-- 모달 및 토스트 -->
    <SenderInfoModal v-model="showSenderModal" @confirm="confirmSender" />
    <AlimtalkConfirmModal v-model="showAlimtalkModal" @confirm="confirmAlimtalk" />
    <PointChargeModal v-model="showChargeModal" @confirm="handleConfirmCharge" />
    <AutoSendSettingModal v-model="showAutoSendModal" v-model:items="autoSendItems" />
    <BaseToast ref="toastRef" />
  </section>
</template>

<style scoped>
  @import '@/assets/base.css';
  @import '@/assets/css/components.css';
  @import '@/assets/css/styleguide.css';
  @import '@/assets/css/tooltip.css';

  .settings-page {
    padding: 2.5rem 0;
    display: flex;
    justify-content: center;
    background-color: #f9fafb;
  }

  .card-wrapper {
    width: 100%;
    max-width: 600px;
    padding: 2.5rem 2rem;
    border-radius: 16px;
    background-color: #ffffff;
  }

  .setting-header {
    margin-bottom: 2rem;
    text-align: center;
  }

  .page-title {
    font-size: 1.5rem;
    font-weight: 700;
    color: #111827;
  }

  .page-sub {
    font-size: 0.875rem;
    color: #6b7280;
    margin-top: 0.25rem;
  }

  .setting-item {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .setting-item.no-border {
    padding-bottom: 0;
  }

  .setting-status-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.5rem;
  }

  .setting-title.with-underline {
    font-weight: 600;
    font-size: 1rem;
    color: #111827;
    padding-bottom: 0.25rem;
    border-bottom: 1px solid #e5e7eb;
    flex-shrink: 0;
  }

  .setting-title-group {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-grow: 1;
    min-width: 0;
  }

  .status-chip {
    display: inline-block;
    padding: 4px 10px;
    font-size: 12px;
    border-radius: 9999px;
    font-weight: 500;
    white-space: nowrap;
  }

  .status-chip.success {
    background-color: #dcfce7;
    color: #166534;
  }

  .status-chip.error {
    background-color: #fee2e2;
    color: #b91c1c;
  }

  .status-chip.neutral {
    background-color: #f3f4f6;
    color: #374151;
  }

  .action-button.sm {
    padding: 6px 12px;
    font-size: 13px;
    line-height: 1.2;
    white-space: nowrap;
    height: auto;
    flex-shrink: 0;
    min-width: 100px;
  }

  .section-divider {
    border: none;
    border-top: 1px solid #e5e7eb;
    margin: 2rem 0 0.5rem;
  }
</style>
