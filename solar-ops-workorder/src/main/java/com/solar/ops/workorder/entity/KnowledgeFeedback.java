package com.solar.ops.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_feedback")
@ApiModel(value = "知识库反馈实体")
public class KnowledgeFeedback extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "知识库ID")
    private Long knowledgeId;

    @ApiModelProperty(value = "关联工单ID")
    private Long workOrderId;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "用户姓名")
    private String userName;

    @ApiModelProperty(value = "反馈类型 1-点赞 2-点踩")
    private Integer feedbackType;

    @ApiModelProperty(value = "反馈备注")
    private String remark;
}
