package com.dietagent.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteFoodRequest(
    val foodName: String,
    val category: String? = null,
    val note: String? = null,
    val caloriesPer100g: Int? = null,
    val proteinPer100g: Double? = null,
    val fatPer100g: Double? = null,
    val carbPer100g: Double? = null,
)

@Serializable
data class FavoriteFoodResponse(
    val id: Long,
    val userId: Long? = null,
    val foodName: String,
    val category: String? = null,
    val note: String? = null,
    val caloriesPer100g: Int? = null,
    val proteinPer100g: Double? = null,
    val fatPer100g: Double? = null,
    val carbPer100g: Double? = null,
)
