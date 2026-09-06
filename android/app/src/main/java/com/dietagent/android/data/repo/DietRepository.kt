package com.dietagent.android.data.repo

import com.dietagent.android.data.remote.ApiService
import com.dietagent.android.data.remote.dto.DietRecordRequest
import com.dietagent.android.data.remote.dto.DietRecordResponse

class DietRepository(private val api: ApiService) {

    suspend fun getRecords(date: String): List<DietRecordResponse> = api.getRecords(date)

    suspend fun addRecord(
        foodName: String,
        mealType: String,
        portionSize: String?,
        calories: Int?,
        protein: Double?,
        fat: Double?,
        carbohydrate: Double?,
        mealTime: String,
    ): DietRecordResponse = api.addRecord(
        DietRecordRequest(
            foodName = foodName,
            mealType = mealType,
            portionSize = portionSize,
            calories = calories,
            protein = protein,
            fat = fat,
            carbohydrate = carbohydrate,
            mealTime = mealTime,
        )
    )

    suspend fun updateRecord(
        id: Long,
        foodName: String,
        mealType: String,
        portionSize: String?,
        calories: Int?,
        protein: Double?,
        fat: Double?,
        carbohydrate: Double?,
        mealTime: String,
    ): DietRecordResponse = api.updateRecord(
        id,
        DietRecordRequest(
            foodName = foodName,
            mealType = mealType,
            portionSize = portionSize,
            calories = calories,
            protein = protein,
            fat = fat,
            carbohydrate = carbohydrate,
            mealTime = mealTime,
        )
    )

    suspend fun deleteRecord(id: Long) = api.deleteRecord(id)
}
