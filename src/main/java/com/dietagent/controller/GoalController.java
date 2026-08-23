package com.dietagent.controller;

import com.dietagent.entity.GoalSetting;
import com.dietagent.entity.User;
import com.dietagent.service.GoalTrackingService;
import com.dietagent.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalTrackingService goalTrackingService;
    private final UserService userService;

    @PostMapping("/daily")
    public ResponseEntity<GoalSetting> setDailyGoal(
            @AuthenticationPrincipal Long userId,
            @RequestBody GoalSetting goalSetting) {
        return ResponseEntity.ok(goalTrackingService.setDailyGoal(userId, goalSetting));
    }

    @GetMapping("/daily")
    public ResponseEntity<GoalSetting> getDailyGoal(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return ResponseEntity.ok(goalTrackingService.getDailyGoal(userId, date));
    }

    @GetMapping("/history")
    public ResponseEntity<List<GoalSetting>> getGoalHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(goalTrackingService.getGoalHistory(userId, startDate, endDate));
    }

    @PostMapping("/achieve")
    public ResponseEntity<Void> markGoalAchieved(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        goalTrackingService.markGoalAchieved(userId, date);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody User updateData) {
        return ResponseEntity.ok(userService.updateUserProfile(userId, updateData));
    }
}
