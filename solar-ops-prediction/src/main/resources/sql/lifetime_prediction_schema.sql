-- =====================================================
-- 设备寿命预测模块 - 数据库初始化脚本
-- =====================================================

USE solar_ops;

-- =====================================================
-- 17. 逆变器健康度记录表
-- =====================================================
DROP TABLE IF EXISTS inverter_health;
CREATE TABLE inverter_health (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    inverter_id BIGINT NOT NULL COMMENT '逆变器ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    avg_temperature DECIMAL(6,2) DEFAULT NULL COMMENT '平均温度(℃)',
    max_temperature DECIMAL(6,2) DEFAULT NULL COMMENT '最高温度(℃)',
    operating_hours DECIMAL(8,2) DEFAULT NULL COMMENT '工作时长(小时)',
    fault_count INT DEFAULT 0 COMMENT '故障次数',
    fault_severity INT DEFAULT 0 COMMENT '故障严重程度(1-5)',
    output_power_ratio DECIMAL(6,4) DEFAULT NULL COMMENT '输出功率比(0-1)',
    efficiency_drop DECIMAL(6,2) DEFAULT NULL COMMENT '效率下降率(%)',
    health_score DECIMAL(6,4) DEFAULT NULL COMMENT '健康度评分(0-1)',
    assessment_time DATETIME DEFAULT NULL COMMENT '评估时间',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_station_id (station_id),
    KEY idx_inverter_id (inverter_id),
    KEY idx_record_date (record_date),
    KEY idx_inverter_date (inverter_id, record_date),
    KEY idx_health_score (health_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逆变器健康度记录表';

-- =====================================================
-- 18. 寿命预测记录表
-- =====================================================
DROP TABLE IF EXISTS lifetime_prediction;
CREATE TABLE lifetime_prediction (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    inverter_id BIGINT NOT NULL COMMENT '逆变器ID',
    prediction_time DATETIME NOT NULL COMMENT '预测时间',
    current_health_score DECIMAL(6,4) DEFAULT NULL COMMENT '当前健康度评分(0-1)',
    remaining_life_days INT DEFAULT NULL COMMENT '预测剩余寿命(天)',
    forecast_days INT DEFAULT 90 COMMENT '预测未来天数',
    health_trend TEXT DEFAULT NULL COMMENT '健康度趋势数据(JSON数组)',
    confidence_trend TEXT DEFAULT NULL COMMENT '置信度趋势数据(JSON数组)',
    model_version VARCHAR(50) DEFAULT NULL COMMENT '模型版本',
    replacement_advice TINYINT DEFAULT 0 COMMENT '是否建议更换备件 0-否 1-是',
    alert_level TINYINT DEFAULT 1 COMMENT '预警级别 1-正常 2-注意 3-警告 4-紧急',
    remark VARCHAR(512) DEFAULT NULL COMMENT '预测说明',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_station_id (station_id),
    KEY idx_inverter_id (inverter_id),
    KEY idx_prediction_time (prediction_time),
    KEY idx_inverter_prediction (inverter_id, prediction_time),
    KEY idx_alert_level (alert_level),
    KEY idx_replacement_advice (replacement_advice)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='寿命预测记录表';

-- =====================================================
-- 19. 寿命预警表
-- =====================================================
DROP TABLE IF EXISTS lifetime_alert;
CREATE TABLE lifetime_alert (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    inverter_id BIGINT NOT NULL COMMENT '逆变器ID',
    alert_time DATETIME NOT NULL COMMENT '预警时间',
    alert_type TINYINT NOT NULL DEFAULT 1 COMMENT '预警类型 1-寿命预警 2-备件更换建议',
    alert_level TINYINT NOT NULL DEFAULT 2 COMMENT '预警级别 1-低 2-中 3-高 4-紧急',
    alert_title VARCHAR(200) DEFAULT NULL COMMENT '预警标题',
    alert_content VARCHAR(512) DEFAULT NULL COMMENT '预警内容',
    remaining_life_days INT DEFAULT NULL COMMENT '剩余寿命(天)',
    current_health DECIMAL(6,4) DEFAULT NULL COMMENT '当前健康度',
    spare_part VARCHAR(200) DEFAULT NULL COMMENT '建议备件',
    estimated_cost DECIMAL(12,2) DEFAULT NULL COMMENT '预估费用',
    status TINYINT DEFAULT 0 COMMENT '状态 0-未处理 1-已处理 2-已忽略',
    handle_time DATETIME DEFAULT NULL COMMENT '处理时间',
    handle_person VARCHAR(50) DEFAULT NULL COMMENT '处理人',
    handle_remark VARCHAR(512) DEFAULT NULL COMMENT '处理备注',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='寿命预警表';

-- =====================================================
-- 初始化示例数据
-- =====================================================

-- 插入示例健康度数据（逆变器ID=1，最近90天）
INSERT INTO inverter_health (station_id, inverter_id, record_date, avg_temperature, max_temperature, operating_hours, fault_count, fault_severity, output_power_ratio, efficiency_drop, health_score, assessment_time, create_time, update_time)
SELECT
    1,
    1,
    DATE_SUB(CURDATE(), INTERVAL t.n DAY),
    35 + RAND() * 15,
    45 + RAND() * 15,
    8 + RAND() * 4,
    CASE WHEN RAND() > 0.9 THEN 1 ELSE 0 END,
    CASE WHEN RAND() > 0.9 THEN FLOOR(1 + RAND() * 3) ELSE 0 END,
    0.85 + RAND() * 0.15,
    RAND() * 5,
    0.75 + RAND() * 0.2,
    DATE_SUB(NOW(), INTERVAL t.n DAY),
    DATE_SUB(NOW(), INTERVAL t.n DAY),
    DATE_SUB(NOW(), INTERVAL t.n DAY)
FROM (
    SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION
    SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION
    SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION
    SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION
    SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION
    SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION
    SELECT 30 UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION
    SELECT 35 UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION
    SELECT 40 UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION
    SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION
    SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION
    SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION
    SELECT 60 UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION
    SELECT 65 UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION
    SELECT 70 UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION
    SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79 UNION
    SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 UNION
    SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION
    SELECT 90
) t
WHERE DATE_SUB(CURDATE(), INTERVAL t.n DAY) < CURDATE();
