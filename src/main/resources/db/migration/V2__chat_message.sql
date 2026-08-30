-- V2: 对话记忆持久化
-- 记忆从进程内存迁到数据库：重启不丢失、多实例天然共享；
-- 进程内保留 Caffeine 读穿透缓存，连续多轮对话不再重复查库

CREATE TABLE `chat_message` (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    role        VARCHAR(10) NOT NULL COMMENT 'user / assistant',
    content     TEXT        NOT NULL COMMENT '消息内容（AI 回复可能超 varchar 长度）',
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    -- 覆盖读取路径：按用户取最近 N 条（id 递增即写入顺序）
    KEY `idx_chat_message_user` (`user_id`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '对话记忆（持久化）';
