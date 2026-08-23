package com.dietagent.controller;

import com.dietagent.dto.request.DietRecordRequest;
import com.dietagent.dto.response.DietRecordResponse;
import com.dietagent.service.DietRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/diet")
@RequiredArgsConstructor
public class DietRecordController {

    private final DietRecordService dietRecordService;

    @PostMapping("/records")
    public ResponseEntity<DietRecordResponse> addRecord(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DietRecordRequest request) {
        if (userId == null) {
            throw new com.dietagent.exception.BusinessException("用户未登录");
        }
        return ResponseEntity.ok(dietRecordService.addRecord(userId, request));
    }

    @PutMapping("/records/{id}")
    public ResponseEntity<DietRecordResponse> updateRecord(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody DietRecordRequest request) {
        return ResponseEntity.ok(dietRecordService.updateRecord(userId, id, request));
    }

    @DeleteMapping("/records/{id}")
    public ResponseEntity<Void> deleteRecord(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        dietRecordService.deleteRecord(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/records")
    public ResponseEntity<List<DietRecordResponse>> getRecords(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dietRecordService.getRecords(userId, date));
    }

    @GetMapping("/records/range")
    public ResponseEntity<List<DietRecordResponse>> getRecords(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(dietRecordService.getRecords(userId, startDate, endDate));
    }
}
