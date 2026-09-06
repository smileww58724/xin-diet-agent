package com.dietagent.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String? = null,
)

@Serializable
data class LoginResponse(
    val userId: Long,
    val token: String,
    val username: String,
    val nickname: String? = null,
)
