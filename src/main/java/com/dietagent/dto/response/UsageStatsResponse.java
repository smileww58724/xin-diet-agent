package com.dietagent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageStatsResponse {

    private Long totalCalls;
    private Long totalPromptTokens;
    private Long totalCompletionTokens;
    private Long totalTokens;

    private List<ModelStat> byModel;
    private List<DailyStat> daily;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelStat {
        private String model;
        private Long calls;
        private Long promptTokens;
        private Long completionTokens;
        private Long totalTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStat {
        private String date;
        private Long calls;
        private Long tokens;
    }
}
