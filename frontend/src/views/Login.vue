<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand">
        <span class="brand-logo">🥗</span>
        <span class="brand-name">膳灵</span>
      </div>
      <h1 class="auth-title">欢迎回来</h1>
      <p class="auth-sub">登录后继续你的饮食管理</p>

      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" class="auth-btn" size="large" @click="handleLogin" :loading="loading">登录</el-button>
      </el-form>

      <div class="auth-footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
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
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await authAPI.login(form)
    localStorage.setItem('token', res.token)
    localStorage.setItem('username', res.username)
    localStorage.setItem('userId', res.userId)
    ElMessage.success('登录成功')
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
