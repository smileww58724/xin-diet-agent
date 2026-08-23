<template>
  <div class="analysis-container">
    <el-page-header @back="$router.back()" content="营养分析" />

    <el-radio-group v-model="viewType" class="mt">
      <el-radio-button label="daily">日报</el-radio-button>
      <el-radio-button label="weekly">周报</el-radio-button>
    </el-radio-group>

    <div v-if="summary" class="summary-cards">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card>
            <template #header>热量</template>
            <div class="progress-item">
              <span>{{ summary.totalCalories }} / {{ summary.calorieGoal }} kcal</span>
              <el-progress :percentage="summary.calorieProgress" :stroke-width="24" />
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>蛋白质</template>
            <div class="progress-item">
              <span>{{ summary.totalProtein }}g / {{ summary.proteinGoal }}g</span>
              <el-progress :percentage="summary.proteinProgress" :stroke-width="24" color="#67C23A" />
            </div>
          </el-card>
        </el-col>
        <el-col :span="12" class="mt">
          <el-card>
            <template #header>脂肪</template>
            <div class="progress-item">
              <span>{{ summary.totalFat }}g / {{ summary.fatGoal }}g</span>
              <el-progress :percentage="summary.fatProgress" :stroke-width="24" color="#E6A23C" />
            </div>
          </el-card>
        </el-col>
        <el-col :span="12" class="mt">
          <el-card>
            <template #header>碳水</template>
            <div class="progress-item">
              <span>{{ summary.totalCarbohydrate }}g / {{ summary.carbGoal }}g</span>
              <el-progress :percentage="summary.carbProgress" :stroke-width="24" color="#409EFF" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { analysisAPI } from '../api'

const viewType = ref('daily')
const summary = ref(null)

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
.analysis-container {
  padding: 20px;
}
.mt {
  margin-top: 20px;
}
.progress-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.progress-item span {
  font-size: 16px;
  font-weight: bold;
}
</style>
