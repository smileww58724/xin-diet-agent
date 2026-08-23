<template>
  <div class="profile-container">
    <el-page-header @back="$router.back()" content="个人设置" />

    <el-card class="mt">
      <template #header>基本信息</template>
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio value="male">男</el-radio>
            <el-radio value="female">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="form.age" :min="1" :max="150" />
        </el-form-item>
        <el-form-item label="身高(cm)">
          <el-input-number v-model="form.height" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="体重(kg)">
          <el-input-number v-model="form.weight" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="运动水平">
          <el-select v-model="form.activityLevel" placeholder="请选择">
            <el-option label="久坐" value="sedentary" />
            <el-option label="轻度" value="light" />
            <el-option label="中度" value="moderate" />
            <el-option label="活跃" value="active" />
            <el-option label="非常活跃" value="very_active" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标类型">
          <el-select v-model="form.goalType" placeholder="请选择">
            <el-option label="减肥" value="lose_weight" />
            <el-option label="增肌" value="gain_weight" />
            <el-option label="维持" value="maintain" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="loading">保存</el-button>
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
  username: localStorage.getItem('username') || '',
  nickname: '',
  gender: '',
  age: null,
  height: null,
  weight: null,
  activityLevel: '',
  goalType: '',
})

onMounted(async () => {
  try {
    const profile = await goalsAPI.getProfile()
    Object.assign(form, profile)
  } catch (e) {
    console.error(e)
  }
})

const handleSave = async () => {
  loading.value = true
  try {
    await goalsAPI.updateProfile(form)
    if (form.nickname) {
      localStorage.setItem('username', form.nickname)
    }
    ElMessage.success('保存成功')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.profile-container {
  padding: 20px;
}
.mt {
  margin-top: 20px;
}
</style>
