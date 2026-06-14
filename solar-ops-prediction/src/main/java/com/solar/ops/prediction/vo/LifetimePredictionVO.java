package com.solar.ops.prediction.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(value = "寿命预测结果VO")
public class LifetimePredictionVO {

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "逆变器名称")
    private String inverterName;

    @ApiModelProperty(value = "预测时间")
    private LocalDateTime predictionTime;

    @ApiModelProperty(value = "当前健康度评分(0-1)")
    private BigDecimal currentHealthScore;

    @ApiModelProperty(value = "健康度等级描述")
    private String healthLevelDesc;

    @ApiModelProperty(value = "健康度颜色")
    private String healthColor;

    @ApiModelProperty(value = "预测剩余寿命(天)")
    private Integer remainingLifeDays;

    @ApiModelProperty(value = "剩余寿命描述(如: 约1.5年)")
    private String remainingLifeDesc;

    @ApiModelProperty(value = "预测未来天数")
    private Integer forecastDays;

    @ApiModelProperty(value = "健康度趋势数据")
    private List<BigDecimal> healthTrend;

    @ApiModelProperty(value = "置信度趋势数据")
    private List<BigDecimal> confidenceTrend;

    @ApiModelProperty(value = "时间轴标签")
    private List<String> timeAxis;

    @ApiModelProperty(value = "模型版本")
    private String modelVersion;

    @ApiModelProperty(value = "是否需要更换备件")
    private Boolean replacementAdvice;

    @ApiModelProperty(value = "预警级别：1-正常 2-注意 3-警告 4-紧急")
    private Integer alertLevel;

    @ApiModelProperty(value = "预警级别描述")
    private String alertLevelDesc;
}
