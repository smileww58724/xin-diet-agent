package com.dietagent.repository;

import com.dietagent.entity.GoalSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoalSettingRepository extends JpaRepository<GoalSetting, Long> {

    Optional<GoalSetting> findByUserIdAndGoalDate(Long userId, LocalDate goalDate);

    List<GoalSetting> findByUserIdAndGoalDateBetween(Long userId, LocalDate start, LocalDate end);

    List<GoalSetting> findByUserIdOrderByGoalDateDesc(Long userId);
}
