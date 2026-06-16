package com.solar.ops.workorder.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel(value = "运维人员推荐VO")
public class OperatorRecommendVO {

    @ApiModelProperty(value = "运维人员ID")
    private Long userId;

    @ApiModelProperty(value = "运维人员姓名")
    private String userName;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "距离(公里)")
    private BigDecimal distanceKm;

    @ApiModelProperty(value = "技能标签")
    private List<String> skillTags;

    @ApiModelProperty(value = "技能匹配度(0-100)")
    private Integer skillMatchScore;

    @ApiModelProperty(value = "当前进行中任务数")
    private Integer activeTaskCount;

    @ApiModelProperty(value = "任务负载评分(0-100，分数越低越空闲)")
    private Integer workloadScore;

    @ApiModelProperty(value = "综合推荐评分(0-100，越高越优)")
    private Integer totalScore;

    @ApiModelProperty(value = "推荐等级")
    private String recommendLevel;

    @ApiModelProperty(value = "预计到达时间(分钟)")
    private Integer etaMinutes;

    @ApiModelProperty(value = "最后上报时间")
    private String lastReportTime;
}
