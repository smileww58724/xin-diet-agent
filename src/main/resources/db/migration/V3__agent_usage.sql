-- V3: AI 调用用量记录
-- 大模型调用按 token 计费：落库每次调用的用量与耗时，
-- 支撑按用户的成本统计看板（/api/agent/usage）与后续配额治理

CREATE TABLE `agent_usage` (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    model             VARCHAR(64)  NOT NULL,
    prompt_tokens     INT          NOT NULL DEFAULT 0,
    completion_tokens INT          NOT NULL DEFAULT 0,
    total_tokens      INT          NOT NULL DEFAULT 0,
    duration_ms       BIGINT       NOT NULL DEFAULT 0 COMMENT '调用耗时（流式为到末尾分片的耗时）',
    created_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY `idx_agent_usage_user_time` (`user_id`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 调用用量';
