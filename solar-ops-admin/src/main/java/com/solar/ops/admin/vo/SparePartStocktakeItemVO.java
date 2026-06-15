package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "库存盘点明细VO")
public class SparePartStocktakeItemVO {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "盘点单ID")
    private Long stocktakeId;

    @ApiModelProperty(value = "盘点单号")
    private String stocktakeNo;

    @ApiModelProperty(value = "备件ID")
    private Long partId;

    @ApiModelProperty(value = "备件编号")
    private String partCode;

    @ApiModelProperty(value = "备件名称")
    private String partName;

    @ApiModelProperty(value = "备件型号")
    private String partModel;

    @ApiModelProperty(value = "备件类型")
    private String partType;

    @ApiModelProperty(value = "备件类型描述")
    private String partTypeDesc;

    @ApiModelProperty(value = "单位")
    private String unit;

    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "系统库存数量")
    private Integer systemQuantity;

    @ApiModelProperty(value = "实际盘点数量")
    private Integer actualQuantity;

    @ApiModelProperty(value = "差异数量（实际-系统）")
    private Integer diffQuantity;

    @ApiModelProperty(value = "差异类型 0-无差异 1-盘盈 2-盘亏")
    private Integer diffType;

    @ApiModelProperty(value = "差异类型描述")
    private String diffTypeDesc;

    @ApiModelProperty(value = "差异金额")
    private BigDecimal diffAmount;

    @ApiModelProperty(value = "存放位置")
    private String storageLocation;

    @ApiModelProperty(value = "差异原因")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
