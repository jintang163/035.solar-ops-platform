-- 故障库表
CREATE TABLE IF NOT EXISTS `fault_library` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `fault_code` VARCHAR(64) NOT NULL COMMENT '故障码',
    `fault_name` VARCHAR(128) NOT NULL COMMENT '故障名称',
    `fault_level` TINYINT NOT NULL DEFAULT 1 COMMENT '故障级别 1-低级 2-中级 3-高级 4-紧急',
    `fault_desc` TEXT COMMENT '故障描述',
    `solution` TEXT COMMENT '解决方案',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fault_code` (`fault_code`),
    KEY `idx_fault_level` (`fault_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故障库';

-- 工单表
CREATE TABLE IF NOT EXISTS `work_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '工单编号',
    `station_id` BIGINT NOT NULL COMMENT '电站ID',
    `inverter_id` BIGINT COMMENT '逆变器ID',
    `fault_code` VARCHAR(64) COMMENT '故障码',
    `fault_name` VARCHAR(128) COMMENT '故障名称',
    `fault_level` TINYINT COMMENT '故障级别 1-低级 2-中级 3-高级 4-紧急',
    `description` TEXT COMMENT '故障描述',
    `solution` TEXT COMMENT '解决方案',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '工单状态 0-待接单 1-已接单 2-处理中 3-待验收 4-已完成 5-已关闭',
    `handler_id` BIGINT COMMENT '处理人ID',
    `handler_name` VARCHAR(64) COMMENT '处理人姓名',
    `accept_time` DATETIME COMMENT '接单时间',
    `process_time` DATETIME COMMENT '处理时间',
    `complete_time` DATETIME COMMENT '完成时间',
    `expect_time` DATETIME COMMENT '预计完成时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_station_id` (`station_id`),
    KEY `idx_inverter_id` (`inverter_id`),
    KEY `idx_status` (`status`),
    KEY `idx_handler_id` (`handler_id`),
    KEY `idx_fault_code` (`fault_code`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_expect_time` (`expect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- 工单日志表
CREATE TABLE IF NOT EXISTS `work_order_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '工单ID',
    `operator_id` BIGINT COMMENT '操作人ID',
    `operator_name` VARCHAR(64) COMMENT '操作人姓名',
    `action` VARCHAR(64) NOT NULL COMMENT '操作动作',
    `remark` VARCHAR(512) COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单日志表';

-- 初始化故障库数据
INSERT INTO `fault_library` (`fault_code`, `fault_name`, `fault_level`, `fault_desc`, `solution`) VALUES
('INV_NO_COMM', '逆变器通讯中断', 3, '逆变器与监控系统失去通讯连接', '1. 检查逆变器电源是否正常 2. 检查通讯线连接 3. 重启逆变器通讯模块 4. 检查交换机和网线'),
('INV_OVER_TEMP', '逆变器过温', 3, '逆变器内部温度超过告警阈值', '1. 检查机房通风和散热系统 2. 清洁逆变器散热片 3. 检查风扇是否正常运转 4. 降低负载运行'),
('INV_OVER_VOLT', '逆变器过压', 4, '直流侧或交流侧电压超过安全阈值', '1. 立即停机检查 2. 检查组件串电压是否正常 3. 检查电网电压是否稳定 4. 联系厂家技术支持'),
('INV_SHORT_CIRCUIT', '逆变器短路故障', 4, '检测到直流或交流侧短路', '1. 立即停机 2. 断开直流侧开关 3. 检查组件和线缆绝缘 4. 排查短路点并修复'),
('INV_GRID_OFF', '电网失压', 2, '交流侧电网电压异常或中断', '1. 检查电网供电情况 2. 检查交流侧开关状态 3. 等待电网恢复后自动重启'),
('INV_LOW_EFF', '逆变器效率偏低', 2, '逆变器转换效率低于正常范围', '1. 检查逆变器运行参数 2. 清洁逆变器散热系统 3. 检查MPPT跟踪是否正常'),
('STRING_NO_OUTPUT', '组串无输出', 2, '光伏组串无电流输出', '1. 检查组串接线 2. 检查组件是否被遮挡 3. 检查组串保险丝 4. 检查组件是否损坏'),
('PANEL_HOT_SPOT', '组件热斑', 3, '光伏组件出现热斑效应', '1. 热成像检查组件 2. 更换损坏组件 3. 检查组件串并联配置'),
('JUNCTION_BOX_FAULT', '汇流箱故障', 2, '汇流箱通讯或内部故障', '1. 检查汇流箱电源 2. 检查通讯模块 3. 检查熔断器和防雷器'),
('MONITOR_DEVICE_OFFLINE', '监控设备离线', 1, '数据采集器或监控网关离线', '1. 检查设备电源 2. 检查网络连接 3. 重启设备');
