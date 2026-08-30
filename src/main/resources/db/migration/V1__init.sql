-- V1: 初始化三张核心业务表
-- 字段与 JPA 实体严格对应（Spring Boot 默认驼峰转下划线命名策略）

-- 用户表
CREATE TABLE `user` (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    username           VARCHAR(50)  NOT NULL COMMENT '登录名',
    password           VARCHAR(255) NOT NULL COMMENT 'BCrypt 密文',
    nickname           VARCHAR(50)  NULL,
    height             DECIMAL(5, 2) NULL COMMENT '身高 cm',
    weight             DECIMAL(5, 2) NULL COMMENT '体重 kg',
    age                INT          NULL,
    gender             VARCHAR(10)  NULL,
    activity_level     VARCHAR(20)  NULL,
    goal_type          VARCHAR(20)  NULL,
    daily_calorie_goal INT          NULL COMMENT '每日热量目标 kcal',
    protein_goal       INT          NULL COMMENT '每日蛋白质目标 g',
    fat_goal           INT          NULL COMMENT '每日脂肪目标 g',
    carb_goal          INT          NULL COMMENT '每日碳水目标 g',
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户与画像';

-- 饮食记录表
CREATE TABLE `diet_record` (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    food_name     VARCHAR(100)  NOT NULL,
    meal_type     VARCHAR(20)   NOT NULL COMMENT '餐次：早餐/午餐/晚餐/加餐',
    portion_size  VARCHAR(50)   NULL,
    calories      INT           NULL COMMENT '热量 kcal',
    protein       DECIMAL(5, 2) NULL COMMENT '蛋白质 g',
    fat           DECIMAL(5, 2) NULL COMMENT '脂肪 g',
    carbohydrate  DECIMAL(5, 2) NULL COMMENT '碳水 g',
    meal_time     DATETIME(6)   NOT NULL COMMENT '进食时间',
    created_at    DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    -- 覆盖核心查询路径：按用户+时间段查记录、按用户+时间段聚合营养
    KEY `idx_diet_record_user_time` (`user_id`, `meal_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '饮食记录';

-- 每日目标表
CREATE TABLE `goal_setting` (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    goal_date       DATE        NOT NULL,
    target_calories INT         NULL,
    target_protein  INT         NULL,
    target_fat      INT         NULL,
    target_carb     INT         NULL,
    achieved        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY `uk_goal_user_date` (`user_id`, `goal_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '每日营养目标';
