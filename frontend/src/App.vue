<template>
  <router-view v-slot="{ Component }">
    <transition name="route-fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup>
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@400;500;600;700&family=Inter:wght@300;400;500;600;700&family=Dancing+Script:wght@500;600;700&family=Noto+Serif+SC:wght@400;700;900&display=swap');

:root {
  /* === 青白 Brand Color System === */
  --color-primary: #1a5d3a;
  --color-primary-dark: #0f3d24;
  --color-primary-light: #2d8e5a;
  --color-primary-bg: #d6eedd;
  --color-primary-bg-light: #e8f5e8;

  /* Accent colors */
  --color-accent: #F59E0B;
  --color-accent-light: #FCD34D;
  --color-danger: #E53935;
  --color-info: #2196F3;

  /* Neutral */
  --color-title: #1a221e;
  --color-text: #333333;
  --color-text-secondary: #6a746e;
  --color-text-hint: #8a948e;
  --color-border: #c8e0c8;
  --color-border-light: #e0f0e0;
  --color-bg: #f5faf6;
  --color-card: #ffffff;
  --color-warm: #d6eedd;

  /* Legacy aliases (backward compat) */
  --brand-primary: var(--color-primary);
  --brand-primary-light: var(--color-primary-light);
  --brand-primary-dark: var(--color-primary-dark);
  --bg-base: var(--color-bg);
  --bg-card: var(--color-card);
  --bg-warm: var(--color-warm);
  --text-primary: var(--color-title);
  --text-secondary: var(--color-text-secondary);
  --text-tertiary: var(--color-text-hint);
  --border-color: var(--color-border);
  --border-light: var(--color-border-light);

  /* Shadow system */
  --shadow-xs: 0 1px 2px rgba(0,0,0,0.02);
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.04);
  --shadow-md: 0 2px 8px rgba(0,0,0,0.06);
  --shadow-lg: 0 8px 24px rgba(0,0,0,0.10);
  --shadow-hover: 0 12px 32px rgba(0,0,0,0.12);

  /* Radius system */
  --radius-sm: 6px;
  --radius-md: 12px;
  --radius-lg: 20px;
  --radius-xl: 28px;
  --radius-full: 9999px;

  /* Spacing (8px base) */
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 16px;
  --space-lg: 24px;
  --space-xl: 32px;
  --space-2xl: 48px;
  --space-3xl: 64px;

  /* Fonts */
  --font-heading: 'Noto Serif SC', 'Cormorant Garamond', 'STSong', 'SimSun', serif;
  --font-serif: 'Cormorant Garamond', 'Noto Serif SC', 'STSong', serif;
  --font-body: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --font-sans: var(--font-body);
  --font-script: 'Dancing Script', 'Noto Serif SC', cursive;
  --font-number: 'DIN Alternate', 'Roboto Mono', 'Inter', monospace;

  /* Transitions */
  --ease-out: cubic-bezier(0.16, 1, 0.3, 1);
  --ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);
  --ease-standard: cubic-bezier(0.4, 0, 0.2, 1);
  --duration-fast: 0.25s;
  --duration-normal: 0.4s;
  --duration-slow: 0.6s;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html { scroll-behavior: smooth; }

body {
  font-family: var(--font-body);
  background-color: var(--color-bg);
  color: var(--color-text);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

h1, h2, h3, .heading {
  font-family: var(--font-heading);
}

/* === Global Animations === */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes cardEnter {
  from { opacity: 0; transform: translateY(30px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
@keyframes countUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes floatUp {
  from { transform: translateY(0); }
  to { transform: translateY(-8px); }
}
@keyframes bounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.3); }
}
@keyframes slideDown {
  from { transform: translateY(-100%); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

/* Route transition */
.route-fade-enter-active, .route-fade-leave-active {
  transition: opacity 0.3s var(--ease-standard), transform 0.3s var(--ease-standard);
}
.route-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.route-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Section entrance */
.section-anim {
  animation: fadeInUp 0.6s ease both;
}
.section-anim:nth-child(1) { animation-delay: 0s; }
.section-anim:nth-child(2) { animation-delay: 0.1s; }
.section-anim:nth-child(3) { animation-delay: 0.2s; }
.section-anim:nth-child(4) { animation-delay: 0.3s; }

/* === Element Plus Overrides === */
.el-button--primary {
  background: var(--color-primary) !important;
  border: 1px solid var(--color-primary) !important;
  color: #fff !important;
  font-weight: 500;
  border-radius: var(--radius-md) !important;
  padding: 10px 28px;
  transition: all 0.25s var(--ease-out);
}
.el-button--primary:hover {
  background: var(--color-primary-dark) !important;
  border-color: var(--color-primary-dark) !important;
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}
.el-button--primary:active {
  transform: scale(0.96);
}
.el-button--default {
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-md) !important;
  color: var(--color-text-secondary) !important;
  font-weight: 500;
  transition: all 0.25s var(--ease-out);
}
.el-button--default:hover {
  border-color: var(--color-primary) !important;
  color: var(--color-primary) !important;
  background: var(--color-primary-bg) !important;
}

.el-tabs__item.is-active {
  color: var(--color-primary);
  font-weight: 600;
}
.el-tabs__active-bar { background-color: var(--color-primary); }
.el-tabs__item:hover { color: var(--color-primary); }

.el-input__wrapper {
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 0 1px var(--color-border) inset !important;
  transition: box-shadow 0.25s var(--ease-out);
}
.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px var(--color-primary) inset !important;
}

.el-pagination.is-background .el-pager li {
  border-radius: var(--radius-sm) !important;
}
.el-pagination.is-background .el-pager li.is-active {
  background-color: var(--color-primary) !important;
}

.el-tag {
  border-radius: var(--radius-sm) !important;
  font-weight: 500;
}

.el-card {
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-sm) !important;
  border: 1px solid var(--color-border-light) !important;
}

.el-rate__icon { font-size: 14px; }

/* Scrollbar */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--color-border); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: var(--color-primary-light); }

/* Smooth transitions */
a, button, .el-button {
  transition: all var(--duration-fast) var(--ease-out);
}

/* Scroll progress bar */
.scroll-progress {
  position: fixed; top: 0; left: 0;
  height: 2px; background: var(--color-primary);
  z-index: 9999; transition: width 0.1s linear;
}

/* Section utilities */
.section-eyebrow {
  display: inline-flex; align-items: center; gap: 10px;
  font-size: 11px; font-weight: 500;
  letter-spacing: 0.2em; text-transform: uppercase;
  color: var(--color-primary); margin-bottom: 12px;
}
.section-eyebrow::after {
  content: ''; width: 40px; height: 1px;
  background: var(--color-primary);
}
.section-title {
  font-family: var(--font-heading);
  font-size: 28px; font-weight: 700;
  color: var(--color-title);
}
.editorial-divider {
  width: 40px; height: 2px;
  background: var(--color-primary);
  margin: 16px auto 0; border-radius: 1px;
}
</style>
