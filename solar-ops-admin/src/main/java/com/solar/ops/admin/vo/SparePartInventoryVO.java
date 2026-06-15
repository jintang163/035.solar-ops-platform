package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "备件库存VO")
public class SparePartInventoryVO {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "备件编号")
    private String partCode;

    @ApiModelProperty(value = "备件名称")
    private String partName;

    @ApiModelProperty(value = "备件类型")
    private String partType;

    @ApiModelProperty(value = "备件类型描述")
    private String partTypeDesc;

    @ApiModelProperty(value = "备件型号")
    private String partModel;

    @ApiModelProperty(value = "品牌")
    private String brand;

    @ApiModelProperty(value = "规格参数")
    private String specification;

    @ApiModelProperty(value = "单位")
    private String unit;

    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "库存数量")
    private Integer quantity;

    @ApiModelProperty(value = "安全库存数量")
    private Integer safeQuantity;

    @ApiModelProperty(value = "最小采购数量")
    private Integer minPurchaseQuantity;

    @ApiModelProperty(value = "库存金额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "存放位置")
    private String storageLocation;

    @ApiModelProperty(value = "所属仓库")
    private String warehouse;

    @ApiModelProperty(value = "供应商")
    private String supplier;

    @ApiModelProperty(value = "生产厂家")
    private String manufacturer;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "状态描述")
    private String statusDesc;

    @ApiModelProperty(value = "预警状态")
    private Integer warnStatus;

    @ApiModelProperty(value = "预警状态描述")
    private String warnStatusDesc;

    @ApiModelProperty(value = "二维码图片地址")
    private String qrCodeUrl;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
