package com.dietagent.service;

import com.dietagent.config.CacheConfig;
import com.dietagent.dto.response.NutritionSummaryResponse;
import com.dietagent.entity.User;
import com.dietagent.repository.DietRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NutritionAnalysisService {

    private final DietRecordRepository dietRecordRepository;
    private final UserService userService;

    @Cacheable(cacheNames = CacheConfig.NUTRITION_SUMMARY, key = "#userId + ':daily:' + #date")
    public NutritionSummaryResponse getDailySummary(Long userId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        return getSummary(userId, start, end);
    }

    @Cacheable(cacheNames = CacheConfig.NUTRITION_SUMMARY, key = "#userId + ':weekly:' + #startDate")
    public NutritionSummaryResponse getWeeklySummary(Long userId, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return getSummary(userId, start, end);
    }

    private NutritionSummaryResponse getSummary(Long userId, LocalDateTime start, LocalDateTime end) {
        // 单条聚合查询替代此前 4 条 SUM：每次 AI 对话与日报页面都会触发，减少 3/4 的查询开销
        List<Object[]> rows = dietRecordRepository.aggregateNutrition(userId, start, end);
        Object[] row = rows != null && !rows.isEmpty() ? rows.get(0) : null;
        Integer totalCalories = toInt(row != null && row.length > 0 ? row[0] : null);
        Double totalProtein = toDouble(row != null && row.length > 1 ? row[1] : null);
        Double totalFat = toDouble(row != null && row.length > 2 ? row[2] : null);
        Double totalCarb = toDouble(row != null && row.length > 3 ? row[3] : null);

        User user = userService.getUserById(userId);
        int calorieGoal = user.getDailyCalorieGoal() != null ? user.getDailyCalorieGoal() : 2000;
        int proteinGoal = user.getProteinGoal() != null ? user.getProteinGoal() : 60;
        int fatGoal = user.getFatGoal() != null ? user.getFatGoal() : 65;
        int carbGoal = user.getCarbGoal() != null ? user.getCarbGoal() : 300;

        return NutritionSummaryResponse.builder()
                .totalCalories(totalCalories)
                .totalProtein(totalProtein)
                .totalFat(totalFat)
                .totalCarbohydrate(totalCarb)
                .calorieGoal(calorieGoal)
                .proteinGoal(proteinGoal)
                .fatGoal(fatGoal)
                .carbGoal(carbGoal)
                .calorieProgress(calculateProgress(totalCalories, calorieGoal))
                .proteinProgress(calculateProgress(totalProtein, proteinGoal))
                .fatProgress(calculateProgress(totalFat, fatGoal))
                .carbProgress(calculateProgress(totalCarb, carbGoal))
                .build();
    }

    /** 目标空值用合理默认值兜底；进度封顶 100%，避免超 100% 的展示问题 */
    private Double calculateProgress(double actual, int goal) {
        if (goal <= 0) return 0.0;
        return Math.min(actual / goal * 100, 100.0);
    }

    /** JPQL SUM 对不同数值列返回 Long/BigDecimal 等不同包装类型，统一经 Number 换算 */
    private static Integer toInt(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private static Double toDouble(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0.0;
    }
}
