-- =============================================
-- 语音播报相关表
-- 生成时间: 2026-06-17
-- =============================================

-- 1. 语音播报记录表
DROP TABLE IF EXISTS `voice_broadcast_record`;
CREATE TABLE `voice_broadcast_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `broadcast_type` int DEFAULT NULL COMMENT '播报类型：1-逆变器离线 2-火灾预警 3-高级告警 4-紧急告警 5-设备异常',
  `alarm_level` int DEFAULT NULL COMMENT '告警级别：1-低级 2-中级 3-高级 4-紧急',
  `station_id` bigint DEFAULT NULL COMMENT '电站ID',
  `station_name` varchar(255) DEFAULT NULL COMMENT '电站名称',
  `inverter_id` bigint DEFAULT NULL COMMENT '逆变器ID',
  `inverter_name` varchar(255) DEFAULT NULL COMMENT '逆变器名称',
  `fault_code` varchar(100) DEFAULT NULL COMMENT '故障码',
  `description` varchar(500) DEFAULT NULL COMMENT '告警描述',
  `broadcast_content` text COMMENT '播报正文（TTS语音文本）',
  `audio_url` varchar(500) DEFAULT NULL COMMENT 'TTS合成语音文件URL',
  `target_speaker_ids` varchar(500) DEFAULT NULL COMMENT '推送的音箱设备ID列表，逗号分隔',
  `success_speaker_count` int DEFAULT 0 COMMENT '推送成功的音箱数量',
  `fail_speaker_count` int DEFAULT 0 COMMENT '推送失败的音箱数量',
  `push_result` text COMMENT '推送结果详情（JSON格式）',
  `status` int DEFAULT 0 COMMENT '播报状态：0-待播报 1-已播报 2-播报失败',
  `broadcast_time` datetime DEFAULT NULL COMMENT '实际播报时间',
  `work_order_id` bigint DEFAULT NULL COMMENT '关联工单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_station_id` (`station_id`),
  KEY `idx_inverter_id` (`inverter_id`),
  KEY `idx_status` (`status`),
  KEY `idx_alarm_level` (`alarm_level`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_broadcast_time` (`broadcast_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音播报记录';

-- 2. 告警消息消费日志表
DROP TABLE IF EXISTS `alarm_message_log`;
CREATE TABLE `alarm_message_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `topic` varchar(100) DEFAULT NULL COMMENT 'MQ主题',
  `consumer_group` varchar(100) DEFAULT NULL COMMENT '消费组',
  `message_key` varchar(100) DEFAULT NULL COMMENT '消息Key',
  `message_content` text COMMENT '消息内容JSON',
  `consume_status` int DEFAULT 0 COMMENT '消费状态：0-待消费 1-消费成功 2-消费失败 3-已重试',
  `retry_count` int DEFAULT 0 COMMENT '重试次数',
  `error_msg` text COMMENT '失败原因',
  `consume_time` datetime DEFAULT NULL COMMENT '消费完成时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_topic` (`topic`),
  KEY `idx_consume_status` (`consume_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警消息消费日志';

-- 3. 语音音箱设备表
DROP TABLE IF EXISTS `voice_speaker_device`;
CREATE TABLE `voice_speaker_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` varchar(100) NOT NULL COMMENT '音箱设备ID（MAC或SN）',
  `device_name` varchar(255) DEFAULT NULL COMMENT '设备名称',
  `location` varchar(255) DEFAULT NULL COMMENT '位置/所属运维中心',
  `device_type` varchar(50) DEFAULT 'speaker' COMMENT '设备类型',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `online` tinyint(1) DEFAULT 0 COMMENT '在线状态：0离线 1在线',
  `volume` int DEFAULT 80 COMMENT '音量设置 0-100',
  `last_heartbeat_time` datetime DEFAULT NULL COMMENT '最后心跳时间',
  `description` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_online` (`online`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音音箱设备';

-- 4. 初始化测试数据：插入几个示例音箱设备
INSERT INTO `voice_speaker_device` (`device_id`, `device_name`, `location`, `device_type`, `volume`, `description`) VALUES
('SPEAKER-001', '运维中心1号音箱', '一楼运维监控中心', 'speaker', 80, '主监控大屏语音播报'),
('SPEAKER-002', '运维中心2号音箱', '二楼运维监控中心', 'speaker', 80, '备用播报终端'),
('SPEAKER-003', 'A栋厂房音箱', 'A栋生产厂房', 'speaker', 100, '车间现场告警播报');
