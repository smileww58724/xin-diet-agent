package com.dietagent.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NutritionCalculator {

    private static final double CALORIES_PER_GRAM_PROTEIN = 4.0;
    private static final double CALORIES_PER_GRAM_FAT = 9.0;
    private static final double CALORIES_PER_GRAM_CARBOHYDRATE = 4.0;

    public static int calculateCalories(int protein, int fat, int carbohydrate) {
        return (int) (protein * CALORIES_PER_GRAM_PROTEIN
                + fat * CALORIES_PER_GRAM_FAT
                + carbohydrate * CALORIES_PER_GRAM_CARBOHYDRATE);
    }

    public static int calculateCalories(BigDecimal protein, BigDecimal fat, BigDecimal carbohydrate) {
        return calculateCalories(
                protein != null ? protein.intValue() : 0,
                fat != null ? fat.intValue() : 0,
                carbohydrate != null ? carbohydrate.intValue() : 0
        );
    }

    public static int calculateDailyCaloriesGoal(double weight, int age, String gender, String activityLevel, String goalType) {
        double bmr;
        if ("male".equalsIgnoreCase(gender)) {
            bmr = 10 * weight + 6.25 * 170 - 5 * age + 5;
        } else {
            bmr = 10 * weight + 6.25 * 160 - 5 * age - 161;
        }

        double activityMultiplier = switch (activityLevel.toLowerCase()) {
            case "sedentary" -> 1.2;
            case "light" -> 1.375;
            case "moderate" -> 1.55;
            case "active" -> 1.725;
            case "very_active" -> 1.9;
            default -> 1.2;
        };

        double tdee = bmr * activityMultiplier;

        return switch (goalType.toLowerCase()) {
            case "lose_weight" -> (int) (tdee - 500);
            case "gain_weight" -> (int) (tdee + 500);
            case "maintain" -> (int) tdee;
            default -> (int) tdee;
        };
    }

    public static int calculateProteinGoal(double weight, String goalType) {
        double multiplier = switch (goalType.toLowerCase()) {
            case "lose_weight" -> 2.0;
            case "gain_weight" -> 2.2;
            case "maintain" -> 1.6;
            default -> 1.6;
        };
        return (int) (weight * multiplier);
    }

    public static int calculateFatGoal(int calorieGoal) {
        return (int) (calorieGoal * 0.25 / CALORIES_PER_GRAM_FAT);
    }

    public static int calculateCarbGoal(int calorieGoal, int proteinGoal, int fatGoal) {
        int proteinCalories = proteinGoal * 4;
        int fatCalories = fatGoal * 9;
        int carbCalories = calorieGoal - proteinCalories - fatCalories;
        return Math.max(0, carbCalories / 4);
    }
}
