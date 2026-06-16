package com.solar.ops.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_usage_log")
@ApiModel(value = "知识库使用记录实体")
public class KnowledgeUsageLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "知识库ID")
    private Long knowledgeId;

    @ApiModelProperty(value = "关联工单ID")
    private Long workOrderId;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "用户姓名")
    private String userName;

    @ApiModelProperty(value = "使用类型 1-浏览 2-引用到工单")
    private Integer usageType;

    @ApiModelProperty(value = "来源类型 1-PC管理端 2-uni-app移动端")
    private Integer sourceType;

    @ApiModelProperty(value = "推荐置信度")
    private BigDecimal confidence;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
