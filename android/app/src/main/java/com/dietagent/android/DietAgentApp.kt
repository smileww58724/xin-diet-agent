package com.dietagent.android

import android.app.Application
import com.dietagent.android.BuildConfig
import com.dietagent.android.data.local.TokenStore
import com.dietagent.android.data.remote.ApiService
import com.dietagent.android.data.remote.AuthInterceptor
import com.dietagent.android.data.remote.SseClient
import com.dietagent.android.data.repo.AgentRepository
import com.dietagent.android.data.repo.AnalysisRepository
import com.dietagent.android.data.repo.AuthRepository
import com.dietagent.android.data.repo.DietRepository
import com.dietagent.android.data.repo.FavoritesRepository
import com.dietagent.android.data.repo.GoalsRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/** 手动 DI 容器：进程内单例，供各 ViewModel 使用 */
class DietAgentApp : Application() {

    val tokenStore: TokenStore by lazy { TokenStore(this) }

    private val apiService: ApiService by lazy {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }

    private val sseClient by lazy { SseClient(BuildConfig.BASE_URL, tokenStore) }

    val authRepository by lazy { AuthRepository(apiService, tokenStore) }
    val dietRepository by lazy { DietRepository(apiService) }
    val analysisRepository by lazy { AnalysisRepository(apiService) }
    val goalsRepository by lazy { GoalsRepository(apiService) }
    val favoritesRepository by lazy { FavoritesRepository(apiService) }
    val agentRepository by lazy { AgentRepository(apiService, sseClient) }
}
