-- =====================================================
-- 多租户与集团管理 - 数据库脚本
-- 包含：组织架构、用户电站权限、数据隔离
-- =====================================================

USE solar_ops;

-- =====================================================
-- 1. 组织架构表
-- =====================================================
DROP TABLE IF EXISTS sys_org;
CREATE TABLE sys_org (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    org_code VARCHAR(50) NOT NULL COMMENT '组织编码',
    org_name VARCHAR(100) NOT NULL COMMENT '组织名称',
    org_type TINYINT NOT NULL DEFAULT 2 COMMENT '组织类型 1-集团总部 2-区域公司 3-电站',
    parent_id BIGINT DEFAULT 0 COMMENT '父级组织ID',
    leader_id BIGINT DEFAULT NULL COMMENT '负责人ID',
    leader_name VARCHAR(50) DEFAULT NULL COMMENT '负责人姓名',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0停用 1启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code (org_code),
    KEY idx_parent_id (parent_id),
    KEY idx_org_type (org_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构表';

-- =====================================================
-- 2. 用户-电站权限关联表
-- =====================================================
DROP TABLE IF EXISTS sys_user_station;
CREATE TABLE sys_user_station (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    station_id BIGINT NOT NULL COMMENT '电站ID',
    station_name VARCHAR(100) DEFAULT NULL COMMENT '电站名称（冗余）',
    permission_type TINYINT DEFAULT 1 COMMENT '权限类型 1-只读 2-读写 3-管理',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_station (user_id, station_id),
    KEY idx_user_id (user_id),
    KEY idx_station_id (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户电站权限关联表';

-- =====================================================
-- 3. 扩展电站表 - 增加组织关联
-- =====================================================
ALTER TABLE station ADD COLUMN org_id BIGINT DEFAULT NULL COMMENT '所属组织ID' AFTER status;
ALTER TABLE station ADD KEY idx_org_id (org_id);

-- =====================================================
-- 4. 扩展用户表 - 增加组织和管理员标识
-- =====================================================
ALTER TABLE sys_user ADD COLUMN org_id BIGINT DEFAULT NULL COMMENT '所属组织ID' AFTER role;
ALTER TABLE sys_user ADD COLUMN is_admin TINYINT DEFAULT 0 COMMENT '是否超级管理员 0-否 1-是' AFTER org_id;
ALTER TABLE sys_user ADD COLUMN data_scope TINYINT DEFAULT 1 COMMENT '数据权限范围 1-全部 2-本组织及以下 3-仅本人' AFTER is_admin;
ALTER TABLE sys_user ADD KEY idx_org_id (org_id);

-- =====================================================
-- 初始化数据
-- =====================================================

-- 初始化组织架构
INSERT INTO sys_org (org_code, org_name, org_type, parent_id, leader_name, sort_order, status, create_time, update_time) VALUES
('ORG-GROUP', '阳光新能源集团', 1, 0, '张总', 1, 1, NOW(), NOW()),
('ORG-BEIJING', '北京分公司', 2, 1, '王经理', 1, 1, NOW(), NOW()),
('ORG-SHANGHAI', '上海分公司', 2, 1, '李经理', 2, 1, NOW(), NOW()),
('ORG-GANSU', '甘肃分公司', 2, 1, '赵经理', 3, 1, NOW(), NOW());

-- 更新电站的组织关联
UPDATE station SET org_id = 2 WHERE station_code = 'ST-2024-001';
UPDATE station SET org_id = 3 WHERE station_code = 'ST-2024-002';
UPDATE station SET org_id = 4 WHERE station_code = 'ST-2024-003';

-- 更新用户信息
-- admin 为超级管理员
UPDATE sys_user SET org_id = 1, is_admin = 1, data_scope = 1 WHERE username = 'admin';
-- ops001 北京分公司运维，可看北京电站
UPDATE sys_user SET org_id = 2, is_admin = 0, data_scope = 2 WHERE username = 'ops001';
-- ops002 上海分公司运维，可看上海电站
UPDATE sys_user SET org_id = 3, is_admin = 0, data_scope = 2 WHERE username = 'ops002';

-- 初始化用户-电站权限
INSERT INTO sys_user_station (user_id, station_id, station_name, permission_type, create_time) VALUES
-- ops001 有北京电站权限
(2, 1, '阳光一号光伏电站', 3, NOW()),
-- ops002 有上海电站权限
(3, 2, '绿色能源二号电站', 3, NOW());
