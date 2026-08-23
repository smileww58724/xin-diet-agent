package com.dietagent.agent.tool;

import com.dietagent.dto.response.NutritionSummaryResponse;
import com.dietagent.service.NutritionAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class NutritionSummaryTool implements Function<NutritionSummaryTool.Request, NutritionSummaryTool.Response> {

    private final NutritionAnalysisService nutritionAnalysisService;

    @Override
    public Response apply(Request request) {
        LocalDate date = request.date != null ? request.date : LocalDate.now();
        NutritionSummaryResponse summary = nutritionAnalysisService.getDailySummary(request.userId, date);

        String info = String.format(
                "今日营养摄入汇总：\n" +
                "- 总热量: %d / %d kcal (进度: %.1f%%)\n" +
                "- 蛋白质: %.1fg / %dg (进度: %.1f%%)\n" +
                "- 脂肪: %.1fg / %dg (进度: %.1f%%)\n" +
                "- 碳水: %.1fg / %dg (进度: %.1f%%)",
                summary.getTotalCalories(),
                summary.getCalorieGoal(),
                summary.getCalorieProgress(),
                summary.getTotalProtein(),
                summary.getProteinGoal(),
                summary.getProteinProgress(),
                summary.getTotalFat(),
                summary.getFatGoal(),
                summary.getFatProgress(),
                summary.getTotalCarbohydrate(),
                summary.getCarbGoal(),
                summary.getCarbProgress()
        );

        return new Response(info);
    }

    public record Request(Long userId, LocalDate date) {}
    public record Response(String nutritionSummary) {}
}
