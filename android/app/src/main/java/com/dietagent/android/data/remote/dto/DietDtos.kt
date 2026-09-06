package com.dietagent.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DietRecordRequest(
    val foodName: String,
    val mealType: String,
    val portionSize: String? = null,
    val calories: Int? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbohydrate: Double? = null,
    val mealTime: String, // yyyy-MM-dd'T'HH:mm:ss
)

@Serializable
data class DietRecordResponse(
    val id: Long,
    val foodName: String,
    val mealType: String,
    val portionSize: String? = null,
    val calories: Int? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbohydrate: Double? = null,
    val mealTime: String? = null,
)
