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