package com.dietagent.android.data.remote

import com.dietagent.android.data.remote.dto.AgentResponse
import com.dietagent.android.data.remote.dto.ChatMessageDto
import com.dietagent.android.data.remote.dto.DietRecordRequest
import com.dietagent.android.data.remote.dto.DietRecordResponse
import com.dietagent.android.data.remote.dto.FavoriteFoodRequest
import com.dietagent.android.data.remote.dto.FavoriteFoodResponse
import com.dietagent.android.data.remote.dto.GoalSetting
import com.dietagent.android.data.remote.dto.LoginRequest
import com.dietagent.android.data.remote.dto.LoginResponse
import com.dietagent.android.data.remote.dto.NutritionSummaryResponse
import com.dietagent.android.data.remote.dto.RegisterRequest
import com.dietagent.android.data.remote.dto.UsageStatsResponse
import com.dietagent.android.data.remote.dto.UserProfile
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 后端全部 REST 接口。userId 一律由后端从 Bearer JWT 解析，
 * 客户端不传 userId（对应后端的 @AuthenticationPrincipal 安全模型）。
 */
interface ApiService {

    // ---- auth（免鉴权）----
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): LoginResponse

    // ---- diet ----
    @GET("api/diet/records")
    suspend fun getRecords(@Query("date") date: String): List<DietRecordResponse>

    @POST("api/diet/records")
    suspend fun addRecord(@Body body: DietRecordRequest): DietRecordResponse

    @PUT("api/diet/records/{id}")
    suspend fun updateRecord(@Path("id") id: Long, @Body body: DietRecordRequest): DietRecordResponse

    @DELETE("api/diet/records/{id}")
    suspend fun deleteRecord(@Path("id") id: Long)

    // ---- analysis ----
    @GET("api/analysis/daily")
    suspend fun getDailySummary(@Query("date") date: String?): NutritionSummaryResponse

    @GET("api/analysis/weekly")
    suspend fun getWeeklySummary(@Query("startDate") startDate: String?): NutritionSummaryResponse

    // ---- goals ----
    @GET("api/goals/daily")
    suspend fun getDailyGoal(@Query("date") date: String?): GoalSetting

    @POST("api/goals/daily")
    suspend fun setDailyGoal(@Body body: GoalSetting): GoalSetting

    @GET("api/goals/history")
    suspend fun getGoalHistory(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
    ): List<GoalSetting>

    @POST("api/goals/achieve")
    suspend fun markGoalAchieved(@Query("date") date: String)

    @GET("api/goals/profile")
    suspend fun getProfile(): UserProfile

    @PUT("api/goals/profile")
    suspend fun updateProfile(@Body body: UserProfile): UserProfile

    // ---- favorites ----
    @GET("api/favorites")
    suspend fun listFavorites(): List<FavoriteFoodResponse>

    @POST("api/favorites")
    suspend fun addFavorite(@Body body: FavoriteFoodRequest): FavoriteFoodResponse

    @PUT("api/favorites/{id}")
    suspend fun updateFavorite(
        @Path("id") id: Long,
        @Body body: FavoriteFoodRequest,
    ): FavoriteFoodResponse

    @DELETE("api/favorites/{id}")
    suspend fun deleteFavorite(@Path("id") id: Long)

    // ---- agent（SSE 流式单独走 SseClient）----
    @POST("api/agent/chat")
    suspend fun chat(@Body body: Map<String, String>): AgentResponse

    @POST("api/agent/chat/stop")
    suspend fun stopGeneration()

    @GET("api/agent/memory")
    suspend fun getHistory(): List<ChatMessageDto>

    @DELETE("api/agent/memory")
    suspend fun clearMemory(): AgentResponse

    @GET("api/agent/usage")
    suspend fun getUsage(): UsageStatsResponse
}
