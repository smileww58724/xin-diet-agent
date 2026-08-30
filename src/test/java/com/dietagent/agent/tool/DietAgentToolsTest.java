package com.dietagent.agent.tool;

import com.dietagent.dto.request.DietRecordRequest;
import com.dietagent.dto.response.DietRecordResponse;
import com.dietagent.service.DietRecordService;
import com.dietagent.service.NutritionAnalysisService;
import com.dietagent.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DietAgentToolsTest {

    @Mock
    private DietRecordService dietRecordService;

    @Mock
    private NutritionAnalysisService nutritionAnalysisService;

    @Mock
    private UserService userService;

    private DietAgentTools tools() {
        return new DietAgentTools(1L, userService, dietRecordService, nutritionAnalysisService);
    }

    @Test
    @DisplayName("合法日期解析后传给服务层，且固定绑定当前用户")
    void validDatePassedToService() {
        tools().getDietRecords("2026-08-29");

        verify(dietRecordService).getRecords(eq(1L), eq(LocalDate.of(2026, 8, 29)));
    }

    @Test
    @DisplayName("日期为空默认查今天")
    void nullDateFallsBackToToday() {
        tools().getDietRecords(null);

        verify(dietRecordService).getRecords(eq(1L), argThat(LocalDate.now()::equals));
    }

    @Test
    @DisplayName("非法格式日期回退为今天，不抛异常打断模型调用")
    void invalidDateFallsBackToToday() {
        String result = tools().getDietRecords("明天");

        verify(dietRecordService).getRecords(eq(1L), argThat(LocalDate.now()::equals));
        assertTrue(result.contains("暂无饮食记录"));
    }

    @Test
    @DisplayName("无记录时返回明确的'暂无'提示供模型转述")
    void emptyRecordsMessage() {
        when(dietRecordService.getRecords(eq(1L), argThat(LocalDate.now()::equals)))
                .thenReturn(List.of());

        String result = tools().getDietRecords("");

        assertTrue(result.contains("暂无饮食记录"));
    }

    @Test
    @DisplayName("有记录时按餐次/食物/营养的固定格式输出")
    void recordFormatting() {
        DietRecordResponse record = DietRecordResponse.builder()
                .mealType("午餐")
                .foodName("鸡胸肉")
                .calories(300)
                .protein(new BigDecimal("40"))
                .fat(new BigDecimal("5"))
                .carbohydrate(new BigDecimal("2"))
                .build();
        when(dietRecordService.getRecords(eq(1L), argThat(LocalDate.of(2026, 8, 29)::equals)))
                .thenReturn(List.of(record));

        String result = tools().getDietRecords("2026-08-29");

        assertTrue(result.contains("[午餐] 鸡胸肉"));
        assertTrue(result.contains("300"));
    }

    @Test
    @DisplayName("记录饮食：口语餐次归一化，未指定时间按餐次取常规时间，绑定当前用户")
    void addDietRecordNormalizesAndSaves() {
        when(dietRecordService.addRecord(eq(1L), argThat(r -> true)))
                .thenReturn(DietRecordResponse.builder().id(9L).mealType("午餐").foodName("米饭").calories(230).build());

        String result = tools().addDietRecord("米饭", "午饭", "一碗(约200g)", 230, 5.0, 0.6, 51.0, null);

        ArgumentCaptor<DietRecordRequest> captor = ArgumentCaptor.forClass(DietRecordRequest.class);
        verify(dietRecordService).addRecord(eq(1L), captor.capture());
        DietRecordRequest saved = captor.getValue();
        assertEquals("米饭", saved.getFoodName());
        assertEquals("午餐", saved.getMealType());
        assertEquals(LocalDateTime.of(LocalDate.now(), java.time.LocalTime.of(12, 0)), saved.getMealTime());
        assertEquals(new BigDecimal("5.0"), saved.getProtein());
        assertTrue(result.contains("已记录"));
        assertTrue(result.contains("估算值"));
        assertTrue(result.contains("9"));
    }

    @Test
    @DisplayName("记录饮食：无法识别的餐次归为加餐，默认时间 15:00")
    void addDietRecordFallsBackToSnack() {
        when(dietRecordService.addRecord(eq(1L), argThat(r -> true)))
                .thenReturn(DietRecordResponse.builder().id(10L).mealType("加餐").foodName("苹果").calories(95).build());

        tools().addDietRecord("苹果", "下午茶", null, 95, null, null, null, "2026-08-29");

        ArgumentCaptor<DietRecordRequest> captor = ArgumentCaptor.forClass(DietRecordRequest.class);
        verify(dietRecordService).addRecord(eq(1L), captor.capture());
        assertEquals("加餐", captor.getValue().getMealType());
        assertEquals(LocalDateTime.of(LocalDate.of(2026, 8, 29), java.time.LocalTime.of(15, 0)),
                captor.getValue().getMealTime());
    }
}
