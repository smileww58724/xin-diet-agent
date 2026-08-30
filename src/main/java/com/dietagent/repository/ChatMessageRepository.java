package com.dietagent.repository;

import com.dietagent.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** id 自增即写入顺序，取最近 N 条后由调用方反转为时间正序 */
    List<ChatMessage> findTop20ByUserIdOrderByIdDesc(Long userId);

    void deleteByUserId(Long userId);

    /**
     * 只保留每个用户最近 limit 条，防止历史无界增长。
     * 嵌套一层 SELECT 是为了让 MySQL 允许对同表子查询使用 LIMIT。
     */
    @Modifying
    @Query(value = "DELETE FROM chat_message WHERE user_id = :userId AND id NOT IN "
            + "(SELECT id FROM (SELECT id FROM chat_message WHERE user_id = :userId ORDER BY id DESC LIMIT :limit) t)",
            nativeQuery = true)
    int trimToRecent(@Param("userId") Long userId, @Param("limit") int limit);
}
