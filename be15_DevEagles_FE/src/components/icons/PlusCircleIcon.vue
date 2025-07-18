<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    :class="className"
  >
    <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2" />
    <line
      x1="12"
      y1="8"
      x2="12"
      y2="16"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
    />
    <line
      x1="8"
      y1="12"
      x2="16"
      y2="12"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
    />
  </svg>
</template>

<script setup>
  defineProps({
    size: {
      type: [String, Number],
      default: 20,
    },
    className: {
      type: String,
      default: '',
    },
  });
</script>
