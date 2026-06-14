-- =====================================================
-- 功率预测模块 - 数据库初始化脚本
-- =====================================================

USE solar_ops;

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
