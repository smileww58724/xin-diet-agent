package com.dietagent.android.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dietagent.android.data.remote.SseEvent
import com.dietagent.android.data.remote.dto.ChatMessageDto
import com.dietagent.android.data.remote.dto.NutritionSummaryResponse
import com.dietagent.android.data.repo.AgentRepository
import com.dietagent.android.data.repo.AnalysisRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val role: String, val content: String, val pending: Boolean = false)

class ChatViewModel(
    private val agentRepo: AgentRepository,
    private val analysisRepo: AnalysisRepository,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "assistant",
                content = "您好！我是您的 AI 饮食管理助手。\n1. 分析今日饮食情况\n2. 推荐合适的食物\n3. 解答营养问题\n4. 直接告诉我吃了什么，帮您记录",
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _summary = MutableStateFlow<NutritionSummaryResponse?>(null)
    val summary: StateFlow<NutritionSummaryResponse?> = _summary.asStateFlow()

    private var streamJob: Job? = null

    init {
        loadHistory()
        loadSummary()
    }

    fun send(message: String) {
        if (message.isBlank() || _loading.value) return
        _error.value = null
        _messages.value = _messages.value + ChatMessage("user", message)
        _messages.value = _messages.value + ChatMessage("assistant", "", pending = true)
        _loading.value = true

        streamJob = viewModelScope.launch {
            try {
                agentRepo.chatStream(message).collect { event ->
                    when (event) {
                        is SseEvent.Delta -> appendDelta(event.text)
                        is SseEvent.FullResult -> replacePending(event.content)
                    }
                }
                finalizePending()
                loadSummary()
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                // HTTP 错误（如 401/403/限流）或网络错误：移除空的占位消息
                val list = _messages.value.toMutableList()
                if (list.isNotEmpty() && list.last().pending) {
                    list.removeAt(list.size - 1)
                }
                _messages.value = list
                _error.value = e.message ?: "发送失败"
            } finally {
                _loading.value = false
            }
        }
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
        viewModelScope.launch { runCatching { agentRepo.stopGeneration() } }
        finalizePending()
        _loading.value = false
    }

    fun clear() {
        _messages.value = listOf(ChatMessage("assistant", "对话已清空。请问有什么可以帮到您？"))
        viewModelScope.launch { runCatching { agentRepo.clearMemory() } }
    }

    fun logout() {
        streamJob?.cancel()
    }

    private fun appendDelta(text: String) {
        val list = _messages.value.toMutableList()
        val idx = list.indexOfLast { it.pending }
        if (idx >= 0) {
            list[idx] = list[idx].copy(content = list[idx].content + text)
            _messages.value = list
        }
    }

    private fun replacePending(content: String) {
        val list = _messages.value.toMutableList()
        val idx = list.indexOfLast { it.pending }
        if (idx >= 0) {
            list[idx] = list[idx].copy(content = content, pending = false)
            _messages.value = list
        }
    }

    private fun finalizePending() {
        val list = _messages.value.toMutableList()
        val idx = list.indexOfLast { it.pending }
        if (idx >= 0) {
            list[idx] = list[idx].copy(pending = false)
            _messages.value = list
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            runCatching {
                val history: List<ChatMessageDto> = agentRepo.getHistory()
                if (history.isNotEmpty()) {
                    _messages.value = history.map { ChatMessage(it.role, it.content) }
                }
            }
        }
    }

    private fun loadSummary() {
        viewModelScope.launch {
            runCatching { analysisRepo.getDaily() }.onSuccess { _summary.value = it }
        }
    }
}
