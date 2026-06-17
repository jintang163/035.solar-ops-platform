-- =============================================
-- 告警消息消费日志建表SQL
-- =============================================

CREATE TABLE IF NOT EXISTS `alarm_message_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `topic` VARCHAR(100) DEFAULT NULL COMMENT '消息主题',
    `message_id` VARCHAR(100) DEFAULT NULL COMMENT '消息ID',
    `content` TEXT COMMENT '消息内容',
    `consume_status` INT DEFAULT 0 COMMENT '消费状态：0-消费中 1-消费成功 2-消费失败',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `error_msg` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_topic` (`topic`),
    KEY `idx_message_id` (`message_id`),
    KEY `idx_consume_status` (`consume_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警消息消费日志表';
