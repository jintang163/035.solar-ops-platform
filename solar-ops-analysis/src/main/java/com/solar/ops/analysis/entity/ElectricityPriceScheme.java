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
@TableName("electricity_price_scheme")
@ApiModel(value = "电价方案实体")
public class ElectricityPriceScheme extends BaseEntity {

    @ApiModelProperty(value = "方案名称")
    private String schemeName;

    @ApiModelProperty(value = "电站ID（null则为全局默认方案）")
    private Long stationId;

    @ApiModelProperty(value = "上网电价（元/kWh）")
    private BigDecimal gridPrice;

    @ApiModelProperty(value = "脱硫煤基准电价（元/kWh）")
    private BigDecimal benchmarkPrice;

    @ApiModelProperty(value = "国家补贴电价（元/kWh）")
    private BigDecimal nationalSubsidy;

    @ApiModelProperty(value = "省级补贴电价（元/kWh）")
    private BigDecimal provincialSubsidy;

    @ApiModelProperty(value = "市级补贴电价（元/kWh）")
    private BigDecimal municipalSubsidy;

    @ApiModelProperty(value = "补贴开始日期")
    private LocalDate subsidyStartDate;

    @ApiModelProperty(value = "补贴结束日期（null则长期有效）")
    private LocalDate subsidyEndDate;

    @ApiModelProperty(value = "是否平价上网：0否 1是")
    private Integer isParity;

    @ApiModelProperty(value = "是否为默认方案：0否 1是")
    private Integer isDefault;

    @ApiModelProperty(value = "状态：0停用 1启用")
    private Integer status;

    @ApiModelProperty(value = "备注")
    private String remark;
}
