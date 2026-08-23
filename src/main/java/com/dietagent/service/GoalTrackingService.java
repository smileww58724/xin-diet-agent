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

    public GoalSetting setDailyGoal(Long userId, GoalSetting goalSetting) {
        GoalSetting existing = goalSettingRepository
                .findByUserIdAndGoalDate(userId, goalSetting.getGoalDate())
                .orElse(null);

        if (existing != null) {
            existing.setTargetCalories(goalSetting.getTargetCalories());
            existing.setTargetProtein(goalSetting.getTargetProtein());
            existing.setTargetFat(goalSetting.getTargetFat());
            existing.setTargetCarb(goalSetting.getTargetCarb());
            existing.setAchieved(false);
            return goalSettingRepository.save(existing);
        }

        goalSetting.setUserId(userId);
        goalSetting.setAchieved(false);
        return goalSettingRepository.save(goalSetting);
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
