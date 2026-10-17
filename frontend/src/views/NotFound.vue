<template>
  <div class="not-found-page">
    <div class="nf-content">
      <div class="nf-code">404</div>
      <h1 class="nf-title">页面走丢了</h1>
      <p class="nf-desc">您访问的页面不存在或已被移动</p>
      <div class="nf-actions">
        <button class="btn-primary" @click="$router.push('/')">返回首页</button>
        <button class="btn-secondary" @click="$router.back()">返回上页</button>
      </div>
      <div class="nf-illustration">
        <svg viewBox="0 0 200 120" width="200" height="120" fill="none" stroke="#2d8c3c" stroke-width="2">
          <path d="M50 100 Q50 60 80 60 Q110 60 110 90 Q110 100 100 100" stroke="#1a5d3a" stroke-width="2.5" fill="none"/>
          <circle cx="80" cy="40" r="15" fill="#2d8c3c" opacity="0.1"/>
          <circle cx="120" cy="50" r="10" fill="#2d8c3c" opacity="0.1"/>
          <circle cx="150" cy="35" r="8" fill="#2d8c3c" opacity="0.1"/>
          <path d="M30 110 L170 110" stroke="#1a5d3a" stroke-width="2" opacity="0.3"/>
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found-page {
  min-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.nf-content {
  text-align: center;
  max-width: 480px;
}
.nf-code {
  font-size: 120px;
  font-weight: 900;
  color: #1a5d3a;
  line-height: 1;
  margin-bottom: 20px;
  letter-spacing: -4px;
  background: linear-gradient(135deg, #1a5d3a, #2d8c3c);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.nf-title {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin-bottom: 12px;
}
.nf-desc {
  font-size: 15px;
  color: #888;
  margin-bottom: 32px;
}
.nf-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 40px;
}
.btn-primary {
  padding: 12px 32px;
  background: #1a5d3a;
  color: white;
  border: none;
  border-radius: 9999px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-primary:hover {
  background: #2d8c3c;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(26,93,58,0.3);
}
.btn-secondary {
  padding: 12px 32px;
  background: white;
  color: #1a5d3a;
  border: 1.5px solid #1a5d3a;
  border-radius: 9999px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-secondary:hover {
  background: #f0f7f0;
}
.nf-illustration {
  margin-top: 20px;
}
</style>
