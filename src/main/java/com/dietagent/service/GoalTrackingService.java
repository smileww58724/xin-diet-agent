package com.dietagent.service;

import com.dietagent.entity.GoalSetting;
import com.dietagent.exception.BusinessException;
import com.dietagent.repository.GoalSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalTrackingService {

    private final GoalSettingRepository goalSettingRepository;
    private final UserService userService;

    public GoalSetting setDailyGoal(Long userId, GoalSetting goalSetting) {
        GoalSetting existing = goalSettingRepository
                .findByUserIdAndGoalDate(userId, goalSetting.getGoalDate())
                .orElse(null);

        GoalSetting saved;
        if (existing != null) {
            existing.setTargetCalories(goalSetting.getTargetCalories());
            existing.setTargetProtein(goalSetting.getTargetProtein());
            existing.setTargetFat(goalSetting.getTargetFat());
            existing.setTargetCarb(goalSetting.getTargetCarb());
            existing.setAchieved(false);
            saved = goalSettingRepository.save(existing);
        } else {
            goalSetting.setUserId(userId);
            goalSetting.setAchieved(false);
            saved = goalSettingRepository.save(goalSetting);
        }

        // 同步到用户画像，保证 AI 提示词与营养分析读到的目标口径一致
        userService.syncDailyGoals(userId, saved.getTargetCalories(),
                saved.getTargetProtein(), saved.getTargetFat(), saved.getTargetCarb());
        return saved;
    }

    public GoalSetting getDailyGoal(Long userId, LocalDate date) {
        return goalSettingRepository.findByUserIdAndGoalDate(userId, date)
                .orElse(null);
    }

    public List<GoalSetting> getGoalHistory(Long userId, LocalDate startDate, LocalDate endDate) {
        return goalSettingRepository.findByUserIdAndGoalDateBetween(userId, startDate, endDate);
    }

    public void markGoalAchieved(Long userId, LocalDate date) {
        GoalSetting goal = goalSettingRepository.findByUserIdAndGoalDate(userId, date)
                .orElseThrow(() -> new BusinessException("目标不存在"));

        goal.setAchieved(true);
        goalSettingRepository.save(goal);
    }
}
