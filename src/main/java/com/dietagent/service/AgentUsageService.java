package com.dietagent.service;

import com.dietagent.dto.response.UsageStatsResponse;
import com.dietagent.entity.AgentUsage;
import com.dietagent.repository.AgentUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 调用用量的记录与统计。
 * 大模型调用按 token 计费：用量落库让成本治理从"打过日志"变成有数据的系统，
 * 支撑按用户的成本看板（/api/agent/usage）与后续配额/预算策略。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentUsageService {

    private static final int DAILY_RANGE_DAYS = 7;

    private final AgentUsageRepository repository;

    /** 记录一次调用；统计是旁路数据，任何异常只记日志、不影响对话主链路 */
    @Transactional
    public void record(Long userId, String model, Integer promptTokens, Integer completionTokens,
                       Integer totalTokens, long durationMs) {
        if (totalTokens == null || totalTokens <= 0) {
            return;
        }
        try {
            repository.save(AgentUsage.builder()
                    .userId(userId)
                    .model(model != null ? model : "unknown")
                    .promptTokens(promptTokens != null ? promptTokens : 0)
                    .completionTokens(completionTokens != null ? completionTokens : 0)
                    .totalTokens(totalTokens)
                    .durationMs(durationMs)
                    .build());
        } catch (Exception e) {
            log.warn("用量记录落库失败 userId={}: {}", userId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public UsageStatsResponse stats(Long userId) {
        List<Object[]> modelRows = repository.sumByModelForUser(userId);

        long totalCalls = 0;
        long totalPrompt = 0;
        long totalCompletion = 0;
        long totalTokens = 0;
        List<UsageStatsResponse.ModelStat> byModel = new ArrayList<>();
        for (Object[] row : modelRows) {
            long prompt = toLong(row[1]);
            long completion = toLong(row[2]);
            long tokens = toLong(row[3]);
            long calls = toLong(row[4]);
            byModel.add(UsageStatsResponse.ModelStat.builder()
                    .model(String.valueOf(row[0]))
                    .calls(calls)
                    .promptTokens(prompt)
                    .completionTokens(completion)
                    .totalTokens(tokens)
                    .build());
            totalPrompt += prompt;
            totalCompletion += completion;
            totalTokens += tokens;
            totalCalls += calls;
        }

        List<Object[]> dayRows = repository.dailyStats(userId, LocalDate.now().minusDays(DAILY_RANGE_DAYS - 1L).atStartOfDay());
        List<UsageStatsResponse.DailyStat> daily = new ArrayList<>();
        for (Object[] row : dayRows) {
            daily.add(UsageStatsResponse.DailyStat.builder()
                    .date(String.valueOf(row[0]))
                    .calls(toLong(row[2]))
                    .tokens(toLong(row[1]))
                    .build());
        }

        return UsageStatsResponse.builder()
                .totalCalls(totalCalls)
                .totalPromptTokens(totalPrompt)
                .totalCompletionTokens(totalCompletion)
                .totalTokens(totalTokens)
                .byModel(byModel)
                .daily(daily)
                .build();
    }

    /** 原生查询聚合列在 Long/BigDecimal/BigInteger 间浮动，统一经 Number 换算 */
    private static long toLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }
}
