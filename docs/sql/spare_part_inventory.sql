-- =====================================================
-- 备件库存管理 - 数据库表结构
-- Database: solar_ops
-- 生成日期: 2024-01-01
-- =====================================================

USE solar_ops;

-- =====================================================
-- 1. 备件库存表
-- =====================================================
DROP TABLE IF EXISTS spare_part_inventory;
CREATE TABLE spare_part_inventory (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    part_code VARCHAR(64) NOT NULL COMMENT '备件编号',
    part_name VARCHAR(100) NOT NULL COMMENT '备件名称',
    part_type VARCHAR(50) NOT NULL COMMENT '备件类型：fan-风扇 capacitor-电容 board-板卡 other-其他',
    part_model VARCHAR(100) DEFAULT NULL COMMENT '备件型号',
    brand VARCHAR(100) DEFAULT NULL COMMENT '品牌',
    specification VARCHAR(255) DEFAULT NULL COMMENT '规格参数',
    unit VARCHAR(20) DEFAULT '个' COMMENT '单位',
    unit_price DECIMAL(12,2) DEFAULT NULL COMMENT '单价',
    quantity INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    safe_quantity INT NOT NULL DEFAULT 10 COMMENT '安全库存数量',
    min_purchase_quantity INT DEFAULT 10 COMMENT '最小采购数量',
    storage_location VARCHAR(255) DEFAULT NULL COMMENT '存放位置',
    warehouse VARCHAR(50) DEFAULT NULL COMMENT '所属仓库',
    supplier VARCHAR(100) DEFAULT NULL COMMENT '供应商',
    manufacturer VARCHAR(100) DEFAULT NULL COMMENT '生产厂家',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
    warn_status TINYINT NOT NULL DEFAULT 0 COMMENT '预警状态 0-正常 1-低库存预警 2-库存不足',
    qr_code_url VARCHAR(255) DEFAULT NULL COMMENT '二维码图片地址',
    remark TEXT DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_part_code (part_code),
    KEY idx_part_type (part_type),
    KEY idx_status (status),
    KEY idx_warn_status (warn_status),
    KEY idx_storage_location (storage_location),
    KEY idx_warehouse (warehouse)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件库存表';

-- =====================================================
-- 2. 备件出入库记录表
-- =====================================================
DROP TABLE IF EXISTS spare_part_in_out_record;
CREATE TABLE spare_part_in_out_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    record_no VARCHAR(64) NOT NULL COMMENT '出入库单号',
    record_type TINYINT NOT NULL COMMENT '记录类型 1-入库 2-出库',
    in_out_type TINYINT NOT NULL COMMENT '出入库类型 11-采购入库 12-盘盈入库 13-退库入库 21-工单出库 22-盘亏出库 23-报废出库',
    part_id BIGINT NOT NULL COMMENT '备件ID',
    part_code VARCHAR(64) NOT NULL COMMENT '备件编号',
    part_name VARCHAR(100) NOT NULL COMMENT '备件名称',
    part_model VARCHAR(100) DEFAULT NULL COMMENT '备件型号',
    quantity INT NOT NULL COMMENT '数量',
    unit_price DECIMAL(12,2) DEFAULT NULL COMMENT '单价',
    total_price DECIMAL(12,2) DEFAULT NULL COMMENT '总价',
    before_quantity INT DEFAULT NULL COMMENT '变动前库存',
    after_quantity INT DEFAULT NULL COMMENT '变动后库存',
    work_order_id BIGINT DEFAULT NULL COMMENT '关联工单ID',
    work_order_no VARCHAR(64) DEFAULT NULL COMMENT '工单编号',
    maintenance_record_id BIGINT DEFAULT NULL COMMENT '维修记录ID',
    asset_id BIGINT DEFAULT NULL COMMENT '资产ID',
    supplier VARCHAR(100) DEFAULT NULL COMMENT '供应商',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    operate_time DATETIME DEFAULT NULL COMMENT '操作时间',
    storage_location VARCHAR(255) DEFAULT NULL COMMENT '存放位置',
    batch_no VARCHAR(64) DEFAULT NULL COMMENT '批次号',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_record_no (record_no),
    KEY idx_part_id (part_id),
    KEY idx_record_type (record_type),
    KEY idx_in_out_type (in_out_type),
    KEY idx_work_order_id (work_order_id),
    KEY idx_operate_time (operate_time),
    KEY idx_operator_id (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件出入库记录表';

-- =====================================================
-- 3. 备件库存盘点主表
-- =====================================================
DROP TABLE IF EXISTS spare_part_stocktake;
CREATE TABLE spare_part_stocktake (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stocktake_no VARCHAR(64) NOT NULL COMMENT '盘点单号',
    stocktake_name VARCHAR(200) NOT NULL COMMENT '盘点名称',
    stocktake_type TINYINT NOT NULL DEFAULT 1 COMMENT '盘点类型 1-全盘 2-抽盘 3-专项盘点',
    warehouse VARCHAR(50) DEFAULT NULL COMMENT '盘点仓库',
    part_type VARCHAR(50) DEFAULT NULL COMMENT '盘点备件类型',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待盘点 1-盘点中 2-已完成 3-已取消',
    total_count INT DEFAULT 0 COMMENT '盘点备件总数',
    diff_count INT DEFAULT 0 COMMENT '差异数量',
    profit_quantity INT DEFAULT 0 COMMENT '盘盈总数',
    loss_quantity INT DEFAULT 0 COMMENT '盘亏总数',
    total_amount DECIMAL(14,2) DEFAULT 0.00 COMMENT '库存总金额',
    diff_amount DECIMAL(14,2) DEFAULT 0.00 COMMENT '差异总金额',
    stocktake_time DATETIME DEFAULT NULL COMMENT '盘点时间',
    operator_id BIGINT DEFAULT NULL COMMENT '盘点人ID',
    operator_name VARCHAR(50) DEFAULT NULL COMMENT '盘点人姓名',
    complete_time DATETIME DEFAULT NULL COMMENT '完成时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stocktake_no (stocktake_no),
    KEY idx_status (status),
    KEY idx_stocktake_type (stocktake_type),
    KEY idx_warehouse (warehouse),
    KEY idx_stocktake_time (stocktake_time),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件库存盘点主表';

-- =====================================================
-- 4. 备件库存盘点明细表
-- =====================================================
DROP TABLE IF EXISTS spare_part_stocktake_item;
CREATE TABLE spare_part_stocktake_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stocktake_id BIGINT NOT NULL COMMENT '盘点单ID',
    stocktake_no VARCHAR(64) NOT NULL COMMENT '盘点单号',
    part_id BIGINT NOT NULL COMMENT '备件ID',
    part_code VARCHAR(64) NOT NULL COMMENT '备件编号',
    part_name VARCHAR(100) NOT NULL COMMENT '备件名称',
    part_model VARCHAR(100) DEFAULT NULL COMMENT '备件型号',
    part_type VARCHAR(50) DEFAULT NULL COMMENT '备件类型',
    unit VARCHAR(20) DEFAULT '个' COMMENT '单位',
    unit_price DECIMAL(12,2) DEFAULT NULL COMMENT '单价',
    system_quantity INT NOT NULL DEFAULT 0 COMMENT '系统库存数量',
    actual_quantity INT NOT NULL DEFAULT 0 COMMENT '实际盘点数量',
    diff_quantity INT NOT NULL DEFAULT 0 COMMENT '差异数量（实际-系统）',
    diff_type TINYINT DEFAULT 0 COMMENT '差异类型 0-无差异 1-盘盈 2-盘亏',
    diff_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '差异金额',
    storage_location VARCHAR(255) DEFAULT NULL COMMENT '存放位置',
    remark VARCHAR(512) DEFAULT NULL COMMENT '差异原因',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_stocktake_id (stocktake_id),
    KEY idx_part_id (part_id),
    KEY idx_diff_type (diff_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件库存盘点明细表';

-- =====================================================
-- 5. 采购建议表
-- =====================================================
DROP TABLE IF EXISTS spare_part_purchase_suggestion;
CREATE TABLE spare_part_purchase_suggestion (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    suggestion_no VARCHAR(64) NOT NULL COMMENT '建议单号',
    part_id BIGINT NOT NULL COMMENT '备件ID',
    part_code VARCHAR(64) NOT NULL COMMENT '备件编号',
    part_name VARCHAR(100) NOT NULL COMMENT '备件名称',
    part_model VARCHAR(100) DEFAULT NULL COMMENT '备件型号',
    part_type VARCHAR(50) DEFAULT NULL COMMENT '备件类型',
    current_quantity INT NOT NULL DEFAULT 0 COMMENT '当前库存',
    safe_quantity INT NOT NULL DEFAULT 0 COMMENT '安全库存',
    suggest_quantity INT NOT NULL DEFAULT 0 COMMENT '建议采购数量',
    min_purchase_quantity INT DEFAULT 10 COMMENT '最小采购数量',
    unit_price DECIMAL(12,2) DEFAULT NULL COMMENT '单价',
    estimated_amount DECIMAL(12,2) DEFAULT NULL COMMENT '预估金额',
    supplier VARCHAR(100) DEFAULT NULL COMMENT '供应商',
    urgency TINYINT NOT NULL DEFAULT 1 COMMENT '紧急程度 1-一般 2-紧急 3-非常紧急',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待处理 1-已采购 2-已忽略',
    generate_time DATETIME DEFAULT NULL COMMENT '生成时间',
    processor_id BIGINT DEFAULT NULL COMMENT '处理人ID',
    processor_name VARCHAR(50) DEFAULT NULL COMMENT '处理人姓名',
    process_time DATETIME DEFAULT NULL COMMENT '处理时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_suggestion_no (suggestion_no),
    KEY idx_part_id (part_id),
    KEY idx_status (status),
    KEY idx_urgency (urgency),
    KEY idx_generate_time (generate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件采购建议表';

-- =====================================================
-- 初始化备件库存数据
-- =====================================================
INSERT INTO spare_part_inventory (part_code, part_name, part_type, part_model, brand, specification, unit, unit_price, quantity, safe_quantity, min_purchase_quantity, storage_location, warehouse, supplier, manufacturer, status, warn_status, remark, create_time, update_time) VALUES
('SP-FAN-001', '散热风扇', 'fan', 'FAN-12038', '台达', '120*120*38mm 24V', '个', 150.00, 25, 20, 10, 'A区-01货架-03层', '中心仓库', '台达电子有限公司', '台达电子', 1, 0, '逆变器散热风扇，常用备件', NOW(), NOW()),
('SP-FAN-002', '轴流风机', 'fan', 'FAN-8025', '建准', '80*80*25mm 12V', '个', 85.00, 8, 15, 10, 'A区-01货架-02层', '中心仓库', '建准电机工业', '建准电机', 1, 1, '低库存预警，建议采购', NOW(), NOW()),
('SP-CAP-001', '电解电容', 'capacitor', 'CAP-450V/1000uF', '江海', '450V 1000uF 螺丝型', '个', 65.00, 35, 30, 20, 'B区-02货架-01层', '中心仓库', '南通江海电容器', '南通江海', 1, 0, '逆变器直流侧电容', NOW(), NOW()),
('SP-CAP-002', '薄膜电容', 'capacitor', 'CAP-800V/50uF', '法拉', '800V 50uF CBB系列', '个', 45.00, 50, 30, 20, 'B区-02货架-02层', '中心仓库', '法拉电子', '法拉电子', 1, 0, '交流滤波电容', NOW(), NOW()),
('SP-BOARD-001', '控制板卡', 'board', 'BD-CONT-001', '阳光电源', '逆变器主控板', '块', 2800.00, 5, 10, 5, 'C区-01货架-01层', '中心仓库', '阳光电源股份', '阳光电源', 1, 2, '库存不足，急需采购', NOW(), NOW()),
('SP-BOARD-002', '驱动板卡', 'board', 'BD-DRIVE-001', '阳光电源', 'IGBT驱动板', '块', 1800.00, 12, 10, 5, 'C区-01货架-02层', '中心仓库', '阳光电源股份', '阳光电源', 1, 0, 'IGBT驱动板卡', NOW(), NOW()),
('SP-BOARD-003', '通讯板卡', 'board', 'BD-COMM-4G', '华为', '4G通讯模块板', '块', 950.00, 3, 10, 5, 'C区-02货架-01层', '中心仓库', '华为技术有限公司', '华为技术', 1, 2, '库存严重不足', NOW(), NOW()),
('SP-OTHER-001', '温度传感器', 'other', 'TEMP-NTC-10K', '日置', 'NTC 10K 温度探头', '个', 35.00, 100, 50, 30, 'D区-01货架-01层', '中心仓库', '日置电机', '日置电机', 1, 0, '温度检测传感器', NOW(), NOW());

-- =====================================================
-- 初始化出入库记录示例数据
-- =====================================================
INSERT INTO spare_part_in_out_record (record_no, record_type, in_out_type, part_id, part_code, part_name, part_model, quantity, unit_price, total_price, before_quantity, after_quantity, supplier, operator_name, operate_time, storage_location, remark, create_time, update_time) VALUES
('IN-20240105001', 1, 11, 1, 'SP-FAN-001', '散热风扇', 'FAN-12038', 30, 150.00, 4500.00, 0, 30, '台达电子有限公司', '管理员', '2024-01-05 10:30:00', 'A区-01货架-03层', '首批采购入库', NOW(), NOW()),
('IN-20240105002', 1, 11, 2, 'SP-FAN-002', '轴流风机', 'FAN-8025', 20, 85.00, 1700.00, 0, 20, '建准电机工业', '管理员', '2024-01-05 10:35:00', 'A区-01货架-02层', '首批采购入库', NOW(), NOW()),
('IN-20240106001', 1, 11, 3, 'SP-CAP-001', '电解电容', 'CAP-450V/1000uF', 50, 65.00, 3250.00, 0, 50, '南通江海电容器', '管理员', '2024-01-06 14:00:00', 'B区-02货架-01层', '首批采购入库', NOW(), NOW()),
('IN-20240106002', 1, 11, 4, 'SP-CAP-002', '薄膜电容', 'CAP-800V/50uF', 60, 45.00, 2700.00, 0, 60, '法拉电子', '管理员', '2024-01-06 14:20:00', 'B区-02货架-02层', '首批采购入库', NOW(), NOW()),
('IN-20240107001', 1, 11, 5, 'SP-BOARD-001', '控制板卡', 'BD-CONT-001', 10, 2800.00, 28000.00, 0, 10, '阳光电源股份', '管理员', '2024-01-07 09:00:00', 'C区-01货架-01层', '首批采购入库', NOW(), NOW()),
('IN-20240107002', 1, 11, 6, 'SP-BOARD-002', '驱动板卡', 'BD-DRIVE-001', 15, 1800.00, 27000.00, 0, 15, '阳光电源股份', '管理员', '2024-01-07 09:30:00', 'C区-01货架-02层', '首批采购入库', NOW(), NOW()),
('OUT-20240115001', 2, 21, 1, 'SP-FAN-001', '散热风扇', 'FAN-12038', 5, 150.00, 750.00, 30, 25, NULL, '王工', '2024-01-15 14:30:00', 'A区-01货架-03层', '工单WO20240115001 维修领用', NOW(), NOW()),
('OUT-20240116001', 2, 21, 5, 'SP-BOARD-001', '控制板卡', 'BD-CONT-001', 3, 2800.00, 8400.00, 10, 7, NULL, '李工', '2024-01-16 10:00:00', 'C区-01货架-01层', '工单WO20240116001 维修领用', NOW(), NOW()),
('OUT-20240118001', 2, 21, 2, 'SP-FAN-002', '轴流风机', 'FAN-8025', 12, 85.00, 1020.00, 20, 8, NULL, '张工', '2024-01-18 15:30:00', 'A区-01货架-02层', '工单WO20240118001 批量更换', NOW(), NOW()),
('OUT-20240120001', 2, 21, 5, 'SP-BOARD-001', '控制板卡', 'BD-CONT-001', 2, 2800.00, 5600.00, 7, 5, NULL, '王工', '2024-01-20 11:00:00', 'C区-01货架-01层', '工单WO20240120001 故障更换', NOW(), NOW()),
('OUT-20240122001', 2, 21, 7, 'SP-BOARD-003', '通讯板卡', 'BD-COMM-4G', 2, 950.00, 1900.00, 5, 3, NULL, '李工', '2024-01-22 16:00:00', 'C区-02货架-01层', '工单WO20240122001 通讯故障维修', NOW(), NOW());
