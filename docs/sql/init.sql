-- =====================================================
-- 光伏电站远程运维平台 - 数据库初始化脚本
-- Database: solar_ops
-- 生成日期: 2024-01-01
-- =====================================================

CREATE DATABASE IF NOT EXISTS solar_ops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE solar_ops;

-- =====================================================
-- 1. 系统用户表
-- =====================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    role VARCHAR(50) DEFAULT 'user' COMMENT '角色 admin-管理员 user-普通用户 ops-运维人员',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- =====================================================
-- 2. 电站表
-- =====================================================
DROP TABLE IF EXISTS station;
CREATE TABLE station (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_name VARCHAR(100) NOT NULL COMMENT '电站名称',
    station_code VARCHAR(50) NOT NULL COMMENT '电站编号',
    capacity DECIMAL(10,2) DEFAULT NULL COMMENT '装机容量(kW)',
    address VARCHAR(255) DEFAULT NULL COMMENT '地址',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
    contact VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    contact_phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    peak_sun_hours DECIMAL(4,2) DEFAULT 4.00 COMMENT '峰值日照小时数',
    status TINYINT DEFAULT 1 COMMENT '状态 0停用 1启用',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_station_code (station_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电站表';

-- =====================================================
-- 3. 逆变器表
-- =====================================================
DROP TABLE IF EXISTS inverter;
CREATE TABLE inverter (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    device_sn VARCHAR(50) NOT NULL COMMENT '设备序列号',
    device_name VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
    device_model VARCHAR(50) DEFAULT NULL COMMENT '设备型号',
    rated_power DECIMAL(10,2) DEFAULT NULL COMMENT '额定功率(kW)',
    install_location VARCHAR(255) DEFAULT NULL COMMENT '安装位置',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
    communication_type VARCHAR(20) DEFAULT '4G' COMMENT '通讯方式 4G/RS485/WIFI',
    status TINYINT DEFAULT 1 COMMENT '状态 0停用 1启用',
    online_status TINYINT DEFAULT 0 COMMENT '在线状态 0离线 1在线',
    last_online_time DATETIME DEFAULT NULL COMMENT '最后在线时间',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_sn (device_sn),
    KEY idx_station_id (station_id),
    KEY idx_online_status (online_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逆变器表';

-- =====================================================
-- 4. 故障库表
-- =====================================================
DROP TABLE IF EXISTS fault_library;
CREATE TABLE fault_library (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    fault_code VARCHAR(64) NOT NULL COMMENT '故障码',
    fault_name VARCHAR(128) NOT NULL COMMENT '故障名称',
    fault_level TINYINT NOT NULL DEFAULT 1 COMMENT '故障级别 1-低级 2-中级 3-高级 4-紧急',
    fault_type VARCHAR(50) DEFAULT NULL COMMENT '故障类型',
    fault_desc TEXT COMMENT '故障描述',
    solution TEXT COMMENT '解决方案',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fault_code (fault_code),
    KEY idx_fault_level (fault_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故障库';

-- =====================================================
-- 5. 工单表
-- =====================================================
DROP TABLE IF EXISTS work_order;
CREATE TABLE work_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '工单编号',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    inverter_id BIGINT COMMENT '逆变器ID',
    fault_code VARCHAR(64) COMMENT '故障码',
    fault_name VARCHAR(128) COMMENT '故障名称',
    fault_level TINYINT COMMENT '故障级别 1-低级 2-中级 3-高级 4-紧急',
    description TEXT COMMENT '故障描述',
    solution TEXT COMMENT '解决方案',
    repair_photos TEXT COMMENT '维修照片(JSON数组)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '工单状态 0-待接单 1-已接单 2-处理中 3-待验收 4-已完成 5-已关闭',
    source TINYINT DEFAULT 1 COMMENT '工单来源 1-自动生成 2-手动创建',
    handler_id BIGINT COMMENT '处理人ID',
    handler_name VARCHAR(64) COMMENT '处理人姓名',
    accept_time DATETIME COMMENT '接单时间',
    process_time DATETIME COMMENT '处理时间',
    complete_time DATETIME COMMENT '完成时间',
    expect_time DATETIME COMMENT '预计完成时间',
    is_timeout TINYINT DEFAULT 0 COMMENT '是否超时 0-否 1-是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_station_id (station_id),
    KEY idx_inverter_id (inverter_id),
    KEY idx_status (status),
    KEY idx_handler_id (handler_id),
    KEY idx_fault_code (fault_code),
    KEY idx_create_time (create_time),
    KEY idx_expect_time (expect_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- =====================================================
-- 6. 工单日志表
-- =====================================================
DROP TABLE IF EXISTS work_order_log;
CREATE TABLE work_order_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '工单ID',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(64) COMMENT '操作人姓名',
    action VARCHAR(64) NOT NULL COMMENT '操作动作',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_operator_id (operator_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单日志表';

-- =====================================================
-- 7. 效率统计表
-- =====================================================
DROP TABLE IF EXISTS efficiency_statistics;
CREATE TABLE efficiency_statistics (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    inverter_id BIGINT COMMENT '逆变器ID',
    statistics_date DATE NOT NULL COMMENT '统计日期',
    statistics_type TINYINT NOT NULL COMMENT '统计类型 1-日 2-周 3-月 4-年',
    pr_value DECIMAL(5,4) DEFAULT NULL COMMENT 'PR值(性能比)',
    system_efficiency DECIMAL(5,4) DEFAULT NULL COMMENT '系统效率',
    equivalent_hours DECIMAL(6,2) DEFAULT NULL COMMENT '等效利用小时数',
    total_energy DECIMAL(12,2) DEFAULT NULL COMMENT '总发电量(kWh)',
    irradiance DECIMAL(10,2) DEFAULT NULL COMMENT '辐照量(kWh/m²)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_station_date_type (station_id, statistics_date, statistics_type),
    KEY idx_inverter_id (inverter_id),
    KEY idx_statistics_date (statistics_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='效率统计表';

-- =====================================================
-- 8. 电站健康度表
-- =====================================================
DROP TABLE IF EXISTS station_health;
CREATE TABLE station_health (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    health_level TINYINT NOT NULL COMMENT '健康等级 1-优秀 2-良好 3-较差',
    pr_value DECIMAL(5,4) DEFAULT NULL COMMENT 'PR值',
    fault_count INT DEFAULT 0 COMMENT '故障数量',
    efficiency_score DECIMAL(5,2) DEFAULT NULL COMMENT '效率评分',
    assessment_time DATETIME NOT NULL COMMENT '评估时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_station_id (station_id),
    KEY idx_assessment_time (assessment_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电站健康度表';

-- =====================================================
-- 9. 告警记录表
-- =====================================================
DROP TABLE IF EXISTS alarm_record;
CREATE TABLE alarm_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    inverter_id BIGINT COMMENT '逆变器ID',
    alarm_type VARCHAR(50) NOT NULL COMMENT '告警类型',
    alarm_level TINYINT NOT NULL DEFAULT 1 COMMENT '告警级别 1-低 2-中 3-高 4-紧急',
    alarm_content VARCHAR(512) DEFAULT NULL COMMENT '告警内容',
    alarm_value VARCHAR(100) DEFAULT NULL COMMENT '告警值',
    threshold VARCHAR(100) DEFAULT NULL COMMENT '阈值',
    status TINYINT DEFAULT 0 COMMENT '状态 0-未处理 1-已处理 2-已忽略',
    handle_time DATETIME DEFAULT NULL COMMENT '处理时间',
    handle_remark VARCHAR(512) DEFAULT NULL COMMENT '处理备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_station_id (station_id),
    KEY idx_inverter_id (inverter_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 初始化用户 (密码: 123456, BCrypt加密)
INSERT INTO sys_user (username, password, nickname, phone, email, status, role, create_time, update_time) VALUES
('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', '13800138000', 'admin@solar.com', 1, 'admin', NOW(), NOW()),
('ops001', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '运维工程师-王工', '13900139001', 'wang@solar.com', 1, 'ops', NOW(), NOW()),
('ops002', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '运维工程师-李工', '13900139002', 'li@solar.com', 1, 'ops', NOW(), NOW());

-- 初始化电站数据
INSERT INTO station (station_name, station_code, capacity, address, longitude, latitude, contact, contact_phone, peak_sun_hours, status, create_time, update_time) VALUES
('阳光一号光伏电站', 'ST-2024-001', 5000.00, '北京市海淀区中关村大街1号', 116.310000, 39.980000, '张工', '13900139001', 4.20, 1, NOW(), NOW()),
('绿色能源二号电站', 'ST-2024-002', 3000.00, '上海市浦东新区张江高科技园区', 121.550000, 31.200000, '李工', '13900139002', 3.80, 1, NOW(), NOW()),
('西部太阳能发电站', 'ST-2024-003', 8000.00, '甘肃省酒泉市肃州区', 98.520000, 39.730000, '赵工', '13900139003', 5.50, 1, NOW(), NOW());

-- 初始化逆变器数据
INSERT INTO inverter (station_id, device_sn, device_name, device_model, rated_power, install_location, longitude, latitude, communication_type, status, online_status, last_online_time, create_time, update_time) VALUES
(1, 'INV-001-2024', '1号逆变器', 'SUN-100KTL', 100.00, 'A区1号机房', 116.310100, 39.980100, '4G', 1, 1, NOW(), NOW(), NOW()),
(1, 'INV-002-2024', '2号逆变器', 'SUN-100KTL', 100.00, 'A区2号机房', 116.310200, 39.980200, '4G', 1, 1, NOW(), NOW(), NOW()),
(1, 'INV-003-2024', '3号逆变器', 'SUN-100KTL', 100.00, 'B区1号机房', 116.310300, 39.980300, 'RS485', 1, 0, '2024-01-15 10:30:00', NOW(), NOW()),
(1, 'INV-004-2024', '4号逆变器', 'SUN-50KTL', 50.00, 'B区2号机房', 116.310400, 39.980400, '4G', 1, 1, NOW(), NOW(), NOW()),
(1, 'INV-005-2024', '5号逆变器', 'SUN-50KTL', 50.00, 'C区1号机房', 116.310500, 39.980500, '4G', 1, 1, NOW(), NOW(), NOW()),
(2, 'INV-006-2024', '6号逆变器', 'SUN-100KTL', 100.00, '1号厂房屋顶', 121.550100, 31.200100, '4G', 1, 1, NOW(), NOW(), NOW()),
(2, 'INV-007-2024', '7号逆变器', 'SUN-100KTL', 100.00, '2号厂房屋顶', 121.550200, 31.200200, 'WIFI', 1, 1, NOW(), NOW(), NOW()),
(2, 'INV-008-2024', '8号逆变器', 'SUN-50KTL', 50.00, '3号厂房屋顶', 121.550300, 31.200300, '4G', 1, 0, '2024-01-20 14:00:00', NOW(), NOW()),
(3, 'INV-009-2024', '9号逆变器', 'SUN-200KTL', 200.00, 'A区阵列', 98.520100, 39.730100, '4G', 1, 1, NOW(), NOW(), NOW()),
(3, 'INV-010-2024', '10号逆变器', 'SUN-200KTL', 200.00, 'B区阵列', 98.520200, 39.730200, 'RS485', 1, 1, NOW(), NOW(), NOW());

-- 初始化故障库数据
INSERT INTO fault_library (fault_code, fault_name, fault_level, fault_type, fault_desc, solution) VALUES
('INV_NO_COMM', '逆变器通讯中断', 3, '通讯故障', '逆变器与监控系统失去通讯连接', '1. 检查逆变器电源是否正常 2. 检查通讯线连接 3. 重启逆变器通讯模块 4. 检查交换机和网线'),
('INV_OVER_TEMP', '逆变器过温', 3, '温度故障', '逆变器内部温度超过告警阈值', '1. 检查机房通风和散热系统 2. 清洁逆变器散热片 3. 检查风扇是否正常运转 4. 降低负载运行'),
('INV_OVER_VOLT', '逆变器过压', 4, '电气故障', '直流侧或交流侧电压超过安全阈值', '1. 立即停机检查 2. 检查组件串电压是否正常 3. 检查电网电压是否稳定 4. 联系厂家技术支持'),
('INV_SHORT_CIRCUIT', '逆变器短路故障', 4, '电气故障', '检测到直流或交流侧短路', '1. 立即停机 2. 断开直流侧开关 3. 检查组件和线缆绝缘 4. 排查短路点并修复'),
('INV_GRID_OFF', '电网失压', 2, '电网故障', '交流侧电网电压异常或中断', '1. 检查电网供电情况 2. 检查交流侧开关状态 3. 等待电网恢复后自动重启'),
('INV_LOW_EFF', '逆变器效率偏低', 2, '效率异常', '逆变器转换效率低于正常范围', '1. 检查逆变器运行参数 2. 清洁逆变器散热系统 3. 检查MPPT跟踪是否正常'),
('STRING_NO_OUTPUT', '组串无输出', 2, '组件故障', '光伏组串无电流输出', '1. 检查组串接线 2. 检查组件是否被遮挡 3. 检查组串保险丝 4. 检查组件是否损坏'),
('PANEL_HOT_SPOT', '组件热斑', 3, '组件故障', '光伏组件出现热斑效应', '1. 热成像检查组件 2. 更换损坏组件 3. 检查组件串并联配置'),
('JUNCTION_BOX_FAULT', '汇流箱故障', 2, '电气故障', '汇流箱通讯或内部故障', '1. 检查汇流箱电源 2. 检查通讯模块 3. 检查熔断器和防雷器'),
('MONITOR_DEVICE_OFFLINE', '监控设备离线', 1, '通讯故障', '数据采集器或监控网关离线', '1. 检查设备电源 2. 检查网络连接 3. 重启设备'),
('DC_INSULATION_FAULT', '直流绝缘故障', 3, '电气故障', '直流侧对地绝缘电阻低于阈值', '1. 停机检查直流线缆绝缘 2. 检查组件接地情况 3. 逐段排查接地点'),
('GRID_FREQUENCY_ERROR', '电网频率异常', 2, '电网故障', '电网频率超出允许范围', '1. 检查电网频率 2. 等待电网恢复正常 3. 检查逆变器频率保护参数'),
('MPPT_LOW_EFFICIENCY', 'MPPT效率低', 2, '效率异常', 'MPPT跟踪效率偏低', '1. 检查MPPT工作状态 2. 检查组串配置 3. 重启逆变器'),
('FAN_FAILURE', '风扇故障', 2, '设备故障', '散热风扇运转异常或停转', '1. 检查风扇供电 2. 清洁风扇叶片 3. 更换损坏风扇');

-- 初始化示例工单数据
INSERT INTO work_order (order_no, station_id, inverter_id, fault_code, fault_name, fault_level, description, solution, status, source, handler_id, handler_name, accept_time, process_time, complete_time, expect_time, create_time) VALUES
('WO20240101001', 1, 3, 'INV_NO_COMM', '逆变器通讯中断', 3, '3号逆变器通讯中断，已离线超过24小时', NULL, 0, 1, NULL, NULL, NULL, NULL, NULL, DATE_ADD(NOW(), INTERVAL 24 HOUR), DATE_SUB(NOW(), INTERVAL 2 DAY)),
('WO20240101002', 2, 8, 'INV_LOW_EFF', '逆变器效率偏低', 2, '8号逆变器PR值持续低于0.7，疑似效率异常', NULL, 1, 1, 2, '运维工程师-李工', DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, NULL, DATE_ADD(NOW(), INTERVAL 24 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR)),
('WO20240101003', 1, 1, 'INV_OVER_TEMP', '逆变器过温', 3, '1号逆变器温度过高，达到65度', '已清理散热片，更换故障已排除', 4, 1, 2, '运维工程师-李工', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 36 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));
