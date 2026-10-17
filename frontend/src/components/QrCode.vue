<template>
  <div class="qrcode-wrapper">
    <canvas ref="canvasRef"></canvas>
    <div v-if="loading" class="qr-loading">生成中...</div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import QRCode from 'qrcode'

const props = defineProps({
  text: { type: String, required: true },
  size: { type: Number, default: 160 }
})

const canvasRef = ref(null)
const loading = ref(true)

const generate = async () => {
  if (!canvasRef.value || !props.text) return
  loading.value = true
  try {
    await QRCode.toCanvas(canvasRef.value, props.text, {
      width: props.size,
      margin: 2,
      color: { dark: '#1a5d3a', light: '#ffffff' }
    })
  } catch (e) {
    console.error('QR code generation failed:', e)
  } finally {
    loading.value = false
  }
}

watch(() => props.text, generate)
onMounted(generate)
</script>

<style scoped>
.qrcode-wrapper { position: relative; display: inline-block; }
.qr-loading { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #999; background: #f5f5f5; }
</style>
