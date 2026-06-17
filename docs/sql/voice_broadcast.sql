-- =============================================
-- 语音播报相关建表SQL
-- =============================================

-- 语音播报记录表
CREATE TABLE IF NOT EXISTS `voice_broadcast_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `broadcast_type` INT DEFAULT NULL COMMENT '播报类型：1-逆变器离线 2-火灾预警 3-高级告警 4-紧急告警 5-设备异常',
    `alarm_level` INT DEFAULT NULL COMMENT '告警级别：1-低级 2-中级 3-高级 4-紧急',
    `station_id` BIGINT DEFAULT NULL COMMENT '电站ID',
    `station_name` VARCHAR(100) DEFAULT NULL COMMENT '电站名称',
    `inverter_id` BIGINT DEFAULT NULL COMMENT '逆变器ID',
    `inverter_name` VARCHAR(100) DEFAULT NULL COMMENT '逆变器名称',
    `fault_code` VARCHAR(50) DEFAULT NULL COMMENT '故障码',
    `broadcast_content` TEXT COMMENT '播报内容文本',
    `audio_url` VARCHAR(500) DEFAULT NULL COMMENT '语音文件URL',
    `status` INT DEFAULT 0 COMMENT '播报状态：0-待播报 1-已播报 2-播报失败',
    `broadcast_time` DATETIME DEFAULT NULL COMMENT '播报时间',
    `work_order_id` BIGINT DEFAULT NULL COMMENT '关联工单ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_station_id` (`station_id`),
    KEY `idx_inverter_id` (`inverter_id`),
    KEY `idx_broadcast_type` (`broadcast_type`),
    KEY `idx_alarm_level` (`alarm_level`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音播报记录表';

-- 语音播报配置表
CREATE TABLE IF NOT EXISTS `voice_broadcast_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key` VARCHAR(50) NOT NULL COMMENT '配置键',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `min_alarm_level` INT DEFAULT 3 COMMENT '最小告警级别',
    `enabled_types` VARCHAR(100) DEFAULT '1,2,3,4,5' COMMENT '启用播报类型，逗号分隔',
    `volume` INT DEFAULT 80 COMMENT '音量 0-100',
    `speed` INT DEFAULT 50 COMMENT '语速 0-100',
    `voice_name` VARCHAR(50) DEFAULT 'xiaoyan' COMMENT '发音人',
    `broadcast_start_time` VARCHAR(10) DEFAULT '08:00' COMMENT '播报开始时间',
    `broadcast_end_time` VARCHAR(10) DEFAULT '20:00' COMMENT '播报结束时间',
    `night_broadcast` TINYINT(1) DEFAULT 0 COMMENT '夜间是否播报',
    `tts_provider` VARCHAR(50) DEFAULT 'xunfei' COMMENT 'TTS服务商',
    `speaker_api_url` VARCHAR(500) DEFAULT NULL COMMENT '音箱API地址',
    `speaker_api_key` VARCHAR(200) DEFAULT NULL COMMENT '音箱API密钥',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音播报配置表';

-- 初始配置数据
INSERT INTO `voice_broadcast_config` (`config_key`, `enabled`, `min_alarm_level`, `enabled_types`, `volume`, `speed`, `voice_name`, `broadcast_start_time`, `broadcast_end_time`, `night_broadcast`, `tts_provider`)
VALUES ('default', 1, 3, '1,2,3,4,5', 80, 50, 'xiaoyan', '08:00', '20:00', 0, 'xunfei');
