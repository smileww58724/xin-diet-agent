package com.dietagent.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NutritionSummaryResponse(
    val totalCalories: Int? = 0,
    val totalProtein: Double? = 0.0,
    val totalFat: Double? = 0.0,
    val totalCarbohydrate: Double? = 0.0,
    val calorieGoal: Int? = 0,
    val proteinGoal: Int? = 0,
    val fatGoal: Int? = 0,
    val carbGoal: Int? = 0,
    val calorieProgress: Double? = 0.0,
    val proteinProgress: Double? = 0.0,
    val fatProgress: Double? = 0.0,
    val carbProgress: Double? = 0.0,
)
