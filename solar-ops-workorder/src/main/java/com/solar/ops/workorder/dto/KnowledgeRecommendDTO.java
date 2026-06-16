package com.solar.ops.workorder.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "智能推荐请求DTO")
public class KnowledgeRecommendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "故障码")
    private String faultCode;

    @ApiModelProperty(value = "故障名称")
    private String faultName;

    @ApiModelProperty(value = "故障描述")
    private String description;

    @ApiModelProperty(value = "故障级别 1-低级 2-中级 3-高级 4-紧急")
    private Integer faultLevel;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "返回数量，默认5")
    private Integer topN = 5;

    @ApiModelProperty(value = "最低置信度阈值 0-1，默认0.3")
    private Double minConfidence = 0.3;
}
