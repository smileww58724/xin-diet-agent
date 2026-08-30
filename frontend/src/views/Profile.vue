<template>
  <div class="profile-page">
    <div class="page-head">
      <div>
        <h1>个人设置</h1>
        <div class="page-sub">身体数据越完整，AI 建议越精准</div>
      </div>
    </div>

    <div class="profile-grid">
      <div class="form-card">
        <div class="card-title">基本信息</div>
        <el-form :model="form" label-position="top">
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
          <div class="form-row">
            <el-form-item label="年龄" style="flex: 1">
              <el-input-number v-model="form.age" :min="1" :max="150" style="width: 100%" />
            </el-form-item>
            <el-form-item label="身高(cm)" style="flex: 1">
              <el-input-number v-model="form.height" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
            <el-form-item label="体重(kg)" style="flex: 1">
              <el-input-number v-model="form.weight" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </div>
          <el-form-item label="运动水平">
            <el-select v-model="form.activityLevel" placeholder="请选择" style="width: 100%">
              <el-option label="久坐" value="sedentary" />
              <el-option label="轻度" value="light" />
              <el-option label="中度" value="moderate" />
              <el-option label="活跃" value="active" />
              <el-option label="非常活跃" value="very_active" />
            </el-select>
          </el-form-item>
          <el-form-item label="目标类型">
            <el-select v-model="form.goalType" placeholder="请选择" style="width: 100%">
              <el-option label="减肥" value="lose_weight" />
              <el-option label="增肌" value="gain_weight" />
              <el-option label="维持" value="maintain" />
            </el-select>
          </el-form-item>
          <el-button type="primary" style="width: 100%" @click="handleSave" :loading="loading">保存</el-button>
        </el-form>
      </div>

      <div class="side-col">
        <div class="card-title">AI 用量统计</div>
        <div v-if="usage && usage.totalCalls > 0">
          <div class="stat-panel">
            <div class="usage-hero">
              <span class="usage-num">{{ usage.totalCalls }}</span>
              <span class="usage-label">次对话</span>
            </div>
            <div class="usage-line"><span>总 tokens</span><b>{{ fmt(usage.totalTokens) }}</b></div>
            <div class="usage-line"><span>提示 / 生成</span><b>{{ fmt(usage.totalPromptTokens) }} / {{ fmt(usage.totalCompletionTokens) }}</b></div>
          </div>
          <template v-if="usage.daily && usage.daily.length">
            <div class="sub-title">近 7 天</div>
            <div class="usage-table">
              <div v-for="d in usage.daily" :key="d.date" class="usage-row">
                <span>{{ d.date }}</span>
                <span>{{ d.calls }} 次</span>
                <span>{{ fmt(d.tokens) }} tokens</span>
              </div>
            </div>
          </template>
        </div>
        <div v-else class="usage-empty">还没有 AI 对话，去聊两句试试～</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { goalsAPI, agentAPI } from '../api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const usage = ref(null)
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

const fmt = (n) => (n == null ? '0' : Number(n).toLocaleString())

onMounted(async () => {
  try {
    const profile = await goalsAPI.getProfile()
    Object.assign(form, profile)
  } catch (e) {
    console.error(e)
  }
  // 用量统计是旁路数据，加载失败不打扰用户
  try {
    usage.value = await agentAPI.usage()
  } catch (e) {
    console.error('用量统计加载失败', e)
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
.profile-page {
  max-width: 880px;
  margin: 0 auto;
}
.profile-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 16px;
  align-items: start;
}
@media (max-width: 760px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
.form-card {
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
.form-row {
  display: flex;
  gap: 12px;
}
.side-col {
  min-width: 0;
}
.usage-hero {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}
.usage-num {
  font-size: 30px;
  font-weight: 700;
  color: var(--ink);
}
.usage-label {
  font-size: 12px;
  color: var(--text-3);
}
.usage-line {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-2);
  padding: 5px 0;
}
.usage-line b {
  color: var(--ink);
  font-weight: 600;
}
.sub-title {
  margin: 16px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}
.usage-table {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.usage-row {
  display: grid;
  grid-template-columns: 1fr 52px 1fr;
  gap: 6px;
  padding: 9px 12px;
  font-size: 12px;
  color: var(--text-2);
  border-bottom: 1px solid var(--line);
}
.usage-row:last-child {
  border-bottom: none;
}
.usage-empty {
  background: var(--panel-soft);
  border-radius: var(--radius-md);
  padding: 18px;
  font-size: 13px;
  color: var(--text-3);
  text-align: center;
}
</style>
