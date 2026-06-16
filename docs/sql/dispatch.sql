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
