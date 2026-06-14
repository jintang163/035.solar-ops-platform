CREATE DATABASE IF NOT EXISTS solar_ops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE solar_ops;

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    role VARCHAR(50) DEFAULT NULL COMMENT '角色',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

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
    status TINYINT DEFAULT 1 COMMENT '状态 0停用 1启用',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_station_code (station_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电站表';

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
    status TINYINT DEFAULT 1 COMMENT '状态 0停用 1启用',
    online_status TINYINT DEFAULT 0 COMMENT '在线状态 0离线 1在线',
    last_online_time DATETIME DEFAULT NULL COMMENT '最后在线时间',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_sn (device_sn),
    KEY idx_station_id (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逆变器表';

INSERT INTO sys_user (username, password, nickname, phone, email, status, role, create_time, update_time) VALUES
('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', '13800138000', 'admin@solar.com', 1, 'admin', NOW(), NOW());

INSERT INTO station (station_name, station_code, capacity, address, longitude, latitude, contact, contact_phone, status, create_time, update_time) VALUES
('阳光一号光伏电站', 'ST-2024-001', 5000.00, '北京市海淀区中关村大街1号', 116.310000, 39.980000, '张工', '13900139001', 1, NOW(), NOW()),
('绿色能源二号电站', 'ST-2024-002', 3000.00, '上海市浦东新区张江高科技园区', 121.550000, 31.200000, '李工', '13900139002', 1, NOW(), NOW());

INSERT INTO inverter (station_id, device_sn, device_name, device_model, rated_power, install_location, longitude, latitude, status, online_status, last_online_time, create_time, update_time) VALUES
(1, 'INV-001-2024', '1号逆变器', 'SUN-100KTL', 100.00, 'A区1号机房', 116.310100, 39.980100, 1, 1, NOW(), NOW(), NOW()),
(1, 'INV-002-2024', '2号逆变器', 'SUN-100KTL', 100.00, 'A区2号机房', 116.310200, 39.980200, 1, 0, '2024-01-15 10:30:00', NOW(), NOW()),
(2, 'INV-003-2024', '3号逆变器', 'SUN-50KTL', 50.00, 'B区1号机房', 121.550100, 31.200100, 1, 1, NOW(), NOW(), NOW());

-- =====================================================
-- 14. 气象记录表
-- =====================================================
DROP TABLE IF EXISTS weather_record;
CREATE TABLE weather_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    record_time DATETIME NOT NULL COMMENT '记录时间',
    temperature DECIMAL(5,2) DEFAULT NULL COMMENT '温度(℃)',
    humidity DECIMAL(5,2) DEFAULT NULL COMMENT '湿度(%)',
    irradiance DECIMAL(8,2) DEFAULT NULL COMMENT '辐照度(W/m²)',
    cloud_cover DECIMAL(5,2) DEFAULT NULL COMMENT '云量(%)',
    weather VARCHAR(50) DEFAULT NULL COMMENT '天气状况',
    wind_direction VARCHAR(20) DEFAULT NULL COMMENT '风向',
    wind_speed DECIMAL(5,2) DEFAULT NULL COMMENT '风速(m/s)',
    pressure DECIMAL(8,2) DEFAULT NULL COMMENT '气压(hPa)',
    source VARCHAR(20) DEFAULT NULL COMMENT '数据来源 amap/qweather',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_station_id (station_id),
    KEY idx_record_time (record_time),
    KEY idx_station_time (station_id, record_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='气象记录表';

-- =====================================================
-- 15. 功率预测表
-- =====================================================
DROP TABLE IF EXISTS power_prediction;
CREATE TABLE power_prediction (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    inverter_id BIGINT DEFAULT NULL COMMENT '逆变器ID',
    predict_time DATETIME NOT NULL COMMENT '预测时间',
    target_time DATETIME NOT NULL COMMENT '目标预测时刻',
    horizon INT NOT NULL COMMENT '预测时域(小时) 1-6',
    predicted_power DECIMAL(12,4) DEFAULT NULL COMMENT '预测功率(kW)',
    actual_power DECIMAL(12,4) DEFAULT NULL COMMENT '实际功率(kW)',
    deviation DECIMAL(12,4) DEFAULT NULL COMMENT '功率偏差(kW)',
    deviation_rate DECIMAL(6,4) DEFAULT NULL COMMENT '偏差率(%)',
    temperature DECIMAL(5,2) DEFAULT NULL COMMENT '温度(℃)',
    humidity DECIMAL(5,2) DEFAULT NULL COMMENT '湿度(%)',
    irradiance DECIMAL(8,2) DEFAULT NULL COMMENT '辐照度(W/m²)',
    cloud_cover DECIMAL(5,2) DEFAULT NULL COMMENT '云量(%)',
    model_version VARCHAR(50) DEFAULT NULL COMMENT '模型版本',
    status TINYINT DEFAULT 0 COMMENT '状态 0-预测中 1-已验证 2-已过期',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_station_id (station_id),
    KEY idx_inverter_id (inverter_id),
    KEY idx_target_time (target_time),
    KEY idx_station_target (station_id, target_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功率预测表';

-- =====================================================
-- 16. 预测告警表
-- =====================================================
DROP TABLE IF EXISTS prediction_alert;
CREATE TABLE prediction_alert (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    inverter_id BIGINT DEFAULT NULL COMMENT '逆变器ID',
    prediction_id BIGINT DEFAULT NULL COMMENT '关联预测记录ID',
    alert_time DATETIME NOT NULL COMMENT '告警时间',
    target_time DATETIME DEFAULT NULL COMMENT '目标时间',
    alert_type TINYINT NOT NULL COMMENT '告警类型 1-预测偏差超标 2-气象异常 3-疑似设备故障',
    alert_level TINYINT NOT NULL DEFAULT 2 COMMENT '告警级别 1-低 2-中 3-高 4-紧急',
    alert_content VARCHAR(512) DEFAULT NULL COMMENT '告警内容',
    predicted_value DECIMAL(12,4) DEFAULT NULL COMMENT '预测值',
    actual_value DECIMAL(12,4) DEFAULT NULL COMMENT '实际值',
    deviation_rate DECIMAL(6,4) DEFAULT NULL COMMENT '偏差率(%)',
    threshold DECIMAL(6,4) DEFAULT NULL COMMENT '告警阈值',
    status TINYINT DEFAULT 0 COMMENT '状态 0-未处理 1-已处理 2-已忽略',
    handle_time DATETIME DEFAULT NULL COMMENT '处理时间',
    handle_remark VARCHAR(512) DEFAULT NULL COMMENT '处理备注',
    root_cause VARCHAR(100) DEFAULT NULL COMMENT '根因分析 weather-天气原因 equipment-设备故障 other-其他',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_station_id (station_id),
    KEY idx_inverter_id (inverter_id),
    KEY idx_alert_time (alert_time),
    KEY idx_status (status),
    KEY idx_alert_type (alert_type),
    KEY idx_alert_level (alert_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预测告警表';

-- =====================================================
-- 17. 无人机巡检任务表
-- =====================================================
DROP TABLE IF EXISTS drone_inspection_task;
CREATE TABLE drone_inspection_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_code VARCHAR(50) NOT NULL COMMENT '任务编号',
    task_name VARCHAR(100) DEFAULT NULL COMMENT '任务名称',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    area VARCHAR(100) DEFAULT NULL COMMENT '巡检区域',
    flight_mode VARCHAR(20) DEFAULT 'manual' COMMENT '飞行模式 manual-手动 auto-自动 waypoint-航点',
    drone_code VARCHAR(50) DEFAULT NULL COMMENT '无人机编号',
    pilot VARCHAR(50) DEFAULT NULL COMMENT '飞手',
    inspection_time DATETIME DEFAULT NULL COMMENT '巡检时间',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    status TINYINT DEFAULT 0 COMMENT '状态 0-待执行 1-执行中 2-已完成 3-已取消 4-异常',
    image_count INT DEFAULT 0 COMMENT '拍摄图片数量',
    detected_image_count INT DEFAULT 0 COMMENT '已检测图片数量',
    defect_count INT DEFAULT 0 COMMENT '检测缺陷数量',
    confirmed_defect_count INT DEFAULT 0 COMMENT '已确认缺陷数量',
    workorder_count INT DEFAULT 0 COMMENT '生成工单数量',
    description VARCHAR(500) DEFAULT NULL COMMENT '任务描述',
    operator_name VARCHAR(50) DEFAULT NULL COMMENT '巡检人员',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_code (task_code),
    KEY idx_station_id (station_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='无人机巡检任务表';

-- =====================================================
-- 18. 巡检图像表
-- =====================================================
DROP TABLE IF EXISTS drone_inspection_image;
CREATE TABLE drone_inspection_image (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '巡检任务ID',
    station_id BIGINT DEFAULT NULL COMMENT '电站ID',
    image_name VARCHAR(255) DEFAULT NULL COMMENT '图片名称',
    image_url VARCHAR(500) DEFAULT NULL COMMENT '图片访问URL',
    annotated_image_url VARCHAR(500) DEFAULT NULL COMMENT '标注后图片URL',
    image_path VARCHAR(500) NOT NULL COMMENT '图片存储路径',
    thumbnail_path VARCHAR(500) DEFAULT NULL COMMENT '缩略图路径',
    annotated_path VARCHAR(500) DEFAULT NULL COMMENT '标注后图片路径',
    image_type VARCHAR(20) DEFAULT 'visible' COMMENT '图像类型 visible-可见光 infrared-红外 thermal-热成像',
    image_size BIGINT DEFAULT NULL COMMENT '文件大小(字节)',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '拍摄经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '拍摄纬度',
    altitude DECIMAL(8,2) DEFAULT NULL COMMENT '飞行高度(米)',
    shoot_time DATETIME DEFAULT NULL COMMENT '拍摄时间',
    camera_model VARCHAR(50) DEFAULT NULL COMMENT '相机型号',
    image_width INT DEFAULT NULL COMMENT '图像宽度(px)',
    image_height INT DEFAULT NULL COMMENT '图像高度(px)',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小(字节)',
    detect_status TINYINT DEFAULT 0 COMMENT '检测状态 0-待检测 1-检测中 2-检测完成 3-检测失败',
    detect_start_time DATETIME DEFAULT NULL COMMENT '检测开始时间',
    detect_end_time DATETIME DEFAULT NULL COMMENT '检测结束时间',
    detect_result TEXT DEFAULT NULL COMMENT 'AI检测结果JSON',
    defect_count INT DEFAULT 0 COMMENT '检测缺陷数量',
    detect_time DATETIME DEFAULT NULL COMMENT '检测完成时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_station_id (station_id),
    KEY idx_detect_status (detect_status),
    KEY idx_shoot_time (shoot_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检图像表';

-- =====================================================
-- 19. 缺陷识别表
-- =====================================================
DROP TABLE IF EXISTS drone_defect;
CREATE TABLE drone_defect (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '巡检任务ID',
    image_id BIGINT NOT NULL COMMENT '图像ID',
    station_id BIGINT DEFAULT NULL COMMENT '电站ID',
    defect_code VARCHAR(50) DEFAULT NULL COMMENT '缺陷编号',
    defect_type VARCHAR(30) NOT NULL COMMENT '缺陷类型 hot_spot-热斑 microcrack-隐裂 shadow-遮挡 delamination-脱层 broken-破损 dirt-脏污',
    defect_level TINYINT DEFAULT 2 COMMENT '缺陷等级 1-轻微 2-一般 3-严重 4-紧急',
    confidence DECIMAL(5,4) DEFAULT NULL COMMENT '置信度 0-1',
    bbox_x1 INT DEFAULT NULL COMMENT '边界框左上角X坐标',
    bbox_y1 INT DEFAULT NULL COMMENT '边界框左上角Y坐标',
    bbox_x2 INT DEFAULT NULL COMMENT '边界框右下角X坐标',
    bbox_y2 INT DEFAULT NULL COMMENT '边界框右下角Y坐标',
    x_min INT DEFAULT NULL COMMENT '边界框左上角X坐标(兼容)',
    y_min INT DEFAULT NULL COMMENT '边界框左上角Y坐标(兼容)',
    x_max INT DEFAULT NULL COMMENT '边界框右下角X坐标(兼容)',
    y_max INT DEFAULT NULL COMMENT '边界框右下角Y坐标(兼容)',
    center_x INT DEFAULT NULL COMMENT '中心点X坐标',
    center_y INT DEFAULT NULL COMMENT '中心点Y坐标',
    bbox_width INT DEFAULT NULL COMMENT '边界框宽度',
    bbox_height INT DEFAULT NULL COMMENT '边界框高度',
    area_ratio DECIMAL(8,4) DEFAULT NULL COMMENT '缺陷占比(%)',
    temperature DECIMAL(8,2) DEFAULT NULL COMMENT '温度(℃) 红外图像专用',
    max_temperature DECIMAL(8,2) DEFAULT NULL COMMENT '最高温度(℃)',
    min_temperature DECIMAL(8,2) DEFAULT NULL COMMENT '最低温度(℃)',
    delta_temperature DECIMAL(8,2) DEFAULT NULL COMMENT '温度差(℃)',
    component_row INT DEFAULT NULL COMMENT '组件行号',
    component_col INT DEFAULT NULL COMMENT '组件列号',
    component_code VARCHAR(50) DEFAULT NULL COMMENT '组件编号',
    gps_longitude DECIMAL(10,6) DEFAULT NULL COMMENT '缺陷GPS经度',
    gps_latitude DECIMAL(10,6) DEFAULT NULL COMMENT '缺陷GPS纬度',
    detect_time DATETIME DEFAULT NULL COMMENT '检测时间',
    confirmed TINYINT DEFAULT 0 COMMENT '是否确认 0-否 1-是',
    confirm_by BIGINT DEFAULT NULL COMMENT '确认人',
    confirm_time DATETIME DEFAULT NULL COMMENT '确认时间',
    confirm_remark VARCHAR(500) DEFAULT NULL COMMENT '确认备注',
    work_order_id BIGINT DEFAULT NULL COMMENT '关联工单ID',
    work_order_no VARCHAR(50) DEFAULT NULL COMMENT '关联工单编号',
    work_order_status TINYINT DEFAULT NULL COMMENT '工单状态',
    workorder_id BIGINT DEFAULT NULL COMMENT '关联工单ID(兼容)',
    status TINYINT DEFAULT 0 COMMENT '状态 0-待处理 1-处理中 2-已修复 3-已忽略',
    description VARCHAR(500) DEFAULT NULL COMMENT '缺陷描述',
    suggestion VARCHAR(500) DEFAULT NULL COMMENT '处理建议',
    verified TINYINT DEFAULT 0 COMMENT '是否人工确认 0-否 1-是(兼容)',
    verified_by BIGINT DEFAULT NULL COMMENT '确认人(兼容)',
    verified_time DATETIME DEFAULT NULL COMMENT '确认时间(兼容)',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_image_id (image_id),
    KEY idx_station_id (station_id),
    KEY idx_defect_type (defect_type),
    KEY idx_defect_level (defect_level),
    KEY idx_work_order_id (work_order_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷识别表';
