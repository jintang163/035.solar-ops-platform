package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("station")
@ApiModel(value = "Station对象", description = "电站")
public class Station extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "电站编号")
    private String stationCode;

    @ApiModelProperty(value = "装机容量(kW)")
    private BigDecimal capacity;

    @ApiModelProperty(value = "地址")
    private String address;

    @ApiModelProperty(value = "经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "联系人")
    private String contact;

    @ApiModelProperty(value = "联系电话")
    private String contactPhone;

    @ApiModelProperty(value = "状态 0停用 1启用")
    private Integer status;

    @ApiModelProperty(value = "总投资额（元）")
    private BigDecimal totalInvestment;

    @ApiModelProperty(value = "年运维成本（元）")
    private BigDecimal annualOperationCost;

    @ApiModelProperty(value = "设计寿命（年）")
    private Integer designLife;

    @ApiModelProperty(value = "峰值日照小时数")
    private BigDecimal peakSunHours;

    @ApiModelProperty(value = "组织ID")
    private Long orgId;
}
