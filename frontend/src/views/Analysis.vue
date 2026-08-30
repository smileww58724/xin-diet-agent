<template>
  <div class="analysis-page">
    <div class="page-head">
      <div>
        <h1>营养分析</h1>
        <div class="page-sub">基于你的真实饮食记录与目标</div>
      </div>
      <div class="seg">
        <button :class="{ active: viewType === 'daily' }" @click="viewType = 'daily'">日报</button>
        <button :class="{ active: viewType === 'weekly' }" @click="viewType = 'weekly'">周报</button>
      </div>
    </div>

    <div v-if="summary" class="stat-grid">
      <div v-for="card in cards" :key="card.label" class="stat-card">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">
          {{ card.value }}<span class="stat-unit">/ {{ card.goal }}{{ card.unit }}</span>
        </div>
        <div class="bar"><div class="bar-fill" :style="{ width: card.progress + '%' }"></div></div>
        <div class="stat-pct">{{ Math.round(card.progress) }}% of 目标</div>
      </div>
    </div>

    <div v-if="summary" class="insight">
      <template v-if="summary.totalCalories === 0">
        <p class="insight-title">今天还没有记录</p>
        <p class="insight-text">去记录第一餐，或在 AI 对话里直接告诉我你吃了什么。</p>
        <router-link to="/diet" class="pill-link">去记录饮食</router-link>
      </template>
      <template v-else>
        <p class="insight-title">{{ viewType === 'daily' ? '今日小结' : '本周小结' }}</p>
        <p class="insight-text">
          热量完成 {{ Math.round(summary.calorieProgress) }}%，蛋白质完成
          {{ Math.round(summary.proteinProgress) }}%。
          <template v-if="summary.calorieProgress >= 100">已达到热量目标，注意不要超量哦。</template>
          <template v-else-if="summary.calorieProgress >= 60">进度良好，晚餐记得均衡搭配。</template>
          <template v-else>摄入还有空间，别忘记补充优质蛋白。</template>
        </p>
      </template>
    </div>

    <div v-if="!summary" class="empty-state"><p>加载中…</p></div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { analysisAPI } from '../api'

const viewType = ref('daily')
const summary = ref(null)

const cards = computed(() => {
  if (!summary.value) return []
  const s = summary.value
  return [
    { label: '热量', value: s.totalCalories, goal: s.calorieGoal, unit: ' kcal', progress: s.calorieProgress },
    { label: '蛋白质', value: s.totalProtein, goal: s.proteinGoal, unit: 'g', progress: s.proteinProgress },
    { label: '脂肪', value: s.totalFat, goal: s.fatGoal, unit: 'g', progress: s.fatProgress },
    { label: '碳水', value: s.totalCarbohydrate, goal: s.carbGoal, unit: 'g', progress: s.carbProgress },
  ]
})

const loadData = async () => {
  try {
    if (viewType.value === 'daily') {
      summary.value = await analysisAPI.getDaily()
    } else {
      summary.value = await analysisAPI.getWeekly()
    }
  } catch (e) {
    console.error(e)
  }
}

watch(viewType, loadData, { immediate: true })
</script>

<style scoped>
.analysis-page {
  max-width: 860px;
  margin: 0 auto;
}

/* 分段切换：胶囊容器 + 黑色激活块 */
.seg {
  display: flex;
  background: var(--panel-soft);
  border-radius: var(--radius-pill);
  padding: 4px;
}
.seg button {
  border: none;
  background: transparent;
  padding: 7px 20px;
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.12s;
}
.seg button.active {
  background: var(--ink);
  color: #fff;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
@media (max-width: 760px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
.stat-card {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
}
.stat-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
}
.stat-value {
  margin: 10px 0 14px;
  font-size: 26px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: -0.4px;
}
.stat-unit {
  margin-left: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-3);
  letter-spacing: 0;
}
.bar {
  height: 6px;
  background: #e8e8ea;
  border-radius: 999px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  background: var(--ink);
  border-radius: 999px;
  transition: width 0.4s ease;
}
.stat-pct {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-3);
}

.insight {
  margin-top: 16px;
  background: var(--panel-soft);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
  text-align: center;
}
.insight-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
}
.insight-text {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.7;
}
.pill-link {
  display: inline-block;
  margin-top: 14px;
  padding: 9px 22px;
  background: var(--ink);
  color: #fff;
  border-radius: var(--radius-pill);
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
}
.pill-link:hover {
  opacity: 0.85;
}
</style>
