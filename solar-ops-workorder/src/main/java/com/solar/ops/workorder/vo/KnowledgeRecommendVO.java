package com.solar.ops.workorder.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "知识推荐结果VO")
public class KnowledgeRecommendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "知识库ID")
    private Long id;

    @ApiModelProperty(value = "故障码")
    private String faultCode;

    @ApiModelProperty(value = "故障名称")
    private String faultName;

    @ApiModelProperty(value = "故障级别 1-低级 2-中级 3-高级 4-紧急")
    private Integer faultLevel;

    @ApiModelProperty(value = "故障类型")
    private String faultType;

    @ApiModelProperty(value = "故障描述")
    private String faultDesc;

    @ApiModelProperty(value = "解决方案(纯文本摘要)")
    private String solution;

    @ApiModelProperty(value = "富文本解决方案")
    private String solutionRichText;

    @ApiModelProperty(value = "视频教程URL")
    private String videoUrl;

    @ApiModelProperty(value = "标签(逗号分隔)")
    private String tags;

    @ApiModelProperty(value = "推荐置信度 0-1")
    private BigDecimal confidence;

    @ApiModelProperty(value = "置信度等级：high-高 medium-中 low-低")
    private String confidenceLevel;

    @ApiModelProperty(value = "匹配原因说明")
    private String matchReason;

    @ApiModelProperty(value = "点赞数")
    private Integer likeCount;

    @ApiModelProperty(value = "点踩数")
    private Integer dislikeCount;

    @ApiModelProperty(value = "使用次数")
    private Integer useCount;

    @ApiModelProperty(value = "创建人姓名")
    private String creatorName;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
