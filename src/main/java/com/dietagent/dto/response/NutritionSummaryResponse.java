package com.dietagent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionSummaryResponse {

    private Integer totalCalories;
    private Double totalProtein;
    private Double totalFat;
    private Double totalCarbohydrate;

    private Integer calorieGoal;
    private Integer proteinGoal;
    private Integer fatGoal;
    private Integer carbGoal;

    private Double calorieProgress;
    private Double proteinProgress;
    private Double fatProgress;
    private Double carbProgress;
}
