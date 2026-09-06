package com.dietagent.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
)

@Serializable
data class AgentResponse(
    val message: String? = null,
    val type: String? = null,
    val data: String? = null,
)

@Serializable
data class UsageStatsResponse(
    val totalCalls: Long? = 0,
    val totalPromptTokens: Long? = 0,
    val totalCompletionTokens: Long? = 0,
    val totalTokens: Long? = 0,
    val byModel: List<ModelStat>? = emptyList(),
    val daily: List<DailyStat>? = emptyList(),
)

@Serializable
data class ModelStat(
    val model: String,
    val calls: Long? = 0,
    val promptTokens: Long? = 0,
    val completionTokens: Long? = 0,
    val totalTokens: Long? = 0,
)

@Serializable
data class DailyStat(
    val date: String,
    val calls: Long? = 0,
    val tokens: Long? = 0,
)
