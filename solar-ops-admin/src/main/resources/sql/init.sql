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
