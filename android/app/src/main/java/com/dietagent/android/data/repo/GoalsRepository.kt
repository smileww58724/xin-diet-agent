package com.dietagent.android.data.repo

import com.dietagent.android.data.remote.ApiService
import com.dietagent.android.data.remote.dto.UserProfile

class GoalsRepository(private val api: ApiService) {

    suspend fun getProfile(): UserProfile = api.getProfile()

    suspend fun updateProfile(
        nickname: String?,
        gender: String?,
        age: Int?,
        height: Double?,
        weight: Double?,
        activityLevel: String?,
        goalType: String?,
        dailyCalorieGoal: Int?,
        proteinGoal: Int?,
        fatGoal: Int?,
        carbGoal: Int?,
    ): UserProfile = api.updateProfile(
        UserProfile(
            nickname = nickname,
            gender = gender,
            age = age,
            height = height,
            weight = weight,
            activityLevel = activityLevel,
            goalType = goalType,
            dailyCalorieGoal = dailyCalorieGoal,
            proteinGoal = proteinGoal,
            fatGoal = fatGoal,
            carbGoal = carbGoal,
        )
    )
}
