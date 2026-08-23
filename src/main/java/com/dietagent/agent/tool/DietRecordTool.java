package com.dietagent.agent.tool;

import com.dietagent.dto.response.DietRecordResponse;
import com.dietagent.service.DietRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class DietRecordTool implements Function<DietRecordTool.Request, DietRecordTool.Response> {

    private final DietRecordService dietRecordService;

    @Override
    public Response apply(Request request) {
        LocalDate date = request.date != null ? request.date : LocalDate.now();
        List<DietRecordResponse> records = dietRecordService.getRecords(request.userId, date);

        StringBuilder sb = new StringBuilder();
        if (records.isEmpty()) {
            sb.append("暂无饮食记录");
        } else {
            for (DietRecordResponse r : records) {
                sb.append(String.format("[%s] %s - %s kcal (蛋白质: %sg, 脂肪: %sg, 碳水: %sg)\n",
                        r.getMealType(),
                        r.getFoodName(),
                        r.getCalories(),
                        r.getProtein(),
                        r.getFat(),
                        r.getCarbohydrate()));
            }
        }

        return new Response(sb.toString());
    }

    public record Request(Long userId, LocalDate date) {}
    public record Response(String dietRecords) {}
}
