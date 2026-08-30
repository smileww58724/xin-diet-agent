<template>
  <div class="chat-layout">
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>🍽️ 饮食助手</h3>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item active">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 对话</span>
        </router-link>
        <router-link to="/diet" class="nav-item">
          <el-icon><Food /></el-icon>
          <span>饮食记录</span>
        </router-link>
        <router-link to="/analysis" class="nav-item">
          <el-icon><DataAnalysis /></el-icon>
          <span>营养分析</span>
        </router-link>
        <router-link to="/goals" class="nav-item">
          <el-icon><Aim /></el-icon>
          <span>目标设定</span>
        </router-link>
        <router-link to="/profile" class="nav-item">
          <el-icon><User /></el-icon>
          <span>个人设置</span>
        </router-link>
      </nav>
    </div>

    <div class="chat-main">
      <div class="chat-header">
        <span class="chat-title">AI 饮食助手</span>
        <div class="header-right">
          <el-button @click="handleClear" link>清空对话</el-button>
          <el-dropdown @command="handleCommand" trigger="hover">
            <el-avatar class="user-avatar">{{ username }}</el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人设置</el-dropdown-item>
                <el-dropdown-item command="goals">目标设定</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div class="messages" ref="messagesRef">
        <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
          <div class="message-content">
            <el-avatar :icon="msg.role === 'user' ? 'User' : 'ChatDotRound'" />
            <div :class="['text', { streaming: msg.pending }]" v-html="formatMessage(msg.content)"></div>
          </div>
        </div>
        <div v-if="loading" class="loading-tip">
          <span class="dot-animation">●</span> {{ thinkingText }}
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="inputMessage"
          placeholder="请输入您的饮食问题..."
          @keyup.enter="handleSend"
          :disabled="loading"
        >
          <template #append>
            <el-button
              :type="loading ? 'danger' : 'primary'"
              :disabled="loading ? false : !inputMessage"
              @click="loading ? handleStop() : handleSend()"
            >
              {{ loading ? '停止' : '发送' }}
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { agentAPI } from '../api'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { useSseChat } from '../composables/useSseChat'

const router = useRouter()
const username = localStorage.getItem('username') || '用户'
const { loading, sendMessage, stop } = useSseChat()

marked.setOptions({
  breaks: true,
  gfm: true,
})

const WELCOME = '您好！我是您的 AI 饮食管理助手。我可以帮您：\n1. 分析今日饮食情况\n2. 推荐合适的食物\n3. 解答营养问题\n4. 生成个性化食谱\n5. 直接告诉我吃了什么，帮您记录\n\n请问有什么可以帮到您的？'

const messagesRef = ref()
const inputMessage = ref('')
const thinkingText = ref('正在思考...')
const messages = ref([
  {
    role: 'assistant',
    content: WELCOME,
  },
])

// 切页返回时从持久化记忆恢复对话历史（后端按用户隔离存储）
onMounted(async () => {
  try {
    const history = await agentAPI.history()
    if (Array.isArray(history) && history.length > 0) {
      messages.value = history.map(m => ({ role: m.role, content: m.content, pending: false }))
    }
  } catch (e) {
    console.error('恢复对话历史失败', e)
  }
})

let scrollTimer = null
const scrollToBottom = () => {
  if (scrollTimer) return
  scrollTimer = requestAnimationFrame(() => {
    scrollTimer = null
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const deriveThinkingText = (text) => {
  if (text.includes('今天') || text.includes('吃了')) return '正在查询饮食记录...'
  if (text.includes('营养') || text.includes('摄入')) return '正在分析营养数据...'
  if (text.includes('目标') || text.includes('计划')) return '正在查看目标计划...'
  if (text.includes('推荐') || text.includes('建议')) return '正在生成建议...'
  return '正在思考...'
}

const handleSend = async () => {
  if (!inputMessage.value.trim() || loading.value) return

  const userMessage = inputMessage.value.trim()
  thinkingText.value = deriveThinkingText(userMessage)

  messages.value.push({ role: 'user', content: userMessage })
  messages.value.push({ role: 'assistant', content: '', pending: true })
  const assistantIndex = messages.value.length - 1
  inputMessage.value = ''
  scrollToBottom()

  // 防抖更新：每50ms最多滚动一次
  let updateTimer = null
  const flushContent = () => {
    if (updateTimer) clearTimeout(updateTimer)
    updateTimer = setTimeout(scrollToBottom, 50)
  }

  const result = await sendMessage(userMessage, {
    onDelta: (text) => {
      if (messages.value[assistantIndex]) {
        messages.value[assistantIndex].content += text
        flushContent()
      }
    },
    onFullResult: (fullResult) => {
      if (messages.value[assistantIndex]) {
        messages.value[assistantIndex].content = fullResult
        flushContent()
      }
    },
  })
  if (updateTimer) clearTimeout(updateTimer)

  if (result.ok) {
    // 流正常结束
  } else if (result.aborted) {
    ElMessage.info('已停止生成')
  } else if (result.status === 401 || result.status === 403) {
    // 流式请求走 fetch 不经过 axios 拦截器，认证失效需自行处理
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('userId')
    ElMessage.error('登录已过期，请重新登录')
    router.push('/login')
    return
  } else {
    console.error('流式请求失败', result)
    ElMessage.error(result.status ? `请求失败（${result.status}）` : '发送失败，请重试')
    messages.value.splice(assistantIndex, 1)
    return
  }

  // 成功与停止：保留内容，结束流式渲染状态
  if (messages.value[assistantIndex]) {
    messages.value[assistantIndex].pending = false
  }
  scrollToBottom()
}

const handleStop = () => {
  stop()
}

const handleClear = async () => {
  messages.value = [
    {
      role: 'assistant',
      content: '对话已清空。请问有什么可以帮到您？',
    },
  ]
  // 同步清空后端持久化记忆，否则切页返回历史又会被恢复
  try {
    await agentAPI.clearMemory()
  } catch (e) {
    console.error('清空服务端对话记忆失败', e)
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('userId')
  router.push('/login')
}

const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'goals') {
    router.push('/goals')
  }
}

const formatMessage = (text) => {
  if (!text) return ''
  // 流式阶段把图片标记渲染为占位提示（避免裸标记闪现）；
  // 结束后内容会被 [FULL_RESULT] 全量替换为图片卡片，不再含标记
  const display = text.replace(/\[搜索图片:\s*([^\]]*)\]/g, '_🖼️ 正在搜索「$1」的图片…_')
  // AI 输出与富媒体卡片经 v-html 渲染，必须过净化器防 XSS
  return DOMPurify.sanitize(marked.parse(display), { ADD_ATTR: ['target'] })
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 200px;
  background: #fff;
  border-right: 1px solid #e4e4e7;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.sidebar-nav {
  flex: 1;
  padding: 10px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  color: #666;
  text-decoration: none;
  transition: all 0.2s;
}

.nav-item:hover {
  background: #f5f5f5;
  color: #409EFF;
}

.nav-item.router-link-active,
.nav-item.active {
  background: #ecf5ff;
  color: #409EFF;
  border-right: 3px solid #409EFF;
}

.nav-item span {
  font-size: 14px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-avatar {
  cursor: pointer;
  background: #409EFF;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message-content {
  display: flex;
  gap: 10px;
  max-width: 70%;
}

.message-content > .el-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
}

.message-content > :last-child {
  flex: 1;
  min-width: 0;
}

.message.user .message-content {
  flex-direction: row-reverse;
}

.text {
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.7;
  word-wrap: break-word;
  overflow-wrap: break-word;
  white-space: normal;
}

/* 流式输出时的样式 */
.message.assistant .text.streaming {
  white-space: pre-wrap;
}

.message.user .text {
  background: #409EFF;
  color: white;
}

.message.assistant .text {
  background: #fff;
  color: #333;
}

.input-area {
  padding: 20px;
  background: #fff;
  border-top: 1px solid #eee;
}

.loading-tip {
  font-size: 12px;
  color: #999;
  padding-left: 50px;
  margin-top: -10px;
}

.dot-animation {
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>

<style>
/* AI 消息内容排版 */
.message.assistant .text {
  line-height: 1.8;
  font-size: 14px;
}

.message.assistant .text h1,
.message.assistant .text h2,
.message.assistant .text h3,
.message.assistant .text h4 {
  margin: 1em 0 0.5em 0;
  font-weight: 600;
}

.message.assistant .text h1 { font-size: 1.4em; }
.message.assistant .text h2 { font-size: 1.2em; }
.message.assistant .text h3 { font-size: 1.1em; }

.message.assistant .text p {
  margin: 0.8em 0;
}

.message.assistant .text ul,
.message.assistant .text ol {
  margin: 0.8em 0;
  padding-left: 1.5em;
}

.message.assistant .text li {
  margin: 0.4em 0;
}

.message.assistant .text table {
  border-collapse: collapse;
  margin: 1em 0;
  width: 100%;
}

.message.assistant .text table th,
.message.assistant .text table td {
  border: 1px solid #ddd;
  padding: 0.5em 0.8em;
  text-align: left;
}

.message.assistant .text table th {
  background: #f5f5f5;
  font-weight: 600;
}

.message.assistant .text code {
  background: #f5f5f5;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-size: 0.9em;
}

.message.assistant .text pre {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 1em;
  border-radius: 8px;
  overflow-x: auto;
  margin: 1em 0;
}

.message.assistant .text pre code {
  background: none;
  padding: 0;
}

.message.assistant .text strong {
  font-weight: 600;
}

.message.assistant .text blockquote {
  border-left: 4px solid #409EFF;
  margin: 1em 0;
  padding: 0.5em 1em;
  background: #f8f9fa;
}

.message.assistant .text hr {
  border: none;
  border-top: 1px solid #eee;
  margin: 1.5em 0;
  display: block;
}

/* 图片样式 */
.message.assistant .text .pexels-images {
  margin: 1em 0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.message.assistant .text .pexels-images p {
  width: 100%;
  margin: 0 0 0.5em 0;
  font-weight: 600;
}
.message.assistant .text .pexels-image-item {
  flex: 1;
  min-width: 150px;
  max-width: 200px;
}
.message.assistant .text .pexels-image-item img {
  width: 100%;
  border-radius: 8px;
  object-fit: cover;
}
.message.assistant .text .pexels-image-item .pexels-credit {
  font-size: 12px;
  color: #999;
  margin: 5px 0 0 0;
}
</style>
