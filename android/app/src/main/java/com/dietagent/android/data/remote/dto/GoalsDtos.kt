package com.dietagent.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GoalSetting(
    val id: Long? = null,
    val userId: Long? = null,
    val goalDate: String? = null, // yyyy-MM-dd
    val targetCalories: Int? = null,
    val targetProtein: Int? = null,
    val targetFat: Int? = null,
    val targetCarb: Int? = null,
    val achieved: Boolean? = null,
)

@Serializable
data class UserProfile(
    val id: Long? = null,
    val username: String? = null,
    val nickname: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    val age: Int? = null,
    val gender: String? = null,
    val activityLevel: String? = null,
    val goalType: String? = null,
    val dailyCalorieGoal: Int? = null,
    val proteinGoal: Int? = null,
    val fatGoal: Int? = null,
    val carbGoal: Int? = null,
)
