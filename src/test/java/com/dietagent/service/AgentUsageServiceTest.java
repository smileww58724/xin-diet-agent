package com.dietagent.service;

import com.dietagent.dto.response.UsageStatsResponse;
import com.dietagent.entity.AgentUsage;
import com.dietagent.repository.AgentUsageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentUsageServiceTest {

    @Mock
    private AgentUsageRepository repository;

    @Test
    @DisplayName("totalTokens 为空或 0 时不落库（中间分片无 usage）")
    void recordSkipsZeroUsage() {
        new AgentUsageService(repository).record(1L, "m", 10, 5, null, 100);
        new AgentUsageService(repository).record(1L, "m", 10, 5, 0, 100);

        verify(repository, org.mockito.Mockito.never()).save(any(AgentUsage.class));
    }

    @Test
    @DisplayName("正常记录：字段完整映射，null 归零")
    void recordMapsFields() {
        new AgentUsageService(repository).record(1L, "deepseek-chat", null, null, 123, 4567);

        ArgumentCaptor<AgentUsage> captor = ArgumentCaptor.forClass(AgentUsage.class);
        verify(repository).save(captor.capture());
        AgentUsage saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("deepseek-chat", saved.getModel());
        assertEquals(0, saved.getPromptTokens());
        assertEquals(0, saved.getCompletionTokens());
        assertEquals(123, saved.getTotalTokens());
        assertEquals(4567L, saved.getDurationMs());
    }

    @Test
    @DisplayName("落库异常只记日志，不影响对话主链路")
    void recordSwallowsDbError() {
        when(repository.save(any(AgentUsage.class))).thenThrow(new RuntimeException("db down"));

        new AgentUsageService(repository).record(1L, "m", 1, 1, 2, 10);
        // 未抛异常即通过
    }

    @Test
    @DisplayName("统计：按模型聚合换算为合计，按天趋势正确映射")
    void statsAggregatesRows() {
        when(repository.sumByModelForUser(1L)).thenReturn(java.util.Collections.singletonList(
                new Object[]{"deepseek-chat", new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("150"), new BigInteger("2")}));
        when(repository.dailyStats(eq(1L), any()))
                .thenReturn(java.util.Collections.singletonList(new Object[]{java.sql.Date.valueOf(LocalDate.now()), new BigDecimal("150"), new BigInteger("2")}));

        UsageStatsResponse stats = new AgentUsageService(repository).stats(1L);

        assertEquals(2L, stats.getTotalCalls());
        assertEquals(100L, stats.getTotalPromptTokens());
        assertEquals(50L, stats.getTotalCompletionTokens());
        assertEquals(150L, stats.getTotalTokens());
        assertEquals("deepseek-chat", stats.getByModel().get(0).getModel());
        assertEquals(1, stats.getDaily().size());
        assertTrue(stats.getDaily().get(0).getDate().contains(String.valueOf(LocalDate.now().getYear())));
    }
}
