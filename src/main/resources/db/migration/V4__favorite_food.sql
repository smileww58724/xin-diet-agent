-- V4: 用户偏好食物库
-- 用户标记自己爱吃的食物，AI 对话时注入 system prompt：
-- 推荐、食谱、分析及所有食物话题优先围绕偏好展开（用户级个性化 RAG）

CREATE TABLE `favorite_food` (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    user_id            BIGINT        NOT NULL,
    food_name          VARCHAR(100)  NOT NULL,
    category           VARCHAR(30)   NULL COMMENT '分类：高蛋白/粗粮/低脂等',
    note               VARCHAR(200)  NULL COMMENT '备注',
    calories_per_100g  INT           NULL COMMENT '每100g热量 kcal',
    protein_per_100g   DECIMAL(5, 2) NULL,
    fat_per_100g       DECIMAL(5, 2) NULL,
    carb_per_100g      DECIMAL(5, 2) NULL,
    created_at         DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY `uk_fav_user_food` (`user_id`, `food_name`),
    KEY `idx_fav_user` (`user_id`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户偏好食物';
