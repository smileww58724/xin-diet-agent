package com.dietagent.repository;

import com.dietagent.entity.DietRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DietRecordRepository extends JpaRepository<DietRecord, Long> {

    List<DietRecord> findByUserIdAndMealTimeBetweenOrderByMealTimeAsc(
            Long userId, LocalDateTime start, LocalDateTime end);

    List<DietRecord> findByUserIdAndMealTimeBetween(
            Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(d.calories) FROM DietRecord d WHERE d.userId = :userId AND d.mealTime BETWEEN :start AND :end")
    Integer sumCaloriesByUserIdAndMealTimeBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT SUM(d.protein) FROM DietRecord d WHERE d.userId = :userId AND d.mealTime BETWEEN :start AND :end")
    Double sumProteinByUserIdAndMealTimeBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT SUM(d.fat) FROM DietRecord d WHERE d.userId = :userId AND d.mealTime BETWEEN :start AND :end")
    Double sumFatByUserIdAndMealTimeBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT SUM(d.carbohydrate) FROM DietRecord d WHERE d.userId = :userId AND d.mealTime BETWEEN :start AND :end")
    Double sumCarbohydrateByUserIdAndMealTimeBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
