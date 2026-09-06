package com.dietagent.android.data.repo

import com.dietagent.android.data.remote.ApiService
import com.dietagent.android.data.remote.dto.NutritionSummaryResponse

class AnalysisRepository(private val api: ApiService) {

    suspend fun getDaily(date: String? = null): NutritionSummaryResponse = api.getDailySummary(date)

    suspend fun getWeekly(startDate: String? = null): NutritionSummaryResponse =
        api.getWeeklySummary(startDate)
}
