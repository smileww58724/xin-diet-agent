package com.dietagent.repository;

import com.dietagent.entity.AgentUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgentUsageRepository extends JpaRepository<AgentUsage, Long> {

    /** 按模型聚合：调用量与三类 token 合计 */
    @Query("""
            SELECT a.model, SUM(a.promptTokens), SUM(a.completionTokens), SUM(a.totalTokens), COUNT(a)
            FROM AgentUsage a
            WHERE a.userId = :userId
            GROUP BY a.model
            """)
    List<Object[]> sumByModelForUser(@Param("userId") Long userId);

    /** 按天聚合（近 N 天用量趋势） */
    @Query(value = """
            SELECT DATE(created_at), SUM(total_tokens), COUNT(*)
            FROM agent_usage
            WHERE user_id = :userId AND created_at >= :since
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at)
            """, nativeQuery = true)
    List<Object[]> dailyStats(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
