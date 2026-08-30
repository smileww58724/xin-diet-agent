import { ref } from 'vue'
import { agentAPI } from '../api'

/**
 * SSE 流式对话连接管理：建立流式请求、按 SSE 规范增量解析、识别结束与全量替换标记、支持中止。
 *
 * 解析要点（曾因处理不当导致 markdown 换行全部丢失）：
 * - Spring 会把内容中的换行序列化成同一事件的多条 data: 行，规范要求以 \n 连接还原；
 * - data: 之后的空格属于内容，不能去除；
 * - 空行是一个 SSE 事件结束的边界；
 * - 流结束后需处理残留的最后一个不完整事件，避免丢尾。
 *
 * 解析与 UI 解耦：增量内容通过 onDelta / onFullResult 回调交给视图层渲染，
 * 本模块只负责连接、解析与中止，不感知消息列表与 DOM。
 */
export function useSseChat() {
  const loading = ref(false)
  let abortController = null

  /**
   * 发送一条消息并消费流式响应
   * @returns {{ok: boolean, status?: number, aborted?: boolean}}
   *          ok=成功结束；aborted=用户主动停止；status=HTTP 错误码（含 401/403 认证失效）
   */
  const sendMessage = async (message, { onDelta, onFullResult } = {}) => {
    loading.value = true
    abortController = new AbortController()
    try {
      const response = await agentAPI.chatStream(message, abortController.signal)
      if (!response.ok) {
        return { ok: false, status: response.status }
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let inDataEvent = false

      const handleData = (data) => {
        if (data === '[DONE]') return
        if (data.startsWith('[FULL_RESULT]')) {
          inDataEvent = false
          onFullResult?.(data.slice('[FULL_RESULT]'.length))
          return
        }
        onDelta?.(inDataEvent ? '\n' + data : data)
        inDataEvent = true
      }

      const handleLine = (rawLine) => {
        const line = rawLine.endsWith('\r') ? rawLine.slice(0, -1) : rawLine
        if (line.startsWith('data:')) {
          handleData(line.slice(5))
        } else if (line === '') {
          inDataEvent = false
        }
      }

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value)
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) handleLine(line)
      }
      if (buffer) {
        for (const line of buffer.split('\n')) handleLine(line)
      }
      return { ok: true }
    } catch (e) {
      if (e.name === 'AbortError') {
        return { ok: false, aborted: true }
      }
      console.error(e)
      return { ok: false }
    } finally {
      loading.value = false
      abortController = null
    }
  }

  /** 中断当前流：fetch 层立即断开，同时通知服务端取消上游 AI 调用 */
  const stop = () => {
    abortController?.abort()
    agentAPI.stopGeneration().catch(e => console.error('停止生成失败', e))
  }

  return { loading, sendMessage, stop }
}
