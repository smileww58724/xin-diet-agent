<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand">
        <span class="brand-logo">🥗</span>
        <span class="brand-name">膳灵</span>
      </div>
      <h1 class="auth-title">创建账号</h1>
      <p class="auth-sub">一分钟开始你的 AI 饮食管理</p>

      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（至少 6 位）" size="large" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（可选）" size="large" prefix-icon="UserFilled" />
        </el-form-item>
        <el-button type="primary" class="auth-btn" size="large" @click="handleRegister" :loading="loading">注册</el-button>
      </el-form>

      <div class="auth-footer">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  nickname: '',
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度3-50字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await authAPI.register(form)
    localStorage.setItem('token', res.token)
    localStorage.setItem('username', res.username)
    localStorage.setItem('userId', res.userId)
    ElMessage.success('注册成功')
    router.push('/')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 24px;
}
.auth-card {
  width: 400px;
  max-width: 100%;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 20px;
  padding: 36px 32px 28px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
}
.brand-logo {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  background: var(--ink);
  border-radius: 10px;
  font-size: 16px;
}
.brand-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
}
.auth-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: -0.3px;
}
.auth-sub {
  margin: 6px 0 26px;
  font-size: 13px;
  color: var(--text-3);
}
.auth-btn {
  width: 100%;
  margin-top: 6px;
}
.auth-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--text-3);
}
.auth-footer a {
  color: var(--ink);
  font-weight: 600;
  text-decoration: none;
}
</style>
