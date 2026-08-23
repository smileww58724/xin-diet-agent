<template>
  <div class="goals-container">
    <el-page-header @back="$router.back()" content="目标设定" />

    <el-card class="mt">
      <template #header>每日目标</template>
      <el-form :model="form" label-width="100px">
        <el-form-item label="每日卡路里">
          <el-input-number v-model="form.dailyCalorieGoal" :min="0" :step="100" />
          <span class="ml">kcal</span>
        </el-form-item>
        <el-form-item label="蛋白质目标">
          <el-input-number v-model="form.proteinGoal" :min="0" :step="5" />
          <span class="ml">g</span>
        </el-form-item>
        <el-form-item label="脂肪目标">
          <el-input-number v-model="form.fatGoal" :min="0" :step="5" />
          <span class="ml">g</span>
        </el-form-item>
        <el-form-item label="碳水目标">
          <el-input-number v-model="form.carbGoal" :min="0" :step="10" />
          <span class="ml">g</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="loading">保存目标</el-button>
        </el-form-item>
      </el-form>
    </el-card>
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
.goals-container {
  padding: 20px;
}
.mt {
  margin-top: 20px;
}
.ml {
  margin-left: 10px;
}
</style>
