-- 工单智能调度相关表
-- 运维人员位置表
CREATE TABLE IF NOT EXISTS `operator_location` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '运维人员ID',
    `user_name` varchar(64) DEFAULT NULL COMMENT '运维人员姓名',
    `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
    `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
    `accuracy` decimal(10,2) DEFAULT NULL COMMENT '定位精度(米)',
    `speed` decimal(10,2) DEFAULT NULL COMMENT '速度(km/h)',
    `heading` decimal(5,2) DEFAULT NULL COMMENT '方向角度(0-360)',
    `location_type` varchar(32) DEFAULT NULL COMMENT '定位方式 GPS/WIFI/BASE',
    `report_time` bigint DEFAULT NULL COMMENT '上报时间戳',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除 0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_longitude_latitude` (`longitude`, `latitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维人员位置表';

-- 索引优化：位置查询
ALTER TABLE `operator_location` ADD INDEX `idx_location_deleted` (`deleted`, `longitude`, `latitude`);

-- 技能标签表
CREATE TABLE IF NOT EXISTS `sys_skill_tag` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tag_name` varchar(64) NOT NULL COMMENT '标签名称',
    `tag_code` varchar(64) DEFAULT NULL COMMENT '标签编码',
    `category` varchar(64) DEFAULT NULL COMMENT '标签分类',
    `description` varchar(255) DEFAULT NULL COMMENT '描述',
    `sort` int DEFAULT 0 COMMENT '排序',
    `status` tinyint(1) DEFAULT 1 COMMENT '状态 0停用 1启用',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除 0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_code` (`tag_code`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能标签表';

-- 用户技能关联表
CREATE TABLE IF NOT EXISTS `sys_user_skill` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `tag_id` bigint NOT NULL COMMENT '标签ID',
    `proficiency` tinyint DEFAULT 2 COMMENT '熟练程度 1-入门 2-熟练 3-精通',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除 0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_tag_id` (`tag_id`),
    UNIQUE KEY `uk_user_tag` (`user_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户技能关联表';

-- 初始化技能标签数据
INSERT INTO `sys_skill_tag` (`tag_name`, `tag_code`, `category`, `description`, `sort`) VALUES
('逆变器维修', 'inverter_repair', '电气类', '逆变器故障诊断与维修', 1),
('组件清洗', 'panel_cleaning', '运维类', '光伏组件清洁作业', 2),
('电气检修', 'electrical_check', '电气类', '电气设备巡检与检修', 3),
('常规运维', 'routine_maintenance', '运维类', '日常巡检与维护', 4),
('红外检测', 'thermal_inspection', '检测类', '红外热像仪检测设备', 5),
('无人机巡检', 'drone_inspection', '检测类', '无人机航拍巡检', 6),
('数据分析', 'data_analysis', '技术类', '运行数据诊断分析', 7),
('安全管理', 'safety_manage', '管理类', '现场安全管理', 8);

