package com.dietagent.service;

import com.dietagent.dto.response.NutritionSummaryResponse;
import com.dietagent.entity.User;
import com.dietagent.repository.DietRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class NutritionAnalysisService {

    private final DietRecordRepository dietRecordRepository;
    private final UserService userService;

    public NutritionSummaryResponse getDailySummary(Long userId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        return getSummary(userId, start, end);
    }

    public NutritionSummaryResponse getWeeklySummary(Long userId, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return getSummary(userId, start, end);
    }

    private NutritionSummaryResponse getSummary(Long userId, LocalDateTime start, LocalDateTime end) {
        Integer totalCalories = dietRecordRepository.sumCaloriesByUserIdAndMealTimeBetween(userId, start, end);
        Double totalProtein = dietRecordRepository.sumProteinByUserIdAndMealTimeBetween(userId, start, end);
        Double totalFat = dietRecordRepository.sumFatByUserIdAndMealTimeBetween(userId, start, end);
        Double totalCarb = dietRecordRepository.sumCarbohydrateByUserIdAndMealTimeBetween(userId, start, end);

        User user = userService.getUserById(userId);

        return NutritionSummaryResponse.builder()
                .totalCalories(totalCalories != null ? totalCalories : 0)
                .totalProtein(totalProtein != null ? totalProtein : 0.0)
                .totalFat(totalFat != null ? totalFat : 0.0)
                .totalCarbohydrate(totalCarb != null ? totalCarb : 0.0)
                .calorieGoal(user.getDailyCalorieGoal() != null ? user.getDailyCalorieGoal() : 2000)
                .proteinGoal(user.getProteinGoal() != null ? user.getProteinGoal() : 60)
                .fatGoal(user.getFatGoal() != null ? user.getFatGoal() : 65)
                .carbGoal(user.getCarbGoal() != null ? user.getCarbGoal() : 300)
                .calorieProgress(calculateProgress(
                        totalCalories != null ? totalCalories : 0,
                        user.getDailyCalorieGoal() != null ? user.getDailyCalorieGoal() : 2000))
                .proteinProgress(calculateProgress(
                        totalProtein != null ? totalProtein : 0.0,
                        user.getProteinGoal() != null ? user.getProteinGoal() : 60))
                .fatProgress(calculateProgress(
                        totalFat != null ? totalFat : 0.0,
                        user.getFatGoal() != null ? user.getFatGoal() : 65))
                .carbProgress(calculateProgress(
                        totalCarb != null ? totalCarb : 0.0,
                        user.getCarbGoal() != null ? user.getCarbGoal() : 300))
                .build();
    }

    private Double calculateProgress(double actual, int goal) {
        if (goal <= 0) return 0.0;
        return Math.min(actual / goal * 100, 100.0);
    }
}
