package com.dietagent.android.data.repo

import com.dietagent.android.data.remote.ApiService
import com.dietagent.android.data.remote.dto.FavoriteFoodRequest
import com.dietagent.android.data.remote.dto.FavoriteFoodResponse

class FavoritesRepository(private val api: ApiService) {

    suspend fun list(): List<FavoriteFoodResponse> = api.listFavorites()

    suspend fun add(
        foodName: String,
        category: String?,
        note: String?,
        caloriesPer100g: Int?,
        proteinPer100g: Double?,
        fatPer100g: Double?,
        carbPer100g: Double?,
    ): FavoriteFoodResponse = api.addFavorite(
        FavoriteFoodRequest(
            foodName = foodName,
            category = category,
            note = note,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            fatPer100g = fatPer100g,
            carbPer100g = carbPer100g,
        )
    )

    suspend fun update(
        id: Long,
        foodName: String,
        category: String?,
        note: String?,
        caloriesPer100g: Int?,
        proteinPer100g: Double?,
        fatPer100g: Double?,
        carbPer100g: Double?,
    ): FavoriteFoodResponse = api.updateFavorite(
        id,
        FavoriteFoodRequest(
            foodName = foodName,
            category = category,
            note = note,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            fatPer100g = fatPer100g,
            carbPer100g = carbPer100g,
        )
    )

    suspend fun delete(id: Long) = api.deleteFavorite(id)
}
