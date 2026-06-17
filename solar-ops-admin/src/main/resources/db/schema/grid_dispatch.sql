-- =============================================
-- 电网调度相关表
-- 生成时间: 2026-06-17
-- =============================================

-- 1. 电网调度指令表
DROP TABLE IF EXISTS `grid_dispatch_command`;
CREATE TABLE `grid_dispatch_command` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `command_no` varchar(100) DEFAULT NULL COMMENT '调度指令编号',
  `command_source` int DEFAULT NULL COMMENT '调度来源：1-调度主站下发 2-人工干预 3-自动调节',
  `command_type` int DEFAULT NULL COMMENT '指令类型：1-有功功率调节 2-无功功率调节 3-电压调节 4-频率调节 5-启停逆变器',
  `station_id` bigint DEFAULT NULL COMMENT '电站ID',
  `station_name` varchar(255) DEFAULT NULL COMMENT '电站名称',
  `inverter_id` bigint DEFAULT NULL COMMENT '逆变器ID，为空则全站调节',
  `inverter_name` varchar(255) DEFAULT NULL COMMENT '逆变器名称',
  `target_active_power` decimal(18,4) DEFAULT NULL COMMENT '目标有功功率(kW)',
  `target_reactive_power` decimal(18,4) DEFAULT NULL COMMENT '目标无功功率(kVar)',
  `target_voltage` decimal(18,4) DEFAULT NULL COMMENT '目标电压(V)',
  `target_frequency` decimal(18,4) DEFAULT NULL COMMENT '目标频率(Hz)',
  `adjust_ratio` int DEFAULT NULL COMMENT '调节比例(0-100%)',
  `start_stop` tinyint(1) DEFAULT NULL COMMENT '是否启停：true启动 false停机',
  `issue_time` datetime DEFAULT NULL COMMENT '指令下发时间',
  `expect_time` datetime DEFAULT NULL COMMENT '期望执行时间',
  `execute_start_time` datetime DEFAULT NULL COMMENT '执行开始时间',
  `execute_end_time` datetime DEFAULT NULL COMMENT '执行完成时间',
  `status` int DEFAULT 0 COMMENT '指令状态：0-待执行 1-执行中 2-执行成功 3-执行失败 4-已取消 5-超时',
  `execute_result` varchar(500) DEFAULT NULL COMMENT '执行结果描述',
  `actual_active_power` decimal(18,4) DEFAULT NULL COMMENT '实际有功功率(kW)',
  `actual_reactive_power` decimal(18,4) DEFAULT NULL COMMENT '实际无功功率(kVar)',
  `actual_voltage` decimal(18,4) DEFAULT NULL COMMENT '实际电压(V)',
  `actual_frequency` decimal(18,4) DEFAULT NULL COMMENT '实际频率(Hz)',
  `deviation_percent` decimal(10,4) DEFAULT NULL COMMENT '偏差百分比(%)',
  `priority` int DEFAULT 3 COMMENT '优先级：1-紧急 2-高 3-普通 4-低',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID（人工干预时）',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人姓名',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `protocol_command_id` varchar(100) DEFAULT NULL COMMENT '协议指令ID',
  `asdu_address` int DEFAULT NULL COMMENT 'ASDU地址',
  `raw_message` text COMMENT '原始报文',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_command_no` (`command_no`),
  KEY `idx_station_id` (`station_id`),
  KEY `idx_status` (`status`),
  KEY `idx_command_type` (`command_type`),
  KEY `idx_priority` (`priority`),
  KEY `idx_issue_time` (`issue_time`),
  KEY `idx_execute_start_time` (`execute_start_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_inverter_id` (`inverter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电网调度指令';

-- 2. 调度数据上传记录表
DROP TABLE IF EXISTS `grid_dispatch_upload`;
CREATE TABLE `grid_dispatch_upload` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `protocol_type` int DEFAULT NULL COMMENT '协议类型：1-IEC104 2-Modbus TCP',
  `data_type` int DEFAULT NULL COMMENT '数据类型：1-实时功率 2-电压 3-频率 4-发电量 5-设备状态',
  `station_id` bigint DEFAULT NULL COMMENT '电站ID',
  `station_name` varchar(255) DEFAULT NULL COMMENT '电站名称',
  `inverter_id` bigint DEFAULT NULL COMMENT '逆变器ID',
  `inverter_name` varchar(255) DEFAULT NULL COMMENT '逆变器名称',
  `total_active_power` decimal(18,4) DEFAULT NULL COMMENT '总有功功率(kW)',
  `total_reactive_power` decimal(18,4) DEFAULT NULL COMMENT '总无功功率(kVar)',
  `voltage_a` decimal(18,4) DEFAULT NULL COMMENT 'A相电压(V)',
  `voltage_b` decimal(18,4) DEFAULT NULL COMMENT 'B相电压(V)',
  `voltage_c` decimal(18,4) DEFAULT NULL COMMENT 'C相电压(V)',
  `frequency` decimal(18,4) DEFAULT NULL COMMENT '频率(Hz)',
  `power_factor` decimal(10,4) DEFAULT NULL COMMENT '功率因数',
  `daily_generation` decimal(18,4) DEFAULT NULL COMMENT '日发电量(kWh)',
  `total_generation` decimal(18,4) DEFAULT NULL COMMENT '总发电量(kWh)',
  `device_status` int DEFAULT NULL COMMENT '设备状态：1-运行 2-停机 3-故障 4-离线',
  `upload_time` datetime DEFAULT NULL COMMENT '上传时间',
  `upload_status` int DEFAULT 0 COMMENT '上传状态：0-待上传 1-上传成功 2-上传失败',
  `response_code` varchar(100) DEFAULT NULL COMMENT '调度主站响应码',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `retry_count` int DEFAULT 0 COMMENT '重试次数',
  `raw_message` text COMMENT '原始报文（HEX）',
  `cached` int DEFAULT 0 COMMENT '是否已缓存到Redis：0否 1是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_protocol_type` (`protocol_type`),
  KEY `idx_data_type` (`data_type`),
  KEY `idx_station_id` (`station_id`),
  KEY `idx_upload_status` (`upload_status`),
  KEY `idx_upload_time` (`upload_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_cached` (`cached`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调度数据上传记录';

-- 3. 调度协议配置表
DROP TABLE IF EXISTS `grid_dispatch_protocol_config`;
CREATE TABLE `grid_dispatch_protocol_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `protocol_type` int DEFAULT NULL COMMENT '协议类型：1-IEC104 2-Modbus TCP',
  `config_name` varchar(255) DEFAULT NULL COMMENT '配置名称',
  `master_ip` varchar(100) DEFAULT NULL COMMENT '调度主站IP',
  `master_port` int DEFAULT NULL COMMENT '调度主站端口',
  `local_ip` varchar(100) DEFAULT NULL COMMENT '本机IP（被动模式时）',
  `local_port` int DEFAULT NULL COMMENT '本机端口',
  `common_address` int DEFAULT NULL COMMENT '公共地址/站地址（IEC104的ASDU地址/Modbus的Slave ID）',
  `connect_timeout` int DEFAULT 10 COMMENT '连接超时时间(秒)',
  `send_timeout` int DEFAULT 10 COMMENT '发送超时时间(秒)',
  `heartbeat_interval` int DEFAULT 30 COMMENT '心跳间隔(秒)',
  `reverse_isolation_enabled` int DEFAULT 0 COMMENT '是否启用反向隔离：0否 1是',
  `isolation_ip` varchar(100) DEFAULT NULL COMMENT '反向隔离装置IP',
  `isolation_port` int DEFAULT NULL COMMENT '反向隔离装置端口',
  `upload_interval` int DEFAULT 5 COMMENT '上传间隔(秒)',
  `enabled` int DEFAULT 0 COMMENT '启用状态：0停用 1启用',
  `connection_status` int DEFAULT 0 COMMENT '连接状态：0未连接 1已连接 2异常',
  `last_connect_time` datetime DEFAULT NULL COMMENT '最后连接时间',
  `last_disconnect_time` datetime DEFAULT NULL COMMENT '最后断开时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_protocol_type` (`protocol_type`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_connection_status` (`connection_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调度协议配置';

-- 4. 初始化协议配置数据
INSERT INTO `grid_dispatch_protocol_config` (
  `protocol_type`, `config_name`, `master_ip`, `master_port`, `local_port`,
  `common_address`, `connect_timeout`, `send_timeout`, `heartbeat_interval`,
  `reverse_isolation_enabled`, `upload_interval`, `enabled`, `connection_status`, `remark`
) VALUES
(1, 'IEC104默认配置', '127.0.0.1', 2404, 2405, 1, 10, 10, 30, 0, 5, 1, 0, 'IEC104协议默认配置，主动连接调度主站'),
(2, 'Modbus TCP默认配置', '127.0.0.1', 502, 503, 1, 10, 10, 30, 0, 5, 0, 0, 'Modbus TCP协议默认配置，默认停用');
