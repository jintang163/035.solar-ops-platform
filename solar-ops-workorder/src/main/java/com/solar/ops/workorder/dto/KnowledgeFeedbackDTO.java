package com.solar.ops.workorder.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(value = "知识库反馈DTO")
public class KnowledgeFeedbackDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "知识库ID", required = true)
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeId;

    @ApiModelProperty(value = "关联工单ID")
    private Long workOrderId;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "用户姓名")
    private String userName;

    @ApiModelProperty(value = "反馈类型 1-点赞 2-点踩", required = true)
    @NotNull(message = "反馈类型不能为空")
    private Integer feedbackType;

    @ApiModelProperty(value = "反馈备注")
    private String remark;
}
