<template>
  <div class="origin-map-page" v-loading="loading">
    <!-- Hero Section -->
    <div class="hero-section">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="hero-label">ORIGIN MAP</div>
        <h1 class="hero-title">产地地图</h1>
        <div class="hero-divider"></div>
        <p class="hero-subtitle">追溯每一份新鲜，从产地到餐桌</p>
      </div>
    </div>

    <!-- Statistics Bar -->
    <div class="stats-bar">
      <div class="stat-item">
        <div class="stat-icon"><el-icon><UserFilled /></el-icon></div>
        <div class="stat-text">
          <div class="stat-value">{{ farmers.length }}</div>
          <div class="stat-label">合作农户</div>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-icon"><el-icon><FullScreen /></el-icon></div>
        <div class="stat-text">
          <div class="stat-value">{{ totalArea }}</div>
          <div class="stat-label">种植面积(亩)</div>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-icon"><el-icon><MapLocation /></el-icon></div>
        <div class="stat-text">
          <div class="stat-value">{{ regionCount }}</div>
          <div class="stat-label">覆盖产区</div>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-icon"><el-icon><Shop /></el-icon></div>
        <div class="stat-text">
          <div class="stat-value">{{ cropCount }}</div>
          <div class="stat-label">主要作物</div>
        </div>
      </div>
    </div>

    <!-- Main Content: Map + Farmer List -->
    <div class="main-content" v-if="farmers.length > 0">
      <!-- Left: Map (60%) -->
      <div class="map-panel">
        <div class="map-header">
          <div class="map-title-row">
            <div class="map-title-icon"><el-icon><Aim /></el-icon></div>
            <h3 class="map-title">产地分布图</h3>
          </div>
          <div class="map-legend">
            <span class="legend-item"><span class="legend-dot normal"></span>农户产地</span>
            <span class="legend-item"><span class="legend-dot selected"></span>已选中</span>
          </div>
        </div>

        <div class="map-container">
          <div class="map-area">
            <!-- China Map SVG Background -->
            <svg class="china-map-svg" viewBox="0 0 100 80" preserveAspectRatio="none">
              <defs>
                <linearGradient id="terrainGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#d4e8d6" />
                  <stop offset="30%" style="stop-color:#c3dfc6" />
                  <stop offset="60%" style="stop-color:#a8d0ac" />
                  <stop offset="100%" style="stop-color:#8ec093" />
                </linearGradient>
                <linearGradient id="riverGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" style="stop-color:#7cb3d4" />
                  <stop offset="100%" style="stop-color:#a8c8e0" />
                </linearGradient>
                <filter id="mapShadow">
                  <feDropShadow dx="0" dy="1" stdDeviation="1.5" flood-color="#1a5d3a" flood-opacity="0.15"/>
                </filter>
              </defs>
              <!-- China outline (simplified) -->
              <path d="M 85,10 L 72,8 L 55,6 L 38,7 L 20,10 L 8,14 L 3,20 L 5,30 L 2,38 L 7,45 L 4,52 L 10,56 L 18,55 L 25,60 L 20,68 L 28,72 L 38,70 L 48,74 L 58,72 L 68,70 L 75,65 L 80,60 L 88,55 L 92,48 L 88,40 L 82,35 L 88,28 L 92,22 L 88,16 Z" fill="url(#terrainGrad)" stroke="#2d8c3c" stroke-width="0.3" opacity="0.9" filter="url(#mapShadow)"/>
              <!-- Yellow River (north) -->
              <path d="M 15,30 Q 25,28 35,32 Q 45,35 55,33 Q 65,31 72,35" fill="none" stroke="url(#riverGrad)" stroke-width="0.6" opacity="0.6"/>
              <!-- Yangtze River (south) -->
              <path d="M 10,50 Q 20,48 30,52 Q 40,55 50,53 Q 60,51 70,50 Q 78,48 85,52" fill="none" stroke="url(#riverGrad)" stroke-width="0.6" opacity="0.6"/>
              <!-- Province labels -->
              <text x="80" y="14" font-size="2.2" fill="#1a5d3a" opacity="0.5" text-anchor="middle">黑龙江</text>
              <text x="65" y="22" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">内蒙古</text>
              <text x="78" y="38" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">山东</text>
              <text x="70" y="55" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">福建</text>
              <text x="15" y="25" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">新疆</text>
              <text x="10" y="42" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">西藏</text>
              <text x="25" y="55" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">云南</text>
              <text x="45" y="58" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">广西</text>
              <text x="48" y="30" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">陕西</text>
              <text x="55" y="25" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">山西</text>
              <text x="60" y="18" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">北京</text>
              <text x="35" y="20" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">甘肃</text>
              <text x="60" y="35" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">河南</text>
              <text x="72" y="45" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">江西</text>
              <text x="75" y="52" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">广东</text>
              <text x="35" y="45" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">四川</text>
              <text x="52" y="45" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">重庆</text>
              <text x="53" y="38" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">湖北</text>
              <text x="62" y="42" font-size="2" fill="#1a5d3a" opacity="0.4" text-anchor="middle">安徽</text>
            </svg>
            <!-- Decorative map grid -->
            <div class="map-grid-overlay"></div>

            <!-- Farmer Markers -->
            <div
              v-for="(f, idx) in farmers"
              :key="f.id || idx"
              class="map-marker"
              :class="{ selected: selectedId === f.id }"
              :style="markerStyle(f)"
              @click="selectFarmer(f.id)"
              @mouseenter="hoverId = f.id"
              @mouseleave="hoverId = null"
            >
              <span class="marker-dot"></span>
              <span class="marker-pulse" v-if="selectedId === f.id"></span>

              <!-- Hover Tooltip -->
              <div class="marker-tooltip" v-if="hoverId === f.id && selectedId !== f.id">
                <div class="tooltip-name">{{ f.farmName || f.name }}</div>
                <div class="tooltip-loc">{{ f.location }}</div>
              </div>
            </div>

            <!-- Selected Popup Card -->
            <transition name="popup">
              <div
                v-if="selectedFarmer"
                class="marker-popup"
                :style="popupStyle(selectedFarmer)"
              >
                <div class="popup-arrow"></div>
                <div class="popup-content">
                  <div class="popup-header">
                    <div class="popup-avatar">{{ (selectedFarmer.name || '?').charAt(0) }}</div>
                    <div>
                      <div class="popup-name">{{ selectedFarmer.name }}</div>
                      <div class="popup-farm">{{ selectedFarmer.farmName }}</div>
                    </div>
                  </div>
                  <div class="popup-info">
                    <div class="popup-info-item">
                      <el-icon><LocationFilled /></el-icon>
                      <span>{{ selectedFarmer.location }}</span>
                    </div>
                    <div class="popup-info-item">
                      <el-icon><Histogram /></el-icon>
                      <span>{{ selectedFarmer.farmArea }} 亩</span>
                    </div>
                    <div class="popup-info-item">
                      <el-icon><Cherry /></el-icon>
                      <span>{{ selectedFarmer.mainCrop }}</span>
                    </div>
                  </div>
                  <div class="popup-desc" v-if="selectedFarmer.regionDescription">
                    {{ selectedFarmer.regionDescription }}
                  </div>
                </div>
              </div>
            </transition>

            <!-- Map label -->
            <div class="map-label">中国产区分布</div>
          </div>
        </div>
      </div>

      <!-- Right: Farmer List (40%) -->
      <div class="farmer-panel">
        <div class="farmer-panel-header">
          <h3 class="panel-title">农户列表</h3>
          <span class="panel-count">共 {{ farmers.length }} 户</span>
        </div>
        <div class="farmer-list">
          <div
            v-for="f in farmers"
            :key="f.id"
            class="farmer-card"
            :class="{ active: selectedId === f.id }"
            @click="selectFarmer(f.id)"
          >
            <div class="farmer-card-top">
              <div class="farmer-avatar">{{ (f.name || '?').charAt(0) }}</div>
              <div class="farmer-card-info">
                <div class="farmer-card-name">{{ f.name }}</div>
                <div class="farmer-card-farm">{{ f.farmName }}</div>
              </div>
              <div class="farmer-card-arrow">
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
            <div class="farmer-card-meta">
              <div class="meta-tag">
                <el-icon><LocationFilled /></el-icon>
                <span>{{ f.location }}</span>
              </div>
              <div class="meta-tag crop">
                <el-icon><Cherry /></el-icon>
                <span>{{ f.mainCrop }}</span>
              </div>
              <div class="meta-tag area">
                <el-icon><FullScreen /></el-icon>
                <span>{{ f.farmArea }} 亩</span>
              </div>
            </div>
            <div class="farmer-card-tags" v-if="f.regionTags && f.regionTags.length">
              <span v-for="(tag, ti) in regionTagArray(f.regionTags)" :key="ti" class="region-tag">
                {{ tag }}
              </span>
            </div>
            <div class="farmer-card-desc" v-if="f.regionDescription">
              {{ f.regionDescription }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!loading" class="empty-state">
      <el-icon size="64" color="#c0c4cc"><MapLocation /></el-icon>
      <p class="empty-title">暂无产地数据</p>
      <p class="empty-desc">暂时无法获取农户产地信息</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { originMapApi } from '@/api'

const farmers = ref([])
const loading = ref(true)
const selectedId = ref(null)
const hoverId = ref(null)

const selectedFarmer = computed(() =>
  farmers.value.find(f => f.id === selectedId.value) || null
)

const totalArea = computed(() =>
  farmers.value.reduce((sum, f) => {
    const area = parseFloat(f.farmArea) || 0
    return sum + area
  }, 0).toFixed(0)
)

const regionCount = computed(() => {
  const locs = new Set(farmers.value.map(f => f.location).filter(Boolean))
  return locs.size
})

const cropCount = computed(() => {
  const crops = new Set(farmers.value.map(f => f.mainCrop).filter(Boolean))
  return crops.size
})

const regionTagArray = (tags) => {
  if (Array.isArray(tags)) return tags
  if (typeof tags === 'string') {
    return tags.split(/[,，、]/).map(t => t.trim()).filter(Boolean)
  }
  return []
}

const markerStyle = (f) => {
  const lng = parseFloat(f.longitude) || 100
  const lat = parseFloat(f.latitude) || 35
  const left = ((lng - 73) / (135 - 73)) * 100
  const top = ((53 - lat) / (53 - 18)) * 100
  return {
    left: `${Math.max(2, Math.min(96, left))}%`,
    top: `${Math.max(4, Math.min(94, top))}%`
  }
}

const popupStyle = (f) => {
  const lng = parseFloat(f.longitude) || 100
  const lat = parseFloat(f.latitude) || 35
  const left = ((lng - 73) / (135 - 73)) * 100
  const top = ((53 - lat) / (53 - 18)) * 100
  return {
    left: `${Math.max(12, Math.min(84, left))}%`,
    top: `${Math.max(28, Math.min(90, top))}%`
  }
}

const selectFarmer = (id) => {
  selectedId.value = selectedId.value === id ? null : id
}

const loadMapData = async () => {
  loading.value = true
  try {
    const res = await originMapApi.mapData()
    farmers.value = res.data
    if (farmers.value.length > 0) {
      selectedId.value = farmers.value[0].id
    }
  } catch (e) {
    farmers.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadMapData)
</script>

<style scoped>
.origin-map-page { max-width: 1280px; margin: 0 auto; padding-bottom: 40px; }

/* Hero */
.hero-section { position: relative; border-radius: 20px; overflow: hidden; margin-bottom: 24px; box-shadow: 0 8px 30px rgba(0,0,0,0.12); }
.hero-bg { position: absolute; inset: 0; background: linear-gradient(135deg, #1a5d3a 0%, #2d8c3c 55%, #3da655 100%); }
.hero-bg::before { content: ''; position: absolute; inset: 0; background: radial-gradient(circle at 25% 75%, rgba(255,255,255,0.1) 0%, transparent 50%), radial-gradient(circle at 75% 25%, rgba(255,255,255,0.06) 0%, transparent 40%); }
.hero-content { position: relative; z-index: 1; padding: 44px 40px; text-align: center; color: #fff; }
.hero-label { font-size: 12px; letter-spacing: 3px; opacity: 0.7; font-weight: 600; margin-bottom: 8px; }
.hero-title { font-size: 34px; font-weight: 800; margin-bottom: 12px; letter-spacing: 2px; }
.hero-divider { width: 60px; height: 3px; background: rgba(255,255,255,0.5); border-radius: 9999px; margin: 0 auto 16px; }
.hero-subtitle { font-size: 16px; opacity: 0.9; }

/* Stats Bar */
.stats-bar {
  display: flex; align-items: center; gap: 0;
  background: #fff; border-radius: 12px; padding: 20px 30px;
  margin-bottom: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.stat-item { display: flex; align-items: center; gap: 14px; flex: 1; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; background: #e8f5e9; color: #1a5d3a; display: flex; align-items: center; justify-content: center; font-size: 24px; }
.stat-value { font-size: 24px; font-weight: 800; color: #1a5d3a; line-height: 1.2; }
.stat-label { font-size: 13px; color: #999; }
.stat-divider { width: 1px; height: 40px; background: #f0f0f0; }

/* Main Content */
.main-content { display: flex; gap: 20px; align-items: flex-start; }

/* Map Panel (60%) */
.map-panel { width: 60%; background: #fff; border-radius: 20px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.map-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 24px; border-bottom: 1px solid #f0f0f0; }
.map-title-row { display: flex; align-items: center; gap: 10px; }
.map-title-icon { width: 32px; height: 32px; border-radius: 8px; background: #1a5d3a; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 16px; }
.map-title { font-size: 17px; font-weight: 700; color: #1a5d3a; }
.map-legend { display: flex; gap: 16px; }
.legend-item { display: flex; align-items: center; gap: 5px; font-size: 12px; color: #999; }
.legend-dot { width: 10px; height: 10px; border-radius: 9999px; }
.legend-dot.normal { background: #2d8c3c; }
.legend-dot.selected { background: #1a5d3a; box-shadow: 0 0 0 3px rgba(26,93,58,0.2); }

.map-container { padding: 20px; }
.map-area {
  position: relative; width: 100%; aspect-ratio: 1.25 / 1; min-height: 420px;
  border-radius: 12px; overflow: hidden;
  background: linear-gradient(160deg, #eef7ee 0%, #e0f0e1 30%, #d0e8d2 60%, #c0dcc3 100%);
  border: 1px solid #d4e8d6;
}
.china-map-svg {
  position: absolute; inset: 0; width: 100%; height: 100%; z-index: 1;
}
.map-grid-overlay {
  position: absolute; inset: 0; opacity: 0.15;
  background-image: linear-gradient(rgba(26,93,58,0.08) 1px, transparent 1px),
                    linear-gradient(90deg, rgba(26,93,58,0.08) 1px, transparent 1px);
  background-size: 40px 40px; z-index: 2;
}
.map-label { position: absolute; bottom: 14px; right: 18px; font-size: 12px; color: rgba(26,93,58,0.5); font-weight: 600; letter-spacing: 1px; z-index: 3; }

/* Markers */
.map-marker { position: absolute; transform: translate(-50%, -50%); cursor: pointer; z-index: 10; }
.marker-dot {
  display: block; width: 14px; height: 14px; border-radius: 9999px;
  background: #2d8c3c; border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.2); transition: all 0.3s ease;
}
.map-marker:hover .marker-dot { transform: scale(1.3); background: #1a5d3a; }
.map-marker.selected .marker-dot { background: #1a5d3a; box-shadow: 0 0 0 4px rgba(26,93,58,0.25), 0 2px 6px rgba(0,0,0,0.2); transform: scale(1.4); }
.marker-pulse {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  width: 14px; height: 14px; border-radius: 9999px; background: #1a5d3a;
  animation: pulse 1.8s ease-out infinite; pointer-events: none;
}
@keyframes pulse {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 0.6; }
  100% { transform: translate(-50%, -50%) scale(3.5); opacity: 0; }
}

/* Marker Tooltip */
.marker-tooltip {
  position: absolute; bottom: calc(100% + 8px); left: 50%; transform: translateX(-50%);
  background: rgba(26,93,58,0.95); color: #fff; padding: 8px 14px;
  border-radius: 6px; white-space: nowrap; font-size: 12px; z-index: 20;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15); pointer-events: none;
}
.marker-tooltip::after { content: ''; position: absolute; top: 100%; left: 50%; transform: translateX(-50%); border: 5px solid transparent; border-top-color: rgba(26,93,58,0.95); }
.tooltip-name { font-weight: 700; margin-bottom: 2px; }
.tooltip-loc { font-size: 11px; opacity: 0.85; }

/* Selected Popup */
.marker-popup {
  position: absolute; z-index: 30; width: 240px;
  background: #fff; border-radius: 12px; padding: 18px 20px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.12);
  transform: translate(-50%, -100%); margin-top: -16px;
}
.popup-arrow { position: absolute; bottom: -7px; left: 50%; transform: translateX(-50%) rotate(45deg); width: 14px; height: 14px; background: #fff; box-shadow: 3px 3px 6px rgba(0,0,0,0.05); }
.popup-header { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.popup-avatar { width: 42px; height: 42px; border-radius: 9999px; background: linear-gradient(135deg, #1a5d3a, #2d8c3c); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 700; flex-shrink: 0; }
.popup-name { font-size: 15px; font-weight: 700; color: #333; }
.popup-farm { font-size: 13px; color: #999; margin-top: 2px; }
.popup-info { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.popup-info-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #666; }
.popup-info-item .el-icon { color: #2d8c3c; font-size: 15px; }
.popup-desc { font-size: 12px; color: #999; line-height: 1.6; padding-top: 10px; border-top: 1px solid #f5f5f5; }
.popup-enter-active, .popup-leave-active { transition: all 0.3s ease; }
.popup-enter-from, .popup-leave-to { opacity: 0; transform: translate(-50%, -80%); }

/* Farmer Panel (40%) */
.farmer-panel { width: 40%; background: #fff; border-radius: 20px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.06); display: flex; flex-direction: column; max-height: 640px; }
.farmer-panel-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 22px; border-bottom: 1px solid #f0f0f0; flex-shrink: 0; }
.panel-title { font-size: 17px; font-weight: 700; color: #1a5d3a; }
.panel-count { font-size: 13px; color: #999; background: #e8f5e9; padding: 3px 12px; border-radius: 9999px; }

.farmer-list { overflow-y: auto; padding: 14px; flex: 1; }
.farmer-list::-webkit-scrollbar { width: 6px; }
.farmer-list::-webkit-scrollbar-track { background: #f5f5f5; border-radius: 3px; }
.farmer-list::-webkit-scrollbar-thumb { background: #c8e6c9; border-radius: 3px; }
.farmer-list::-webkit-scrollbar-thumb:hover { background: #2d8c3c; }

.farmer-card { padding: 16px 18px; border-radius: 12px; background: #fafafa; margin-bottom: 10px; cursor: pointer; transition: all 0.3s ease; border: 1px solid transparent; }
.farmer-card:last-child { margin-bottom: 0; }
.farmer-card:hover { background: #f0f9eb; border-color: #c8e6c9; transform: translateX(-4px); }
.farmer-card.active { background: #e8f5e9; border-color: #2d8c3c; box-shadow: 0 4px 14px rgba(45,140,60,0.12); }

.farmer-card-top { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.farmer-avatar { width: 40px; height: 40px; border-radius: 9999px; background: linear-gradient(135deg, #1a5d3a, #2d8c3c); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 700; flex-shrink: 0; }
.farmer-card-info { flex: 1; min-width: 0; }
.farmer-card-name { font-size: 15px; font-weight: 700; color: #1a5d3a; }
.farmer-card-farm { font-size: 13px; color: #999; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.farmer-card-arrow { color: #ccc; transition: all 0.3s ease; }
.farmer-card:hover .farmer-card-arrow, .farmer-card.active .farmer-card-arrow { color: #2d8c3c; transform: translateX(3px); }

.farmer-card-meta { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 10px; }
.meta-tag { display: flex; align-items: center; gap: 4px; padding: 4px 10px; border-radius: 9999px; font-size: 12px; background: #f5f5f5; color: #666; }
.meta-tag .el-icon { font-size: 13px; }
.meta-tag.crop { background: #f0f9eb; color: #2d8c3c; }
.meta-tag.area { background: #fff3e0; color: #e6a23c; }

.farmer-card-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.region-tag { padding: 2px 10px; border-radius: 6px; font-size: 11px; background: #e8f5e9; color: #1a5d3a; font-weight: 500; }

.farmer-card-desc { font-size: 12px; color: #999; line-height: 1.6; }

/* Empty State */
.empty-state { text-align: center; padding: 80px 20px; }
.empty-state .el-icon { margin-bottom: 16px; }
.empty-title { font-size: 18px; font-weight: 600; color: #666; margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: #bbb; }

/* Responsive */
@media (max-width: 992px) {
  .main-content { flex-direction: column; }
  .map-panel, .farmer-panel { width: 100%; }
  .farmer-panel { max-height: 500px; }
  .stats-bar { flex-wrap: wrap; gap: 12px; }
  .stat-divider { display: none; }
  .stat-item { flex: 1 1 40%; }
}
</style>
