package com.solar.ops.workorder.dto;

import com.solar.ops.common.page.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "知识库查询DTO")
public class KnowledgeQueryDTO extends PageQuery {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "关键词(搜索故障码、名称、描述、标签)")
    private String keyword;

    @ApiModelProperty(value = "故障码")
    private String faultCode;

    @ApiModelProperty(value = "故障名称")
    private String faultName;

    @ApiModelProperty(value = "故障级别 1-低级 2-中级 3-高级 4-紧急")
    private Integer faultLevel;

    @ApiModelProperty(value = "故障类型")
    private String faultType;

    @ApiModelProperty(value = "标签")
    private String tag;

    @ApiModelProperty(value = "状态 0-草稿 1-已发布 2-已归档")
    private Integer status;

    @ApiModelProperty(value = "创建人ID")
    private Long creatorId;
}
