package com.dietagent.android.data.remote

import com.dietagent.android.data.local.TokenStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/** SSE 事件：增量文本 或 全量替换（富媒体图片卡片） */
sealed interface SseEvent {
    data class Delta(val text: String) : SseEvent
    data class FullResult(val content: String) : SseEvent
}

/**
 * AI 流式对话客户端：OkHttp 发起 POST /api/agent/chat/stream，逐行解析 SSE。
 * 解析逻辑与 Web 端 useSseChat 保持一致：
 *  - 行以 "data:" 开头，取第 6 字符起（data: 后的空格属于内容，不裁剪）
 *  - 空行 = 一个 SSE 事件结束
 *  - 同一事件的多条 data 行以换行连接还原（避免 markdown 换行丢失）
 *  - "[FULL_RESULT]" 前缀 → 全量替换当前消息（图片卡片）
 *  - "[DONE]" → 忽略
 * 停止/取消：协程取消时 awaitClose 执行 call.cancel()，立即断开上游大模型调用
 *  （与 Web 端 Reactor 取消传播、前端 AbortController 语义一致）。
 */
class SseClient(
    private val baseUrl: String,
    private val tokenStore: TokenStore,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS) // 长回答流式读取
        .build()

    fun chatStream(message: String): Flow<SseEvent> = callbackFlow {
        val token = runBlocking { tokenStore.getToken() }
        val request = Request.Builder()
            .url("${baseUrl}api/agent/chat/stream")
            .post(message.toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer ${token.orEmpty()}")
            .build()

        val call = client.newCall(request)
        var response: Response? = null
        try {
            response = call.execute()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val source = response.body?.source()
            var inDataEvent = false

            while (true) {
                val line = source?.readUtf8Line() ?: break
                val trimmed = if (line.endsWith('\r')) line.dropLast(1) else line
                when {
                    trimmed.startsWith("data:") -> {
                        val data = trimmed.substring(5)
                        when {
                            data == "[DONE]" -> Unit
                            data.startsWith("[FULL_RESULT]") -> {
                                inDataEvent = false
                                trySend(SseEvent.FullResult(data.substring("[FULL_RESULT]".length)))
                            }
                            else -> {
                                trySend(SseEvent.Delta(if (inDataEvent) "\n$data" else data))
                                inDataEvent = true
                            }
                        }
                    }
                    trimmed.isEmpty() -> inDataEvent = false
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isClosedForSend) close(e)
        } finally {
            response?.close()
            call.cancel()
        }

        awaitClose { call.cancel() }
    }
}
