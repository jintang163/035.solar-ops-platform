package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spare_part")
@ApiModel(value = "SparePart对象", description = "备件更换记录")
public class SparePart extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "维修记录ID")
    private Long maintenanceRecordId;

    @ApiModelProperty(value = "资产ID")
    private Long assetId;

    @ApiModelProperty(value = "备件编号")
    private String partCode;

    @ApiModelProperty(value = "备件名称")
    private String partName;

    @ApiModelProperty(value = "备件型号")
    private String partModel;

    @ApiModelProperty(value = "品牌")
    private String brand;

    @ApiModelProperty(value = "规格")
    private String specification;

    @ApiModelProperty(value = "更换数量")
    private Integer quantity;

    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "总价")
    private BigDecimal totalPrice;

    @ApiModelProperty(value = "供应商")
    private String supplier;

    @ApiModelProperty(value = "更换时间")
    private LocalDateTime replaceTime;

    @ApiModelProperty(value = "操作人员")
    private String operator;

    @ApiModelProperty(value = "备注")
    private String remark;
}
