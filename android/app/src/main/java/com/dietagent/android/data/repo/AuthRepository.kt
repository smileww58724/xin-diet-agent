package com.dietagent.android.data.repo

import com.dietagent.android.data.local.TokenStore
import com.dietagent.android.data.remote.ApiService
import com.dietagent.android.data.remote.dto.LoginRequest
import com.dietagent.android.data.remote.dto.LoginResponse
import com.dietagent.android.data.remote.dto.RegisterRequest

class AuthRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
) {
    suspend fun login(username: String, password: String): LoginResponse {
        val resp = api.login(LoginRequest(username, password))
        tokenStore.save(resp.token, resp.username, resp.nickname, resp.userId)
        return resp
    }

    suspend fun register(username: String, password: String, nickname: String?): LoginResponse {
        val resp = api.register(RegisterRequest(username, password, nickname))
        tokenStore.save(resp.token, resp.username, resp.nickname, resp.userId)
        return resp
    }

    suspend fun logout() = tokenStore.clear()

    suspend fun isLoggedIn(): Boolean = !tokenStore.getToken().isNullOrBlank()
}
