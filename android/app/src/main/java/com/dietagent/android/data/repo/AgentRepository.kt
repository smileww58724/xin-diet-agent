package com.dietagent.android.data.repo

import com.dietagent.android.data.remote.ApiService
import com.dietagent.android.data.remote.SseClient
import com.dietagent.android.data.remote.SseEvent
import com.dietagent.android.data.remote.dto.ChatMessageDto
import com.dietagent.android.data.remote.dto.UsageStatsResponse
import kotlinx.coroutines.flow.Flow

class AgentRepository(
    private val api: ApiService,
    private val sseClient: SseClient,
) {
    fun chatStream(message: String): Flow<SseEvent> = sseClient.chatStream(message)

    suspend fun stopGeneration() = api.stopGeneration()

    suspend fun getHistory(): List<ChatMessageDto> = api.getHistory()

    suspend fun clearMemory() = api.clearMemory()

    suspend fun getUsage(): UsageStatsResponse = api.getUsage()
}
