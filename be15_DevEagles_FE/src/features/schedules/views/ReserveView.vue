<template>
  <div class="page-wrapper">
    <h1 class="page-title">
      <img src="@/images/suri/team_logo-cutout.png" class="logo-img" alt="로고" />
      예약 페이지
    </h1>

    <div class="reservation-wrapper">
      <!-- 고객 정보 입력 -->
      <h3 class="section-title">👤 고객 정보를 입력해주세요</h3>
      <div class="input-section">
        <div class="form-group">
          <label for="customer">이름</label>
          <input
            id="customer"
            v-model="form.customer"
            type="text"
            placeholder="이름을 입력해주세요"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label for="phone">전화번호</label>
          <input
            id="phone"
            v-model="phoneInput"
            type="tel"
            inputmode="numeric"
            pattern="[0-9]*"
            placeholder="연락 가능한 번호를 입력해주세요"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label for="memo">메모</label>
          <input
            id="memo"
            v-model="form.memo"
            type="text"
            placeholder="요청 사항이 있다면 적어주세요"
            class="form-input"
          />
        </div>
      </div>

      <div class="main-section">
        <!-- 왼쪽: 날짜 & 시간 -->
        <div class="left-section">
          <h3>📅 날짜와 시간을 선택해 주세요</h3>
          <PrimeDatePicker
            v-model="form.date"
            :inline="true"
            :show-time="false"
            :min-date="new Date()"
            :show-icon="false"
          />
          <h4>오전</h4>
          <div class="time-grid">
            <BaseButton
              v-for="t in times.am"
              :key="t"
              :outline="form.time !== t"
              @click="selectTime(t)"
            >
              {{ t }}
            </BaseButton>
          </div>
          <h4>오후</h4>
          <div class="time-grid">
            <BaseButton
              v-for="t in times.pm"
              :key="t"
              :outline="form.time !== t"
              @click="selectTime(t)"
            >
              {{ t }}
            </BaseButton>
          </div>
        </div>

        <!-- 오른쪽: 시술 -->
        <div class="right-section">
          <h3>💇 시술 메뉴를 선택해 주세요</h3>
          <div class="menu-tabs">
            <BaseButton
              v-for="menu in menus"
              :key="menu"
              :outline="form.menu !== menu"
              @click="selectMenu(menu)"
            >
              {{ menu }}
            </BaseButton>
          </div>

          <div class="service-box">
            <div
              v-for="item in serviceOptions"
              :key="item"
              class="service-item"
              :class="{ selected: form.services.includes(item) }"
              @click="toggleService(item)"
            >
              {{ item }}
            </div>
          </div>

          <div class="submit-area">
            <BaseButton type="primary" :disabled="!isValid" @click="submitReservation">
              예약하기
            </BaseButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { reactive, computed, watch, ref } from 'vue';
  import { useRoute } from 'vue-router';
  import { useToast } from 'vue-toastification';
  import BaseButton from '@/components/common/BaseButton.vue';
  import PrimeDatePicker from '@/components/common/PrimeDatePicker.vue';

  const toast = useToast();
  const route = useRoute();
  const phoneInput = ref('');

  const form = reactive({
    designerId: route.params.id,
    customer: '',
    phone: '',
    memo: '',
    date: null,
    time: '',
    menu: '커트',
    services: [],
  });

  const activeMenu = ref('커트');

  const times = {
    am: ['10:00', '10:30', '11:00', '11:30'],
    pm: [
      '12:00',
      '12:30',
      '13:00',
      '13:30',
      '14:00',
      '14:30',
      '15:00',
      '15:30',
      '16:00',
      '16:30',
      '17:00',
      '17:30',
    ],
  };

  const menus = ['커트', '펌', '클리닉', '컬러'];

  const serviceOptionsMap = {
    커트: ['남성 커트', '여성 커트', '샴푸', '(미취학) 아동컷', '병지 커트'],
    펌: ['베이직 펌', '볼륨 펌'],
    클리닉: ['모발 케어', '두피 케어'],
    컬러: ['전체 염색', '부분 염색'],
  };

  const serviceOptions = computed(() => serviceOptionsMap[activeMenu.value] || []);

  const selectMenu = menu => {
    activeMenu.value = menu;
    form.menu = menu;
  };

  const toggleService = item => {
    const idx = form.services.indexOf(item);
    if (idx === -1) form.services.push(item);
    else form.services.splice(idx, 1);
  };

  const selectTime = time => {
    form.time = time;
  };

  watch(phoneInput, val => {
    let digits = val.replace(/\D/g, '');
    if (val !== digits) toast.warning('숫자만 입력 가능합니다.');
    if (digits.length > 11) {
      digits = digits.slice(0, 11);
      toast.warning('전화번호는 최대 11자리까지 입력 가능합니다.');
    }

    let formatted = '';
    if (digits.length < 4) formatted = digits;
    else if (digits.length < 7) formatted = `${digits.slice(0, 3)}-${digits.slice(3)}`;
    else formatted = `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
    phoneInput.value = formatted;
    form.phone = formatted;
  });

  const formatDateOnly = date => {
    const d = new Date(date);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  };

  const isValid = computed(() => {
    return (
      form.customer.trim() &&
      /^\d{3}-\d{3,4}-\d{4}$/.test(form.phone) &&
      form.date &&
      form.time &&
      form.menu &&
      form.services.length > 0
    );
  });

  const submitReservation = () => {
    if (!isValid.value) {
      toast.warning('모든 필수 항목을 입력해주세요.');
      return;
    }

    const payload = {
      ...form,
      date: form.date ? formatDateOnly(form.date) : '',
    };

    toast.success('예약 정보가 정상적으로 준비되었습니다.');
    alert('예약 정보:\n' + JSON.stringify(payload, null, 2));
  };
</script>

<style scoped>
  .page-wrapper {
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
    padding-left: 132px;
  }

  .logo-img {
    height: 60px;
    width: auto;
  }

  .reservation-wrapper {
    background-color: var(--color-neutral-white);
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
    padding: 32px;
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .section-title {
    font-size: 20px;
    font-weight: 600;
    margin: 12px 0 8px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .input-section {
    display: grid;
    grid-template-columns: 1fr 1fr 2fr;
    gap: 8px 16px;
  }

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .form-input {
    padding: 10px;
    border: 1px solid var(--color-gray-300);
    border-radius: 6px;
    font-size: 14px;
    background-color: var(--color-neutral-white);
    color: var(--color-text-primary);
  }

  .form-input:focus {
    outline: none;
    border-color: var(--color-primary-main);
    box-shadow: 0 0 0 2px rgba(54, 79, 107, 0.15);
  }

  .main-section {
    display: flex;
    justify-content: space-between;
    gap: 48px;
  }

  .left-section,
  .right-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .time-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
    margin-top: 8px;
    margin-bottom: 16px;
  }

  .left-section h4 {
    margin-top: 4px;
    margin-bottom: 4px;
  }

  .menu-tabs {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .service-box {
    border: 1px solid var(--color-gray-300);
    padding: 12px;
    border-radius: 8px;
    background-color: var(--color-neutral-white);
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-height: 325px;
    overflow-y: auto;
  }

  .service-item {
    padding: 12px;
    border-radius: 6px;
    cursor: pointer;
    transition: background-color 0.2s ease;
    border: 1px solid transparent;
    background-color: transparent;
    color: var(--color-text-primary);
  }

  .service-item:hover {
    background-color: var(--color-gray-100);
  }

  .service-item.selected {
    background-color: var(--color-primary-main);
    color: var(--color-neutral-white);
    font-weight: bold;
  }

  .submit-area {
    margin-top: auto;
    padding-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
</style>
