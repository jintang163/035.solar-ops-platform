-- =====================================================
-- 运维知识库与智能推荐 - 数据库升级脚本
-- Database: solar_ops
-- 生成日期: 2026-06-16
-- =====================================================

USE solar_ops;

-- =====================================================
-- 1. 扩展故障库表为运维知识库表
-- =====================================================
ALTER TABLE fault_library 
    ADD COLUMN fault_type VARCHAR(50) DEFAULT NULL COMMENT '故障类型' AFTER fault_level,
    ADD COLUMN solution_rich_text LONGTEXT COMMENT '富文本解决方案' AFTER solution,
    ADD COLUMN attachments TEXT COMMENT '附件URL列表(JSON数组)' AFTER solution_rich_text,
    ADD COLUMN video_url VARCHAR(500) DEFAULT NULL COMMENT '视频教程URL' AFTER attachments,
    ADD COLUMN tags VARCHAR(500) DEFAULT NULL COMMENT '标签(逗号分隔)' AFTER video_url,
    ADD COLUMN like_count INT DEFAULT 0 COMMENT '点赞数' AFTER tags,
    ADD COLUMN dislike_count INT DEFAULT 0 COMMENT '点踩数' AFTER like_count,
    ADD COLUMN view_count INT DEFAULT 0 COMMENT '浏览次数' AFTER dislike_count,
    ADD COLUMN use_count INT DEFAULT 0 COMMENT '使用次数' AFTER view_count,
    ADD COLUMN creator_id BIGINT DEFAULT NULL COMMENT '创建人ID' AFTER use_count,
    ADD COLUMN creator_name VARCHAR(64) DEFAULT NULL COMMENT '创建人姓名' AFTER creator_id,
    ADD COLUMN status TINYINT DEFAULT 1 COMMENT '状态 0-草稿 1-已发布 2-已归档' AFTER creator_name;

-- =====================================================
-- 2. 新增知识库反馈表（点赞/点踩记录）
-- =====================================================
DROP TABLE IF EXISTS knowledge_feedback;
CREATE TABLE knowledge_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    knowledge_id BIGINT NOT NULL COMMENT '知识库ID',
    work_order_id BIGINT DEFAULT NULL COMMENT '关联工单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_name VARCHAR(64) DEFAULT NULL COMMENT '用户姓名',
    feedback_type TINYINT NOT NULL COMMENT '反馈类型 1-点赞 2-点踩',
    remark VARCHAR(500) DEFAULT NULL COMMENT '反馈备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_knowledge_id (knowledge_id),
    KEY idx_work_order_id (work_order_id),
    KEY idx_user_id (user_id),
    KEY idx_feedback_type (feedback_type),
    UNIQUE KEY uk_knowledge_user (knowledge_id, user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库反馈表';

-- =====================================================
-- 3. 新增知识库浏览/使用记录表
-- =====================================================
DROP TABLE IF EXISTS knowledge_usage_log;
CREATE TABLE knowledge_usage_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    knowledge_id BIGINT NOT NULL COMMENT '知识库ID',
    work_order_id BIGINT DEFAULT NULL COMMENT '关联工单ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    user_name VARCHAR(64) DEFAULT NULL COMMENT '用户姓名',
    usage_type TINYINT NOT NULL COMMENT '使用类型 1-浏览 2-引用到工单',
    source_type TINYINT DEFAULT 1 COMMENT '来源类型 1-PC管理端 2-uni-app移动端',
    confidence DECIMAL(5,4) DEFAULT NULL COMMENT '推荐置信度',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_knowledge_id (knowledge_id),
    KEY idx_work_order_id (work_order_id),
    KEY idx_user_id (user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库使用记录表';

-- =====================================================
-- 初始化示例数据 - 补充现有故障库的富文本内容和标签
-- =====================================================
UPDATE fault_library SET 
    solution_rich_text = '<p><strong>步骤1：检查电源</strong></p><p>确认逆变器电源指示灯正常，检查输入电压是否在正常范围内。</p><p><strong>步骤2：检查通讯线路</strong></p><p>检查网线或4G天线连接是否牢固，尝试更换通讯线缆。</p><p><strong>步骤3：重启设备</strong></p><p>断开逆变器电源5分钟后重新上电，观察通讯是否恢复。</p><p><strong>步骤4：更换通讯模块</strong></p><p>如以上步骤无效，可能是通讯模块损坏，需联系厂家更换。</p>',
    tags = '通讯,4G,网络,离线,重启',
    status = 1
WHERE fault_code = 'INV_NO_COMM';

UPDATE fault_library SET 
    solution_rich_text = '<p><strong>步骤1：检查通风系统</strong></p><p>检查机房空调是否正常运行，门窗通风是否良好。</p><p><strong>步骤2：清洁散热器</strong></p><p>使用压缩空气清洁逆变器散热片，去除灰尘和杂物。</p><p><strong>步骤3：检查散热风扇</strong></p><p>打开逆变器盖板，检查散热风扇是否正常运转，必要时更换。</p><p><strong>步骤4：降低负载</strong></p><p>临时降低逆变器运行功率，待温度下降后再逐步恢复。</p>',
    tags = '温度,散热,风扇,通风,过热',
    status = 1
WHERE fault_code = 'INV_OVER_TEMP';

UPDATE fault_library SET 
    solution_rich_text = '<p style="color:#ff4d4f"><strong>⚠️ 警告：过压故障存在电击风险，请专业人员操作！</strong></p><p><strong>步骤1：紧急停机</strong></p><p>立即按下急停按钮，断开直流侧和交流侧开关。</p><p><strong>步骤2：检查组件电压</strong></p><p>使用万用表逐串测量光伏组串开路电压，确认是否超出正常范围。</p><p><strong>步骤3：检查电网电压</strong></p><p>测量交流侧电网电压，确认是否在允许范围内。</p><p><strong>步骤4：联系技术支持</strong></p><p>如无法定位问题，请勿自行拆机，联系厂家技术支持。</p>',
    tags = '过压,电气,安全,直流,交流',
    status = 1
WHERE fault_code = 'INV_OVER_VOLT';

UPDATE fault_library SET 
    solution_rich_text = '<p style="color:#ff4d4f"><strong>⚠️ 警告：短路故障存在火灾风险，请立即断电！</strong></p><p><strong>步骤1：切断所有电源</strong></p><p>断开直流侧主开关和交流侧并网开关，确保设备完全断电。</p><p><strong>步骤2：检查直流侧</strong></p><p>使用绝缘表检查直流线缆对地绝缘电阻，逐段排查短路点。</p><p><strong>步骤3：检查组件</strong></p><p>检查光伏组件是否有破损、进水、接线盒烧毁等痕迹。</p><p><strong>步骤4：修复后测试</strong></p><p>修复短路点后，先进行绝缘测试，确认无问题后方可逐步恢复供电。</p>',
    tags = '短路,电气,火灾,绝缘,安全',
    status = 1
WHERE fault_code = 'INV_SHORT_CIRCUIT';

UPDATE fault_library SET 
    solution_rich_text = '<p><strong>步骤1：确认电网状态</strong></p><p>检查电网是否停电，联系电力公司确认供电情况。</p><p><strong>步骤2：检查开关状态</strong></p><p>检查交流侧断路器、漏电保护器是否跳闸。</p><p><strong>步骤3：等待自动恢复</strong></p><p>电网恢复后，逆变器通常会在5分钟内自动重启并网。</p><p><strong>步骤4：手动重启</strong></p><p>如长时间未恢复，可尝试断开逆变器电源30秒后重新上电。</p>',
    tags = '电网,失压,停电,并网,断路器',
    status = 1
WHERE fault_code = 'INV_GRID_OFF';

UPDATE fault_library SET 
    solution_rich_text = '<p><strong>步骤1：检查运行参数</strong></p><p>登录监控系统，查看逆变器输入输出电压、电流、温度等参数是否正常。</p><p><strong>步骤2：清洁散热系统</strong></p><p>清洁逆变器散热器，检查风扇运转，确保工作温度正常。</p><p><strong>步骤3：检查MPPT工作状态</strong></p><p>查看MPPT跟踪数据，确认是否在最佳工作点附近跟踪。</p><p><strong>步骤4：对比同型号设备</strong></p><p>与同型号、同容量的逆变器对比，确认是否存在明显差异。</p>',
    tags = '效率,PR,MPPT,性能,散热',
    status = 1
WHERE fault_code = 'INV_LOW_EFF';

UPDATE fault_library SET 
    solution_rich_text = '<p><strong>步骤1：检查接线</strong></p><p>检查组串正负极接线是否牢固，MC4插头是否有烧毁迹象。</p><p><strong>步骤2：检查遮挡情况</strong></p><p>检查组件是否被灰尘、落叶、阴影等遮挡。</p><p><strong>步骤3：检查保险丝</strong></p><p>检查汇流箱内对应组串的保险丝是否熔断。</p><p><strong>步骤4：测试组件</strong></p><p>使用万用表测量组串开路电压和短路电流，判断组件是否损坏。</p>',
    tags = '组串,组件,无输出,MC4,保险丝',
    status = 1
WHERE fault_code = 'STRING_NO_OUTPUT';
