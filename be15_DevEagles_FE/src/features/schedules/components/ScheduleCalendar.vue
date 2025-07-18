<script setup>
  import { watch, ref, nextTick, onMounted, onBeforeUnmount } from 'vue';
  import FullCalendar from '@fullcalendar/vue3';
  import interactionPlugin from '@fullcalendar/interaction';
  import resourceTimeGridPlugin from '@fullcalendar/resource-timegrid';
  import dayGridPlugin from '@fullcalendar/daygrid';
  import timeGridPlugin from '@fullcalendar/timegrid';
  import koLocale from '@fullcalendar/core/locales/ko';
  import tippy from 'tippy.js';
  import 'tippy.js/dist/tippy.css';
  import dayjs from 'dayjs';

  let resizeObserver;

  onMounted(() => {
    const el = calendarRef.value?.$el || calendarRef.value?.el || calendarRef.value;
    if (el) {
      resizeObserver = new ResizeObserver(() => {
        calendarRef.value?.getApi().updateSize();
      });
      resizeObserver.observe(el);
    }
  });

  onBeforeUnmount(() => {
    resizeObserver?.disconnect();
  });

  const props = defineProps({
    schedules: {
      type: Array,
      default: () => [],
    },
    sidebarCollapsedTrigger: {
      type: [Number, String],
      default: 0,
    },
  });

  const calendarRef = ref(null);

  const emit = defineEmits(['clickSchedule', 'change-date-range']);

  function extractDateRangeFromView(view) {
    const start = dayjs(view.currentStart).format('YYYY-MM-DD');
    const end = dayjs(view.currentEnd).subtract(1, 'day').format('YYYY-MM-DD');
    return { from: start, to: end };
  }

  const statusColors = {
    CONFIRMED: 'var(--color-success-100)',
    PENDING: 'var(--color-warning-200)',
    PAID: 'var(--color-primary-50)',
    NO_SHOW: 'var(--color-error-300)',
    CBC: 'var(--color-error-300)',
    CBS: 'var(--color-error-300)',
  };
  const statusLabels = {
    CONFIRMED: '예약 확정',
    PENDING: '예약 대기',
    NO_SHOW: '노쇼',
    CBC: '예약 취소(고객)',
    CBS: '예약 취소(샵)',
    PAID: '결제 완료',
  };
  const calendarOptions = ref({
    plugins: [interactionPlugin, resourceTimeGridPlugin, dayGridPlugin, timeGridPlugin],
    initialView: 'dayGridMonth',
    editable: false,
    eventStartEditable: false,
    eventDurationEditable: false,
    locale: koLocale,
    resources: [],
    slotMinTime: '09:00:00',
    slotMaxTime: '21:00:00',
    resourceOrder: 'customOrder',
    height: 'auto',

    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'resourceTimeGridDay,dayGridMonth',
    },
    views: {
      resourceTimeGridDay: {
        type: 'resourceTimeGrid',
        buttonText: '일',
      },
      timeGridWeek: {
        type: 'timeGrid',
        buttonText: '주',
      },
      dayGridMonth: {
        type: 'dayGridMonth',
        buttonText: '월',
      },
    },

    eventContent({ event, view }) {
      const type = event.extendedProps.type;
      const time = event.start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

      const showTime = !(type === 'leave' || type === 'regular_leave');
      const viewType = view.type;

      return {
        html: `
      <div class="custom-event-style ${type}">
        ${
          type === 'reservation' || type === 'leave' || type === 'regular_leave'
            ? '<div class="left-bar"></div>'
            : ''
        }
        <div class="text">
          ${showTime ? `<strong>${time}</strong>&nbsp;` : ''}
          ${event.title}
        </div>
      </div>
    `,
      };
    },

    eventClick: info => {
      emit('clickSchedule', {
        id: info.event.id,
        type: info.event.extendedProps.type,
      });
    },

    eventDidMount(info) {
      const { event, el, view } = info;
      const { type, status, staffName, customer, service, timeRange, memo, staffColor } =
        event.extendedProps;
      const viewType = view.type;
      const borderColor = staffColor || 'var(--color-gray-500)';
      const bgColor =
        type === 'reservation'
          ? statusColors[status] || 'var(--color-info-50)'
          : type === 'leave' || type === 'regular_leave'
            ? 'rgba(220, 38, 38, 0.3)'
            : 'var(--color-neutral-white)';

      const box = el.querySelector('.custom-event-style');
      const bar = el.querySelector('.left-bar');

      if (box) {
        box.style.backgroundColor = bgColor;
        if (type === 'plan' || type === 'regular_plan') {
          box.style.border = `2px solid ${borderColor}`;
        }
      }

      if (bar && (type === 'reservation' || type === 'leave' || type === 'regular_leave')) {
        bar.style.backgroundColor = borderColor;
        if (viewType === 'dayGridMonth') {
          nextTick(() => {
            bar.style.height = `${box.offsetHeight}px`;
          });
        }
      }

      const tooltipLines = [];
      const fallbackTimeRange =
        event.start && event.end
          ? `${event.start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${event.end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
          : null;

      if (type === 'reservation') {
        tooltipLines.push(
          staffName && `🙋 담당자: ${staffName}`,
          customer && `👤 고객: ${customer}`,
          service && `💈 시술: ${service}`,
          status && `📌 예약 상태: ${statusLabels[status] || status}`,
          (timeRange || fallbackTimeRange) && `🕒 시간: ${timeRange || fallbackTimeRange}`,
          memo && `📝 메모: ${memo}`
        );
      } else if (type === 'plan' || type === 'regular_plan') {
        tooltipLines.push(
          staffName && `🙋 담당자: ${staffName}`,
          (timeRange || fallbackTimeRange) && `🕒 시간: ${timeRange || fallbackTimeRange}`,
          memo && `📝 메모: ${memo}`
        );
      } else if (type === 'leave' || type === 'regular_leave') {
        tooltipLines.push(
          staffName && `🙋 담당자: ${staffName}`,
          `📅 종일 휴무`,
          memo && `📝 메모: ${memo}`
        );
      }

      const tooltip = tooltipLines.filter(Boolean).join('<br/>');

      nextTick(() => {
        tippy(el, {
          content: tooltip || '스케줄 정보 없음',
          allowHTML: true,
          placement: 'top',
          animation: 'shift-away',
          delay: [100, 0],
          theme: 'light',
        });
      });
    },
  });

  watch(
    () => props.schedules,
    newVal => {
      const api = calendarRef.value?.getApi();
      const currentView = api?.view?.type || calendarOptions.value.initialView;

      updateResources(currentView);
      updateEvents(currentView);
    },
    { immediate: true, deep: true }
  );

  watch(
    () => props.sidebarCollapsedTrigger,
    async () => {
      await nextTick();
      calendarRef.value?.getApi().updateSize();
    }
  );

  calendarOptions.value.viewDidMount = arg => {
    const viewType = arg.view.type;
    updateResources(viewType);
    updateEvents(viewType);
  };
  calendarOptions.value.datesSet = arg => {
    const { from, to } = extractDateRangeFromView(arg.view);
    emit('change-date-range', { from, to });
  };

  function updateResources(viewType) {
    const staffsFromData = props.schedules.map(s => s.staffName?.trim() || '미지정');
    const seen = new Set();
    const uniqueStaffs = [];
    for (const staff of staffsFromData) {
      if (!seen.has(staff)) {
        seen.add(staff);
        uniqueStaffs.push(staff);
      }
    }
    const resultStaffs = uniqueStaffs.filter(s => s !== '미지정');
    resultStaffs.unshift('미지정');

    calendarOptions.value.resources =
      viewType === 'resourceTimeGridDay'
        ? resultStaffs.map((staff, index) => ({
            id: staff,
            title: staff,
            customOrder: index,
          }))
        : [];
  }

  function updateEvents(viewType) {
    calendarOptions.value.events = props.schedules.map(item => {
      const baseEvent = {
        id: item.id,
        title: item.title ?? (item.type === 'reservation' ? '미등록 고객' : '제목 없음'),
        start: item.start,
        end: item.end,
        extendedProps: {
          type: item.type,
          status: item.status,
          staffName: item.staffName,
          customer: item.customer ?? '미등록 고객',
          service: item.service,
          timeRange: item.timeRange,
          memo: item.memo,
          staffColor: item.staffColor,
        },
      };
      if (viewType === 'resourceTimeGridDay') {
        baseEvent.resourceId = item.staffName?.trim() || '미지정';
      }
      return baseEvent;
    });
  }
</script>

<template>
  <FullCalendar ref="calendarRef" :options="calendarOptions" />
</template>

<style scoped>
  :deep(.fc) {
    border-radius: 8px;
    overflow: hidden;
    background-color: white;
    height: auto;
  }

  :deep(.fc-event) {
    background-color: transparent !important;
    border: none !important;
    font-weight: 500;
    border-radius: 6px;
    box-shadow: none;
  }

  :deep(.fc-daygrid-event) {
    display: block !important;
    z-index: 5;
  }

  :deep(.fc .fc-timegrid-slot-label) {
    width: 80px !important;
    min-width: 80px !important;
    max-width: 80px !important;
  }

  :deep(.fc .fc-timegrid-slot-label-cushion),
  :deep(.fc .fc-scrollgrid-shrink-cushion) {
    display: block !important;
    text-align: left !important;
    padding: 0 !important;
    margin: 0 !important;
    width: 100% !important;
  }

  :deep(.custom-event-style) {
    position: relative;
    display: flex;
    align-items: center;
    font-size: 13px;
    font-weight: 500;
    padding: 6px 8px;
    border-radius: 6px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    height: 100%;
    z-index: 5;
    width: 99%;
  }

  :deep(.custom-event-style.plan) {
    background-color: var(--color-neutral-white);
    z-index: 1;
    padding-left: 0;
    text-align: center;
    flex: 1;
  }

  :deep(.custom-event-style .left-bar) {
    position: absolute;
    left: 0;
    top: 0;
    width: 6px;
    border-radius: 6px 0 0 6px;
    height: 100%;
    z-index: 6;
  }
  :deep(.fc .fc-daygrid-day),
  :deep(.fc .fc-daygrid-body) {
    box-sizing: border-box;
  }

  :deep(.custom-event-style .text) {
    padding-left: 6px;
    text-align: left;
    z-index: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--color-text-primary);
  }

  :deep(.fc-col-header-cell),
  :deep(.fc-theme-standard th) {
    background-color: var(--color-primary-main) !important;
    border-bottom: 1px solid var(--color-primary-main);
  }

  :deep(.fc-scrollgrid) {
    border: 2px solid var(--color-primary-main) !important;
    overflow: hidden;
    box-sizing: border-box;
    table-layout: fixed;
  }

  :deep(.fc-scrollgrid-section-header > table),
  :deep(.fc-scrollgrid-sync-table) {
    border-spacing: 0 !important;
    border-collapse: collapse !important;
    table-layout: fixed !important;
  }

  :deep(.fc-col-header-cell-cushion) {
    color: var(--color-neutral-white) !important;
    font-weight: 600 !important;
    text-decoration: none !important;
    font-size: 14px;
  }

  :deep(.fc-scrollgrid-section-header) {
    border: 2px solid var(--color-primary-main);
    border-radius: 8px;
    overflow: hidden;
  }

  .tippy-box[data-theme~='light'] {
    background-color: var(--color-neutral-white);
    color: var(--color-text-primary);
    font-size: 13px;
    padding: 8px;
    border-radius: 6px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
</style>
