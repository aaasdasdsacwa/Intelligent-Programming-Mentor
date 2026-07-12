-- 1. 创建数据库并设置字符集
CREATE DATABASE IF NOT EXISTS `smart_tutor` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `smart_tutor`;

-- 2. 创建用户表 (user)
CREATE TABLE IF NOT EXISTS `user` (
                                      `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
                                      `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名/账号',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'student' COMMENT '角色：student-学生 / admin-管理员',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0-未删, 1-已删)',
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 3. 创建题库表 (problem)
CREATE TABLE IF NOT EXISTS `problem` (
                                         `id` BIGINT AUTO_INCREMENT COMMENT '题目ID',
                                         `title` VARCHAR(100) NOT NULL COMMENT '题目标题',
    `description` TEXT NOT NULL COMMENT '题目描述 (支持Markdown格式)',
    `difficulty` VARCHAR(20) NOT NULL DEFAULT 'medium' COMMENT '难度：easy / medium / hard',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '关联标签，逗号分隔，如 "java,oop"',
    `input_case` TEXT DEFAULT NULL COMMENT '评测输入样例',
    `output_case` TEXT DEFAULT NULL COMMENT '评测期望输出样例',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库表';

-- 4. 创建学习路径主表 (learning_path)
CREATE TABLE IF NOT EXISTS `learning_path` (
                                               `id` BIGINT AUTO_INCREMENT COMMENT '路径ID',
                                               `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
                                               `name` VARCHAR(100) NOT NULL COMMENT '路径名称，如 "Java后端开发路线"',
    `language` VARCHAR(50) NOT NULL COMMENT '目标语言',
    `target` VARCHAR(100) NOT NULL COMMENT '攻克方向',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户学习路径主表';

-- 5. 创建路径节点明细表 (path_node)
CREATE TABLE IF NOT EXISTS `path_node` (
                                           `id` BIGINT AUTO_INCREMENT COMMENT '节点ID',
                                           `path_id` BIGINT NOT NULL COMMENT '关联路径ID',
                                           `node_name` VARCHAR(100) NOT NULL COMMENT '节点阶段名称',
    `node_desc` TEXT DEFAULT NULL COMMENT '阶段描述',
    `matched_tag` VARCHAR(50) DEFAULT NULL COMMENT '匹配的题库标签，如 "java-basics"',
    `sequence` INT NOT NULL COMMENT '节点顺序权重',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '节点完成状态 (0-未开始, 1-学习中, 2-已完成)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_path_id` (`path_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='路径节点明细表';
-- 确保表存在的前提下，清空旧数据并插入3道经典测试题目
TRUNCATE TABLE `problem`;

INSERT INTO `problem` (`id`, `title`, `description`, `input_case`, `output_case`, `difficulty`, `tags`)
VALUES
    (1, 'A + B 经典计算', '输入两个整数 A 和 B，计算它们的和并输出。\n\n【输入格式】\n一行中输入两个用空格隔开的整数 A 和 B。\n\n【输出格式】\n输出一个整数，表示 A + B 的结果。\n\n【输入样例】\n3 5\n\n【输出样例】\n8', '3 5', '8', 'easy', 'java,math'),

    (2, '回文字符串检测', '编写一个 Java 程序，判断输入的字符串是否为回文字符串（即正着读和反着读都完全一样的字符串，例如 "aba", "racecar"）。\n如果为回文，输出 "true"，否则输出 "false"。\n\n【输入格式】\n输入一行包含一个字符串。\n\n【输出格式】\n输出 "true" 或 "false"。\n\n【输入样例】\nlevel\n\n【输出样例】\ntrue', 'level', 'true', 'easy', 'java,string'),

    (3, '斐波那契数列第 N 项', '求出斐波那契数列第 N 项的值。\n斐波那契数列定义如下：\nF(0) = 0, F(1) = 1\nF(N) = F(N-1) + F(N-2) (N >= 2)\n\n【输入格式】\n输入一个非负整数 N（0 <= N <= 30）。\n\n【输出格式】\n输出第 N 项的斐波那契数。\n\n【输入样例】\n6\n\n【输出样例】\n8', '6', '8', 'medium', 'java,algorithm');
ALTER TABLE `path_node` ADD COLUMN `detail` TEXT NULL COMMENT '知识点简单介绍缓存';