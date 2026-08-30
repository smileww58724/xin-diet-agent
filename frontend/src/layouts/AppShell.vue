<template>
  <div class="shell" :class="{ collapsed }">
    <!-- 侧边栏 -->
    <aside class="shell-side">
      <div class="side-head">
        <div class="brand">
          <span class="brand-logo">🥗</span>
          <span v-if="!collapsed" class="brand-name">轻食 AI</span>
        </div>
        <button class="collapse-btn" :title="collapsed ? '展开' : '收起'" @click="collapsed = !collapsed">
          <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
        </button>
      </div>

      <nav class="side-nav">
        <router-link
          v-for="item in nav"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: route.path === item.path }"
          :title="collapsed ? item.label : undefined"
        >
          <el-icon :size="17"><component :is="item.icon" /></el-icon>
          <span v-if="!collapsed">{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="side-foot">
        <template v-if="!collapsed">
          <div class="user-row">
            <span class="user-avatar">{{ username.slice(0, 1).toUpperCase() }}</span>
            <span class="user-name" :title="username">{{ username }}</span>
            <button class="logout-btn" title="退出登录" @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
            </button>
          </div>
        </template>
        <button v-else class="logout-btn collapsed-logout" title="退出登录" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
        </button>
      </div>
    </aside>

    <!-- 内容区 -->
    <main class="shell-main" :class="{ flush }">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Food, DataAnalysis, Aim, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const username = localStorage.getItem('username') || '用户'
const collapsed = ref(false)

/** 对话页等自管滚动的页面在路由 meta 里声明 flush，内容区即不带内边距 */
const flush = computed(() => !!route.meta.flush)

const nav = [
  { path: '/', label: 'AI 对话', icon: ChatDotRound },
  { path: '/diet', label: '饮食记录', icon: Food },
  { path: '/analysis', label: '营养分析', icon: DataAnalysis },
  { path: '/goals', label: '目标设定', icon: Aim },
  { path: '/profile', label: '个人设置', icon: Setting },
]

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('userId')
  router.push('/login')
}
</script>

<style scoped>
.shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ---- 侧边栏 ---- */
.shell-side {
  width: 224px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--panel);
  border-right: 1px solid var(--line);
  transition: width 0.18s ease;
}
.shell.collapsed .shell-side {
  width: 68px;
}

.side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 14px 12px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}
.brand-logo {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: var(--ink);
  border-radius: 9px;
  font-size: 15px;
}
.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: -0.2px;
  white-space: nowrap;
}
.collapse-btn {
  border: none;
  background: transparent;
  color: var(--text-3);
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  display: grid;
  place-items: center;
}
.collapse-btn:hover {
  background: var(--panel-soft);
  color: var(--ink);
}

.side-nav {
  flex: 1;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  color: var(--text-2);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  transition: background 0.12s, color 0.12s;
}
.nav-item:hover {
  background: var(--hover);
  color: var(--ink);
}
.nav-item.active {
  background: var(--panel-soft);
  color: var(--ink);
  font-weight: 600;
}
.shell.collapsed .nav-item {
  justify-content: center;
  padding: 10px 0;
}

.side-foot {
  padding: 12px 10px;
  border-top: 1px solid var(--line);
}
.user-row {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}
.user-avatar {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: var(--panel-soft);
  border: 1px solid var(--line);
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: var(--ink);
}
.user-name {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.logout-btn {
  border: none;
  background: transparent;
  color: var(--text-3);
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  display: grid;
  place-items: center;
}
.logout-btn:hover {
  background: var(--panel-soft);
  color: var(--el-color-danger);
}
.collapsed-logout {
  width: 100%;
}

/* ---- 内容区 ---- */
.shell-main {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 28px 32px;
}
.shell-main.flush {
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
