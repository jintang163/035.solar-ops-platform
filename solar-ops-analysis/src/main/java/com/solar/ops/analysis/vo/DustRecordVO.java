package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@ApiModel(value = "积灰记录VO")
public class DustRecordVO {

    @ApiModelProperty(value = "ID")
    private Long id;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "逆变器名称/方阵编号")
    private String inverterName;

    @ApiModelProperty(value = "方阵编号")
    private String arrayNumber;

    @ApiModelProperty(value = "检测日期")
    private LocalDate detectDate;

    @ApiModelProperty(value = "发电量衰减率（0-1）")
    private BigDecimal attenuationRate;

    @ApiModelProperty(value = "衰减率百分比（用于展示）")
    private BigDecimal attenuationRatePercent;

    @ApiModelProperty(value = "预估损失发电量（kWh）")
    private BigDecimal estimatedLossEnergy;

    @ApiModelProperty(value = "积灰等级 0-无 1-轻度 2-中度 3-重度")
    private Integer dustLevel;

    @ApiModelProperty(value = "积灰等级描述")
    private String dustLevelDesc;

    @ApiModelProperty(value = "积灰等级颜色")
    private String dustLevelColor;

    @ApiModelProperty(value = "连续下降天数")
    private Integer continuousDeclineDays;

    @ApiModelProperty(value = "是否已生成清洗建议 0-否 1-是")
    private Integer hasReminder;

    @ApiModelProperty(value = "实际发电量（kWh）")
    private BigDecimal actualEnergy;

    @ApiModelProperty(value = "理论发电量（kWh）")
    private BigDecimal theoreticalEnergy;
}
