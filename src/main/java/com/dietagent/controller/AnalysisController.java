package com.dietagent.controller;

import com.dietagent.dto.response.NutritionSummaryResponse;
import com.dietagent.service.NutritionAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final NutritionAnalysisService nutritionAnalysisService;

    @GetMapping("/daily")
    public ResponseEntity<NutritionSummaryResponse> getDailySummary(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return ResponseEntity.ok(nutritionAnalysisService.getDailySummary(userId, date));
    }

    @GetMapping("/weekly")
    public ResponseEntity<NutritionSummaryResponse> getWeeklySummary(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(6);
        }
        return ResponseEntity.ok(nutritionAnalysisService.getWeeklySummary(userId, startDate));
    }
}
