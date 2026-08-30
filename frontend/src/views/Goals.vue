<template>
  <div class="goals-page">
    <div class="page-head">
      <div>
        <h1>目标设定</h1>
        <div class="page-sub">设定后 AI 建议与营养分析会以此为准</div>
      </div>
    </div>

    <div class="goals-grid">
      <div class="goal-card">
        <div class="card-title">每日营养目标</div>
        <el-form :model="form" label-position="top" class="goal-form">
          <div class="goal-row">
            <div class="goal-item">
              <span class="goal-label">热量</span>
              <el-input-number v-model="form.dailyCalorieGoal" :min="0" :step="100" controls-position="right" style="width: 100%" />
              <span class="goal-unit">kcal / 天</span>
            </div>
            <div class="goal-item">
              <span class="goal-label">蛋白质</span>
              <el-input-number v-model="form.proteinGoal" :min="0" :step="5" controls-position="right" style="width: 100%" />
              <span class="goal-unit">g / 天</span>
            </div>
          </div>
          <div class="goal-row">
            <div class="goal-item">
              <span class="goal-label">脂肪</span>
              <el-input-number v-model="form.fatGoal" :min="0" :step="5" controls-position="right" style="width: 100%" />
              <span class="goal-unit">g / 天</span>
            </div>
            <div class="goal-item">
              <span class="goal-label">碳水</span>
              <el-input-number v-model="form.carbGoal" :min="0" :step="10" controls-position="right" style="width: 100%" />
              <span class="goal-unit">g / 天</span>
            </div>
          </div>
          <el-button type="primary" style="width: 100%; margin-top: 8px" @click="handleSave" :loading="loading">
            保存目标
          </el-button>
        </el-form>
      </div>

      <div class="goal-aside">
        <div class="card-title">参考值</div>
        <p class="aside-text">不确定设多少？一般成年人可参考：</p>
        <ul class="aside-list">
          <li><b>2000 kcal</b> 每日热量（轻体力活动）</li>
          <li><b>60 g</b> 蛋白质，<b>65 g</b> 脂肪，<b>300 g</b> 碳水</li>
          <li>减脂建议热量下调 10–20%，蛋白质上调</li>
        </ul>
        <p class="aside-note">保存后立即生效，AI 对话与营养分析都会按最新目标计算。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { goalsAPI } from '../api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const form = reactive({
  dailyCalorieGoal: 2000,
  proteinGoal: 60,
  fatGoal: 65,
  carbGoal: 300,
})

onMounted(async () => {
  try {
    const profile = await goalsAPI.getProfile()
    if (profile.dailyCalorieGoal) form.dailyCalorieGoal = profile.dailyCalorieGoal
    if (profile.proteinGoal) form.proteinGoal = profile.proteinGoal
    if (profile.fatGoal) form.fatGoal = profile.fatGoal
    if (profile.carbGoal) form.carbGoal = profile.carbGoal
  } catch (e) {
    console.error(e)
  }
})

const handleSave = async () => {
  loading.value = true
  try {
    await goalsAPI.updateProfile(form)
    ElMessage.success('目标保存成功')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.goals-page {
  max-width: 780px;
  margin: 0 auto;
}
.goals-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
  align-items: start;
}
@media (max-width: 720px) {
  .goals-grid {
    grid-template-columns: 1fr;
  }
}
.goal-card {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 22px;
}
.card-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 18px;
}
.goal-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.goal-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}
.goal-label {
  font-size: 13px;
  color: var(--text-2);
  font-weight: 500;
}
.goal-unit {
  font-size: 11px;
  color: var(--text-3);
}
.goal-aside {
  background: var(--panel-soft);
  border-radius: var(--radius-lg);
  padding: 22px;
}
.aside-text {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--text-2);
}
.aside-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: var(--text-2);
  line-height: 2;
}
.aside-list b {
  color: var(--ink);
}
.aside-note {
  margin: 14px 0 0;
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.7;
}
</style>
