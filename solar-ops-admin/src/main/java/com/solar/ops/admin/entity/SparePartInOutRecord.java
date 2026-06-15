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
@TableName("spare_part_in_out_record")
@ApiModel(value = "SparePartInOutRecord对象", description = "备件出入库记录表")
public class SparePartInOutRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "出入库单号")
    private String recordNo;

    @ApiModelProperty(value = "记录类型 1-入库 2-出库")
    private Integer recordType;

    @ApiModelProperty(value = "出入库类型 11-采购入库 12-盘盈入库 13-退库入库 21-工单出库 22-盘亏出库 23-报废出库")
    private Integer inOutType;

    @ApiModelProperty(value = "备件ID")
    private Long partId;

    @ApiModelProperty(value = "备件编号")
    private String partCode;

    @ApiModelProperty(value = "备件名称")
    private String partName;

    @ApiModelProperty(value = "备件型号")
    private String partModel;

    @ApiModelProperty(value = "数量")
    private Integer quantity;

    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "总价")
    private BigDecimal totalPrice;

    @ApiModelProperty(value = "变动前库存")
    private Integer beforeQuantity;

    @ApiModelProperty(value = "变动后库存")
    private Integer afterQuantity;

    @ApiModelProperty(value = "关联工单ID")
    private Long workOrderId;

    @ApiModelProperty(value = "工单编号")
    private String workOrderNo;

    @ApiModelProperty(value = "维修记录ID")
    private Long maintenanceRecordId;

    @ApiModelProperty(value = "资产ID")
    private Long assetId;

    @ApiModelProperty(value = "供应商")
    private String supplier;

    @ApiModelProperty(value = "操作人ID")
    private Long operatorId;

    @ApiModelProperty(value = "操作人姓名")
    private String operatorName;

    @ApiModelProperty(value = "操作时间")
    private LocalDateTime operateTime;

    @ApiModelProperty(value = "存放位置")
    private String storageLocation;

    @ApiModelProperty(value = "批次号")
    private String batchNo;

    @ApiModelProperty(value = "备注")
    private String remark;
}
