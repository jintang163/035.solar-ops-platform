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
@TableName("spare_part_inventory")
@ApiModel(value = "SparePartInventory对象", description = "备件库存表")
public class SparePartInventory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "备件编号")
    private String partCode;

    @ApiModelProperty(value = "备件名称")
    private String partName;

    @ApiModelProperty(value = "备件类型：fan-风扇 capacitor-电容 board-板卡 other-其他")
    private String partType;

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

    @ApiModelProperty(value = "存放位置")
    private String storageLocation;

    @ApiModelProperty(value = "所属仓库")
    private String warehouse;

    @ApiModelProperty(value = "供应商")
    private String supplier;

    @ApiModelProperty(value = "生产厂家")
    private String manufacturer;

    @ApiModelProperty(value = "状态 0-停用 1-启用")
    private Integer status;

    @ApiModelProperty(value = "预警状态 0-正常 1-低库存预警 2-库存不足")
    private Integer warnStatus;

    @ApiModelProperty(value = "所属电站ID")
    private Long stationId;

    @ApiModelProperty(value = "所属电站名称")
    private String stationName;

    @ApiModelProperty(value = "二维码图片地址")
    private String qrCodeUrl;

    @ApiModelProperty(value = "备注")
    private String remark;
}
