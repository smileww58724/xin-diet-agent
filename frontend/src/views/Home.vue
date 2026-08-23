<template>
  <div class="home-container">
    <el-container>
      <el-header>
        <div class="header-left">
          <h2>🍽️ 饮食管理智能体</h2>
        </div>
        <div class="header-right">
          <span>欢迎，{{ username }}</span>
          <el-button @click="handleLogout" link>退出</el-button>
        </div>
      </el-header>
      <el-container>
        <el-aside width="200px">
          <el-menu :default-active="activeMenu" router>
            <el-menu-item index="/">
              <el-icon><HomeFilled /></el-icon>
              <span>首页</span>
            </el-menu-item>
            <el-menu-item index="/diet">
              <el-icon><Food /></el-icon>
              <span>饮食记录</span>
            </el-menu-item>
            <el-menu-item index="/analysis">
              <el-icon><DataAnalysis /></el-icon>
              <span>营养分析</span>
            </el-menu-item>
            <el-menu-item index="/goals">
              <el-icon><Aim /></el-icon>
              <span>目标设定</span>
            </el-menu-item>
            <el-menu-item index="/chat">
              <el-icon><ChatDotRound /></el-icon>
              <span>AI 对话</span>
            </el-menu-item>
            <el-menu-item index="/profile">
              <el-icon><User /></el-icon>
              <span>个人设置</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card>
                <template #header>
                  <span>今日营养摄入</span>
                </template>
                <div v-if="summary">
                  <p>总热量: {{ summary.totalCalories }} / {{ summary.calorieGoal }} kcal</p>
                  <el-progress :percentage="summary.calorieProgress" :stroke-width="20" />
                  <p class="mt">蛋白质: {{ summary.totalProtein }}g / {{ summary.proteinGoal }}g</p>
                  <el-progress :percentage="summary.proteinProgress" :stroke-width="20" color="#67C23A" />
                  <p class="mt">脂肪: {{ summary.totalFat }}g / {{ summary.fatGoal }}g</p>
                  <el-progress :percentage="summary.fatProgress" :stroke-width="20" color="#E6A23C" />
                  <p class="mt">碳水: {{ summary.totalCarbohydrate }}g / {{ summary.carbGoal }}g</p>
                  <el-progress :percentage="summary.carbProgress" :stroke-width="20" color="#409EFF" />
                </div>
                <el-skeleton v-else loading animated />
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card>
                <template #header>
                  <span>快捷操作</span>
                </template>
                <el-space wrap>
                  <el-button type="primary" @click="$router.push('/diet')">添加饮食记录</el-button>
                  <el-button type="success" @click="$router.push('/chat')">AI 饮食咨询</el-button>
                  <el-button type="warning" @click="$router.push('/goals')">设置目标</el-button>
                  <el-button @click="$router.push('/analysis')">查看分析</el-button>
                </el-space>
              </el-card>
              <el-card class="mt">
                <template #header>
                  <span>今日饮食记录</span>
                </template>
                <el-empty v-if="!records || records.length === 0" description="今日暂无饮食记录" />
                <el-timeline v-else>
                  <el-timeline-item v-for="r in records.slice(0, 5)" :key="r.id" :timestamp="r.mealTime">
                    {{ r.foodName }} - {{ r.calories }} kcal
                  </el-timeline-item>
                </el-timeline>
              </el-card>
            </el-col>
          </el-row>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { analysisAPI, dietAPI } from '../api'

const router = useRouter()
const username = localStorage.getItem('username') || '用户'
const activeMenu = ref('/')
const summary = ref(null)
const records = ref([])

const formatDate = (offset = 0) => {
  const d = new Date()
  d.setDate(d.getDate() + offset)
  return d.toISOString().split('T')[0]
}

onMounted(async () => {
  try {
    const today = formatDate()
    summary.value = await analysisAPI.getDaily(today)
    records.value = await dietAPI.getRecords(today)
  } catch (e) {
    console.error(e)
  }
})

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style scoped>
.home-container {
  height: 100vh;
}
.el-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #409EFF;
  color: white;
}
.header-left h2 {
  margin: 0;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}
.el-aside {
  background: #f5f5f5;
}
.el-main {
  background: #f0f2f5;
  padding: 20px;
}
.mt {
  margin-top: 15px;
}
</style>
