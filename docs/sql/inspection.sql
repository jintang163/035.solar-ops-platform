-- =====================================================
-- 离线巡检模块 - 数据库初始化脚本
-- =====================================================

USE solar_ops;

-- =====================================================
-- 1. 巡检任务表
-- =====================================================
DROP TABLE IF EXISTS inspection_task;
CREATE TABLE inspection_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_no VARCHAR(64) NOT NULL COMMENT '任务编号',
    task_name VARCHAR(200) NOT NULL COMMENT '任务名称',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    station_name VARCHAR(100) DEFAULT NULL COMMENT '电站名称',
    task_type TINYINT NOT NULL DEFAULT 1 COMMENT '任务类型 1-日常巡检 2-专项巡检 3-定期检修',
    priority TINYINT NOT NULL DEFAULT 2 COMMENT '优先级 1-低 2-中 3-高',
    plan_start_time DATETIME DEFAULT NULL COMMENT '计划开始时间',
    plan_end_time DATETIME DEFAULT NULL COMMENT '计划结束时间',
    actual_start_time DATETIME DEFAULT NULL COMMENT '实际开始时间',
    actual_end_time DATETIME DEFAULT NULL COMMENT '实际结束时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待下载 1-已下载 2-执行中 3-已完成 4-已取消',
    assignee_id BIGINT DEFAULT NULL COMMENT '指派人员ID',
    assignee_name VARCHAR(64) DEFAULT NULL COMMENT '指派人员姓名',
    description TEXT DEFAULT NULL COMMENT '任务描述',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_station_id (station_id),
    KEY idx_status (status),
    KEY idx_assignee_id (assignee_id),
    KEY idx_plan_start (plan_start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检任务表';

-- =====================================================
-- 2. 巡检项表（检查项模板）
-- =====================================================
DROP TABLE IF EXISTS inspection_item;
CREATE TABLE inspection_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    item_code VARCHAR(64) NOT NULL COMMENT '检查项编号',
    item_name VARCHAR(200) NOT NULL COMMENT '检查项名称',
    item_type TINYINT NOT NULL DEFAULT 1 COMMENT '检查项类型 1-外观检查 2-仪表读数 3-声音检查 4-红外测温 5-功能测试',
    asset_type VARCHAR(50) DEFAULT NULL COMMENT '适用资产类型',
    standard_value VARCHAR(100) DEFAULT NULL COMMENT '标准值',
    min_value DECIMAL(12,4) DEFAULT NULL COMMENT '最小值阈值',
    max_value DECIMAL(12,4) DEFAULT NULL COMMENT '最大值阈值',
    unit VARCHAR(20) DEFAULT NULL COMMENT '单位',
    is_required TINYINT NOT NULL DEFAULT 1 COMMENT '是否必填 0-否 1-是',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    description TEXT DEFAULT NULL COMMENT '检查说明',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_code (item_code),
    KEY idx_asset_type (asset_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检项表';

-- =====================================================
-- 3. 巡检任务-检查项关联表
-- =====================================================
DROP TABLE IF EXISTS inspection_task_item;
CREATE TABLE inspection_task_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    item_id BIGINT NOT NULL COMMENT '检查项ID',
    asset_id BIGINT DEFAULT NULL COMMENT '关联资产ID',
    asset_name VARCHAR(100) DEFAULT NULL COMMENT '资产名称',
    asset_code VARCHAR(64) DEFAULT NULL COMMENT '资产编号',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_item_id (item_id),
    KEY idx_asset_id (asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检任务-检查项关联表';

-- =====================================================
-- 4. 巡检结果表
-- =====================================================
DROP TABLE IF EXISTS inspection_result;
CREATE TABLE inspection_result (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    result_no VARCHAR(64) NOT NULL COMMENT '结果编号',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    task_no VARCHAR(64) DEFAULT NULL COMMENT '任务编号',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    station_name VARCHAR(100) DEFAULT NULL COMMENT '电站名称',
    inspector_id BIGINT DEFAULT NULL COMMENT '巡检人员ID',
    inspector_name VARCHAR(64) DEFAULT NULL COMMENT '巡检人员姓名',
    start_time DATETIME DEFAULT NULL COMMENT '巡检开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '巡检结束时间',
    total_items INT DEFAULT 0 COMMENT '总检查项数',
    normal_items INT DEFAULT 0 COMMENT '正常项数',
    abnormal_items INT DEFAULT 0 COMMENT '异常项数',
    result_status TINYINT NOT NULL DEFAULT 1 COMMENT '结果状态 1-正常 2-异常 3-待复核',
    overall_remark TEXT DEFAULT NULL COMMENT '总体评价',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '巡检位置经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '巡检位置纬度',
    is_offline TINYINT DEFAULT 0 COMMENT '是否离线提交 0-否 1-是',
    upload_time DATETIME DEFAULT NULL COMMENT '上传时间',
    sync_status TINYINT DEFAULT 0 COMMENT '同步状态 0-待同步 1-已同步 2-同步失败',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_result_no (result_no),
    KEY idx_task_id (task_id),
    KEY idx_station_id (station_id),
    KEY idx_inspector_id (inspector_id),
    KEY idx_result_status (result_status),
    KEY idx_sync_status (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检结果表';

-- =====================================================
-- 5. 巡检结果明细表
-- =====================================================
DROP TABLE IF EXISTS inspection_result_item;
CREATE TABLE inspection_result_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    result_id BIGINT NOT NULL COMMENT '结果ID',
    task_item_id BIGINT NOT NULL COMMENT '任务检查项ID',
    item_id BIGINT NOT NULL COMMENT '检查项ID',
    item_name VARCHAR(200) DEFAULT NULL COMMENT '检查项名称',
    item_type TINYINT DEFAULT NULL COMMENT '检查项类型',
    asset_id BIGINT DEFAULT NULL COMMENT '资产ID',
    asset_name VARCHAR(100) DEFAULT NULL COMMENT '资产名称',
    asset_code VARCHAR(64) DEFAULT NULL COMMENT '资产编号',
    check_value VARCHAR(500) DEFAULT NULL COMMENT '检查值',
    standard_value VARCHAR(100) DEFAULT NULL COMMENT '标准值',
    is_normal TINYINT NOT NULL DEFAULT 1 COMMENT '是否正常 0-异常 1-正常',
    abnormal_desc TEXT DEFAULT NULL COMMENT '异常描述',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    check_time DATETIME DEFAULT NULL COMMENT '检查时间',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '检查位置经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '检查位置纬度',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_result_id (result_id),
    KEY idx_item_id (item_id),
    KEY idx_asset_id (asset_id),
    KEY idx_is_normal (is_normal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检结果明细表';

-- =====================================================
-- 6. 巡检照片表
-- =====================================================
DROP TABLE IF EXISTS inspection_photo;
CREATE TABLE inspection_photo (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    photo_no VARCHAR(64) NOT NULL COMMENT '照片编号',
    result_id BIGINT DEFAULT NULL COMMENT '结果ID',
    result_item_id BIGINT DEFAULT NULL COMMENT '结果明细ID',
    task_id BIGINT DEFAULT NULL COMMENT '任务ID',
    asset_id BIGINT DEFAULT NULL COMMENT '资产ID',
    photo_type TINYINT NOT NULL DEFAULT 1 COMMENT '照片类型 1-普通照片 2-红外照片 3-仪表照片',
    photo_url VARCHAR(500) NOT NULL COMMENT '照片URL',
    thumbnail_url VARCHAR(500) DEFAULT NULL COMMENT '缩略图URL',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小(字节)',
    watermark_time DATETIME DEFAULT NULL COMMENT '水印时间',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '拍摄位置经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '拍摄位置纬度',
    has_watermark TINYINT DEFAULT 0 COMMENT '是否有水印 0-否 1-是',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    is_offline TINYINT DEFAULT 0 COMMENT '是否离线拍摄 0-否 1-是',
    sync_status TINYINT DEFAULT 0 COMMENT '同步状态 0-待同步 1-已同步 2-同步失败',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_photo_no (photo_no),
    KEY idx_result_id (result_id),
    KEY idx_result_item_id (result_item_id),
    KEY idx_task_id (task_id),
    KEY idx_asset_id (asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检照片表';

-- =====================================================
-- 7. 巡检录音表
-- =====================================================
DROP TABLE IF EXISTS inspection_audio;
CREATE TABLE inspection_audio (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    audio_no VARCHAR(64) NOT NULL COMMENT '录音编号',
    result_id BIGINT DEFAULT NULL COMMENT '结果ID',
    result_item_id BIGINT DEFAULT NULL COMMENT '结果明细ID',
    task_id BIGINT DEFAULT NULL COMMENT '任务ID',
    asset_id BIGINT DEFAULT NULL COMMENT '资产ID',
    audio_url VARCHAR(500) NOT NULL COMMENT '录音URL',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小(字节)',
    duration INT DEFAULT NULL COMMENT '录音时长(秒)',
    record_time DATETIME DEFAULT NULL COMMENT '录制时间',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '录制位置经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '录制位置纬度',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    is_offline TINYINT DEFAULT 0 COMMENT '是否离线录制 0-否 1-是',
    sync_status TINYINT DEFAULT 0 COMMENT '同步状态 0-待同步 1-已同步 2-同步失败',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_audio_no (audio_no),
    KEY idx_result_id (result_id),
    KEY idx_result_item_id (result_item_id),
    KEY idx_task_id (task_id),
    KEY idx_asset_id (asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检录音表';

-- =====================================================
-- 8. 巡检报告表
-- =====================================================
DROP TABLE IF EXISTS inspection_report;
CREATE TABLE inspection_report (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    report_no VARCHAR(64) NOT NULL COMMENT '报告编号',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    result_id BIGINT NOT NULL COMMENT '结果ID',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    station_name VARCHAR(100) DEFAULT NULL COMMENT '电站名称',
    report_title VARCHAR(200) DEFAULT NULL COMMENT '报告标题',
    report_type TINYINT NOT NULL DEFAULT 1 COMMENT '报告类型 1-日常巡检报告 2-专项巡检报告',
    total_score DECIMAL(5,2) DEFAULT NULL COMMENT '综合评分',
    health_level TINYINT DEFAULT NULL COMMENT '健康等级 1-优秀 2-良好 3-一般 4-较差',
    total_items INT DEFAULT 0 COMMENT '总检查项',
    pass_rate DECIMAL(5,2) DEFAULT NULL COMMENT '通过率(%)',
    abnormal_count INT DEFAULT 0 COMMENT '异常项数量',
    problem_summary TEXT DEFAULT NULL COMMENT '问题汇总',
    suggestions TEXT DEFAULT NULL COMMENT '处理建议',
    report_content TEXT DEFAULT NULL COMMENT '报告内容(JSON格式，用于前端渲染)',
    generated_time DATETIME DEFAULT NULL COMMENT '生成时间',
    generator_id BIGINT DEFAULT NULL COMMENT '生成人ID',
    generator_name VARCHAR(64) DEFAULT NULL COMMENT '生成人姓名',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_no (report_no),
    KEY idx_task_id (task_id),
    KEY idx_result_id (result_id),
    KEY idx_station_id (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检报告表';

-- =====================================================
-- 初始化巡检项数据
-- =====================================================
INSERT INTO inspection_item (item_code, item_name, item_type, asset_type, standard_value, min_value, max_value, unit, is_required, sort_order, description, create_time, update_time) VALUES
('INV-001', '逆变器外观检查', 1, 'inverter', '完好', NULL, NULL, NULL, 1, 1, '检查逆变器外壳是否完好，有无变形、破损、锈蚀', NOW(), NOW()),
('INV-002', '逆变器运行声音', 3, 'inverter', '正常', NULL, NULL, NULL, 1, 2, '听逆变器运行声音，应无异常杂音、异响', NOW(), NOW()),
('INV-003', '直流电压', 2, 'inverter', '600-1000', 500.0000, 1100.0000, 'V', 1, 3, '测量直流侧输入电压', NOW(), NOW()),
('INV-004', '交流电压', 2, 'inverter', '380±10%', 342.0000, 418.0000, 'V', 1, 4, '测量交流侧输出电压', NOW(), NOW()),
('INV-005', '逆变器温度', 4, 'inverter', '<75', NULL, 75.0000, '℃', 1, 5, '红外测温，检查逆变器内部温度', NOW(), NOW()),
('INV-006', '散热风扇运转', 5, 'inverter', '正常', NULL, NULL, NULL, 1, 6, '检查散热风扇是否正常运转', NOW(), NOW()),
('INV-007', '显示屏状态', 1, 'inverter', '正常显示', NULL, NULL, NULL, 0, 7, '检查显示屏是否正常显示运行数据', NOW(), NOW()),
('CB-001', '汇流箱外观检查', 1, 'combiner', '完好', NULL, NULL, NULL, 1, 1, '检查汇流箱外壳是否完好，密封是否良好', NOW(), NOW()),
('CB-002', '汇流箱温度', 4, 'combiner', '<60', NULL, 60.0000, '℃', 1, 2, '红外测温，检查汇流箱内部温度', NOW(), NOW()),
('CB-003', '熔断器状态', 5, 'combiner', '正常', NULL, NULL, NULL, 1, 3, '检查各支路熔断器状态', NOW(), NOW()),
('PL-001', '组件外观检查', 1, 'panel', '完好', NULL, NULL, NULL, 1, 1, '检查光伏组件玻璃是否完好，有无碎裂、隐裂', NOW(), NOW()),
('PL-002', '组件热斑检测', 4, 'panel', '无热斑', NULL, NULL, NULL, 1, 2, '红外检测组件表面温度，检查是否有热斑', NOW(), NOW()),
('TR-001', '变压器外观检查', 1, 'transformer', '完好', NULL, NULL, NULL, 1, 1, '检查变压器外观，有无渗漏油', NOW(), NOW()),
('TR-002', '变压器油温', 2, 'transformer', '<85', NULL, 85.0000, '℃', 1, 2, '检查变压器油温度', NOW(), NOW()),
('ST-001', '电站整体环境', 1, 'station', '整洁', NULL, NULL, NULL, 1, 1, '检查电站整体环境，有无杂物堆积', NOW(), NOW());

-- =====================================================
-- 初始化示例巡检任务数据
-- =====================================================
INSERT INTO inspection_task (task_no, task_name, station_id, station_name, task_type, priority, plan_start_time, plan_end_time, status, assignee_id, assignee_name, description, create_time, update_time) VALUES
('IT20240101001', '阳光一号电站1月日常巡检', 1, '阳光一号光伏电站', 1, 2, '2024-01-20 08:00:00', '2024-01-20 18:00:00', 0, 2, '运维工程师-王工', '1月份日常巡检，检查逆变器、汇流箱、光伏组件等设备运行状况', NOW(), NOW()),
('IT20240101002', '绿色能源二号电站1月日常巡检', 2, '绿色能源二号电站', 1, 2, '2024-01-22 08:00:00', '2024-01-22 18:00:00', 0, 3, '运维工程师-李工', '1月份日常巡检', NOW(), NOW()),
('IT20240101003', '西部太阳能电站专项巡检', 3, '西部太阳能发电站', 2, 3, '2024-01-25 08:00:00', '2024-01-27 18:00:00', 0, 2, '运维工程师-王工', '冬季专项巡检，重点检查设备低温运行状态', NOW(), NOW());

-- =====================================================
-- 初始化示例巡检任务-检查项关联数据
-- =====================================================
INSERT INTO inspection_task_item (task_id, item_id, asset_id, asset_name, asset_code, sort_order, create_time) VALUES
(1, 1, 2, '1号逆变器', 'AST-INV001', 1, NOW()),
(1, 2, 2, '1号逆变器', 'AST-INV001', 2, NOW()),
(1, 3, 2, '1号逆变器', 'AST-INV001', 3, NOW()),
(1, 4, 2, '1号逆变器', 'AST-INV001', 4, NOW()),
(1, 5, 2, '1号逆变器', 'AST-INV001', 5, NOW()),
(1, 6, 2, '1号逆变器', 'AST-INV001', 6, NOW()),
(1, 1, 3, '2号逆变器', 'AST-INV002', 7, NOW()),
(1, 2, 3, '2号逆变器', 'AST-INV002', 8, NOW()),
(1, 3, 3, '2号逆变器', 'AST-INV002', 9, NOW()),
(1, 4, 3, '2号逆变器', 'AST-INV002', 10, NOW()),
(1, 5, 3, '2号逆变器', 'AST-INV002', 11, NOW()),
(1, 6, 3, '2号逆变器', 'AST-INV002', 12, NOW()),
(1, 8, 5, 'A区1号汇流箱', 'AST-CB001', 13, NOW()),
(1, 9, 5, 'A区1号汇流箱', 'AST-CB001', 14, NOW()),
(1, 10, 5, 'A区1号汇流箱', 'AST-CB001', 15, NOW()),
(1, 11, 6, 'A区光伏组串1', 'AST-PL001', 16, NOW()),
(1, 12, 6, 'A区光伏组串1', 'AST-PL001', 17, NOW()),
(1, 13, 7, '1号变压器', 'AST-TR001', 18, NOW()),
(1, 14, 7, '1号变压器', 'AST-TR001', 19, NOW()),
(1, 15, 1, '阳光一号光伏电站', 'AST-ST001', 20, NOW());
