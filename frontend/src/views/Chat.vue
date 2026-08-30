<template>
  <div class="chat-page">
    <!-- 对话列 -->
    <section class="chat-col">
      <header class="chat-head">
        <h1>AI 对话</h1>
        <el-button link @click="handleClear">清空对话</el-button>
      </header>

      <div class="messages" ref="messagesRef">
        <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
          <div class="msg-avatar" :class="msg.role">
            <el-icon v-if="msg.role === 'assistant'"><ChatDotRound /></el-icon>
            <span v-else>{{ username.slice(0, 1).toUpperCase() }}</span>
          </div>
          <div class="msg-body">
            <div :class="['text', { streaming: msg.pending }]" v-html="formatMessage(msg.content)"></div>
          </div>
        </div>
        <div v-if="loading" class="loading-tip">
          <span class="dot-animation">●</span> {{ thinkingText }}
        </div>
      </div>

      <div class="input-bar">
        <el-input
          v-model="inputMessage"
          class="chat-input"
          placeholder="说说你吃了什么，或问任何营养问题…"
          @keyup.enter="handleSend"
          :disabled="loading"
        />
        <button
          class="send-btn"
          :class="{ stop: loading }"
          :disabled="!loading && !inputMessage.trim()"
          @click="loading ? handleStop() : handleSend()"
        >
          <el-icon v-if="loading" :size="16"><VideoPause /></el-icon>
          <el-icon v-else :size="16"><Promotion /></el-icon>
        </button>
      </div>
    </section>

    <!-- 右侧今日概览 -->
    <aside class="chat-side">
      <h3 class="side-title">今日概览</h3>
      <div class="stat-panel">
        <template v-if="summary">
          <div class="kcal-row">
            <span class="kcal-num">{{ summary.totalCalories }}</span>
            <span class="kcal-goal">/ {{ summary.calorieGoal }} kcal</span>
          </div>
          <div class="bar"><div class="bar-fill" :style="{ width: summary.calorieProgress + '%' }"></div></div>
          <div class="macro" v-for="m in macros" :key="m.label">
            <span class="macro-label">{{ m.label }}</span>
            <div class="macro-bar"><div class="bar-fill" :style="{ width: m.value + '%' }"></div></div>
            <span class="macro-pct">{{ Math.round(m.value) }}%</span>
          </div>
        </template>
        <div v-else class="side-skeleton">加载中…</div>
      </div>

      <div class="side-tip">
        <p class="tip-title">💡 动嘴记录</p>
        <p class="tip-text">直接告诉我"我中午吃了一碗米饭"，我会帮你记录并估算营养。</p>
      </div>

      <router-link to="/analysis" class="side-link">查看完整分析 →</router-link>
    </aside>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { agentAPI, analysisAPI } from '../api'
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
const summary = ref(null)
const messages = ref([
  {
    role: 'assistant',
    content: WELCOME,
  },
])

const macros = computed(() => {
  if (!summary.value) return []
  return [
    { label: '蛋白质', value: summary.value.proteinProgress },
    { label: '脂肪', value: summary.value.fatProgress },
    { label: '碳水', value: summary.value.carbProgress },
  ]
})

const loadSummary = async () => {
  try {
    summary.value = await analysisAPI.getDaily()
  } catch (e) {
    console.error('加载今日概览失败', e)
  }
}

// 切页返回时从持久化记忆恢复对话历史（后端按用户隔离存储）
onMounted(async () => {
  loadSummary()
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
    // 流正常结束；AI 可能刚记录了饮食，刷新概览
    loadSummary()
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
.chat-page {
  flex: 1;
  display: flex;
  min-height: 0;
}

/* ---- 对话列 ---- */
.chat-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px 12px;
}
.chat-head h1 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 8px 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  display: flex;
  gap: 10px;
}
.message.user {
  flex-direction: row-reverse;
}
.msg-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}
.msg-avatar.assistant {
  background: var(--ink);
  color: #fff;
}
.msg-avatar.user {
  background: var(--panel-soft);
  border: 1px solid var(--line);
  color: var(--ink);
}
.msg-body {
  max-width: 72%;
  min-width: 0;
}
.text {
  padding: 11px 15px;
  border-radius: 16px;
  line-height: 1.7;
  word-wrap: break-word;
  overflow-wrap: break-word;
  font-size: 14px;
}
.message.assistant .text {
  background: var(--panel);
  border: 1px solid var(--line);
  border-top-left-radius: 4px;
  color: var(--text-1);
}
.message.user .text {
  background: var(--ink);
  color: #fff;
  border-top-right-radius: 4px;
  white-space: pre-wrap;
}
.message.assistant .text.streaming {
  white-space: pre-wrap;
}

.loading-tip {
  font-size: 12px;
  color: var(--text-3);
  padding-left: 42px;
}
.dot-animation {
  animation: pulse 1s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* ---- 输入栏 ---- */
.input-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 24px 20px;
}
.chat-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-pill);
  padding: 6px 20px;
  box-shadow: 0 0 0 1px var(--line) inset;
  background: var(--panel);
}
.chat-input :deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 1.5px var(--ink) inset !important;
}
.send-btn {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border: none;
  border-radius: 999px;
  background: var(--ink);
  color: #fff;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: opacity 0.12s, transform 0.12s;
}
.send-btn:hover {
  transform: scale(1.05);
}
.send-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
  transform: none;
}
.send-btn.stop {
  background: #d54242;
}

/* ---- 右侧概览 ---- */
.chat-side {
  width: 296px;
  flex-shrink: 0;
  border-left: 1px solid var(--line);
  background: var(--panel);
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
@media (max-width: 1080px) {
  .chat-side {
    display: none;
  }
}
.side-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
}
.kcal-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.kcal-num {
  font-size: 30px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: -0.5px;
}
.kcal-goal {
  font-size: 12px;
  color: var(--text-3);
}
.bar {
  height: 6px;
  background: #e8e8ea;
  border-radius: 999px;
  overflow: hidden;
  margin: 10px 0 16px;
}
.bar-fill {
  height: 100%;
  background: var(--ink);
  border-radius: 999px;
  transition: width 0.4s ease;
}
.macro {
  display: grid;
  grid-template-columns: 44px 1fr 36px;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.macro-label {
  font-size: 12px;
  color: var(--text-2);
}
.macro-bar {
  height: 5px;
  background: #e8e8ea;
  border-radius: 999px;
  overflow: hidden;
}
.macro-pct {
  font-size: 11px;
  color: var(--text-3);
  text-align: right;
}
.side-skeleton {
  color: var(--text-3);
  font-size: 13px;
  padding: 8px 0;
}
.side-tip {
  background: var(--panel-soft);
  border-radius: var(--radius-md);
  padding: 14px;
}
.tip-title {
  margin: 0 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}
.tip-text {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-2);
}
.side-link {
  font-size: 13px;
  color: var(--text-2);
  text-decoration: none;
  font-weight: 500;
}
.side-link:hover {
  color: var(--ink);
}
</style>

<style>
/* AI 消息内容排版（markdown 全局样式） */
.message.assistant .text {
  line-height: 1.8;
}
.message.assistant .text h1,
.message.assistant .text h2,
.message.assistant .text h3,
.message.assistant .text h4 {
  margin: 0.9em 0 0.5em;
  font-weight: 700;
  color: var(--ink);
}
.message.assistant .text h1 { font-size: 1.3em; }
.message.assistant .text h2 { font-size: 1.18em; }
.message.assistant .text h3 { font-size: 1.08em; }
.message.assistant .text p {
  margin: 0.7em 0;
}
.message.assistant .text ul,
.message.assistant .text ol {
  margin: 0.7em 0;
  padding-left: 1.4em;
}
.message.assistant .text li {
  margin: 0.3em 0;
}
.message.assistant .text table {
  border-collapse: collapse;
  margin: 0.9em 0;
  width: 100%;
  font-size: 13px;
}
.message.assistant .text table th,
.message.assistant .text table td {
  border: 1px solid var(--line);
  padding: 6px 10px;
  text-align: left;
}
.message.assistant .text table th {
  background: var(--panel-soft);
  font-weight: 600;
}
.message.assistant .text code {
  background: var(--panel-soft);
  padding: 0.15em 0.4em;
  border-radius: 5px;
  font-size: 0.88em;
  color: var(--ink);
}
.message.assistant .text pre {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 1em;
  border-radius: 10px;
  overflow-x: auto;
  margin: 0.8em 0;
}
.message.assistant .text pre code {
  background: none;
  padding: 0;
}
.message.assistant .text strong {
  font-weight: 600;
}
.message.assistant .text blockquote {
  border-left: 3px solid var(--line);
  margin: 0.8em 0;
  padding: 0.4em 1em;
  background: var(--panel-soft);
  border-radius: 0 8px 8px 0;
  color: var(--text-2);
}
.message.assistant .text hr {
  border: none;
  border-top: 1px solid var(--line);
  margin: 1.2em 0;
}

/* Pexels 图片卡片 */
.message.assistant .text .pexels-images {
  margin: 0.9em 0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.message.assistant .text .pexels-images p {
  width: 100%;
  margin: 0 0 4px;
  font-weight: 600;
  color: var(--ink);
}
.message.assistant .text .pexels-image-item {
  flex: 1;
  min-width: 140px;
  max-width: 190px;
}
.message.assistant .text .pexels-image-item img {
  width: 100%;
  border-radius: 10px;
  object-fit: cover;
}
.message.assistant .text .pexels-image-item .pexels-credit {
  font-size: 11px;
  color: var(--text-3);
  margin: 4px 0 0;
}
</style>
