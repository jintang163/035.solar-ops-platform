package com.solar.ops.analysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dust_accumulation_record")
@ApiModel(value = "积灰检测记录实体")
public class DustAccumulationRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "逆变器名称/方阵编号")
    private String inverterName;

    @ApiModelProperty(value = "方阵编号（如：1号、2号、3号方阵）")
    private String arrayNumber;

    @ApiModelProperty(value = "检测日期")
    private LocalDate detectDate;

    @ApiModelProperty(value = "参考日期（清洗后基准日或7日前）")
    private LocalDate referenceDate;

    @ApiModelProperty(value = "实际发电量（kWh）")
    private BigDecimal actualEnergy;

    @ApiModelProperty(value = "理论发电量（kWh）")
    private BigDecimal theoreticalEnergy;

    @ApiModelProperty(value = "参考期实际/理论比值（基准）")
    private BigDecimal referenceRatio;

    @ApiModelProperty(value = "检测期实际/理论比值")
    private BigDecimal detectRatio;

    @ApiModelProperty(value = "发电量衰减率（0-1）")
    private BigDecimal attenuationRate;

    @ApiModelProperty(value = "预估损失发电量（kWh）")
    private BigDecimal estimatedLossEnergy;

    @ApiModelProperty(value = "积灰等级 0-无 1-轻度 2-中度 3-重度")
    private Integer dustLevel;

    @ApiModelProperty(value = "连续下降天数")
    private Integer continuousDeclineDays;

    @ApiModelProperty(value = "是否已生成清洗建议 0-否 1-是")
    private Integer hasReminder;

    @ApiModelProperty(value = "备注")
    private String remark;
}
