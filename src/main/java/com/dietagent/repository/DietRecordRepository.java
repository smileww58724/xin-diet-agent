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

    /**
     * 单条聚合查询一次取回四项营养汇总（热量/蛋白/脂肪/碳水）。
     * 此前拆成 4 条 SUM 查询，而每次 AI 对话与日报页面都要触发，合并后减少 3/4 查询开销。
     * JPQL 纯聚合查询无匹配行时也返回一行全 null，调用方需做空值兜底。
     */
    @Query("""
            SELECT SUM(d.calories), SUM(d.protein), SUM(d.fat), SUM(d.carbohydrate)
            FROM DietRecord d
            WHERE d.userId = :userId AND d.mealTime BETWEEN :start AND :end
            """)
    Object[] aggregateNutrition(@Param("userId") Long userId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);
}
