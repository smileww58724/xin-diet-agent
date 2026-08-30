package com.dietagent.service;

import com.dietagent.dto.response.NutritionSummaryResponse;
import com.dietagent.entity.User;
import com.dietagent.repository.DietRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NutritionAnalysisServiceTest {

    @Mock
    private DietRecordRepository dietRecordRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private NutritionAnalysisService service;

    private void stubUser(Integer calorieGoal, Integer proteinGoal, Integer fatGoal, Integer carbGoal) {
        User user = User.builder()
                .dailyCalorieGoal(calorieGoal)
                .proteinGoal(proteinGoal)
                .fatGoal(fatGoal)
                .carbGoal(carbGoal)
                .build();
        when(userService.getUserById(1L)).thenReturn(user);
    }

    @Test
    @DisplayName("无记录时汇总为 0，目标用默认值兜底")
    void emptyRecordsDefaultGoals() {
        when(dietRecordRepository.aggregateNutrition(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{null, null, null, null}));
        stubUser(null, null, null, null);

        NutritionSummaryResponse summary = service.getDailySummary(1L, LocalDate.of(2026, 8, 30));

        assertEquals(0, summary.getTotalCalories());
        assertEquals(0.0, summary.getTotalProtein());
        // 默认目标：2000 kcal / 60g 蛋白 / 65g 脂肪 / 300g 碳水
        assertEquals(2000, summary.getCalorieGoal());
        assertEquals(60, summary.getProteinGoal());
        assertEquals(65, summary.getFatGoal());
        assertEquals(300, summary.getCarbGoal());
        assertEquals(0.0, summary.getCalorieProgress());
    }

    @Test
    @DisplayName("超额摄入时进度封顶 100%")
    void progressCappedAt100() {
        // SUM(Integer) 在 JPQL 中返回 Long，BigDecimal 列返回 BigDecimal
        when(dietRecordRepository.aggregateNutrition(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{5000L, new BigDecimal("300.00"), new BigDecimal("200.00"), new BigDecimal("700.00")}));
        stubUser(2000, 60, 65, 300);

        NutritionSummaryResponse summary = service.getDailySummary(1L, LocalDate.of(2026, 8, 30));

        assertEquals(5000, summary.getTotalCalories());
        assertEquals(100.0, summary.getCalorieProgress());
        assertEquals(100.0, summary.getProteinProgress());
        assertEquals(100.0, summary.getFatProgress());
        assertEquals(100.0, summary.getCarbProgress());
    }

    @Test
    @DisplayName("聚合值经 Number 正确换算（Long→int、BigDecimal→double）")
    void numberConversion() {
        when(dietRecordRepository.aggregateNutrition(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{1500L, new BigDecimal("55.50"), new BigDecimal("32.50"), new BigDecimal("180.00")}));
        stubUser(2000, 60, 65, 300);

        NutritionSummaryResponse summary = service.getDailySummary(1L, LocalDate.of(2026, 8, 30));

        assertEquals(1500, summary.getTotalCalories());
        assertEquals(55.5, summary.getTotalProtein());
        assertEquals(32.5, summary.getTotalFat());
        assertEquals(180.0, summary.getTotalCarbohydrate());
        // 1500/2000 = 75%
        assertEquals(75.0, summary.getCalorieProgress());
    }
}
