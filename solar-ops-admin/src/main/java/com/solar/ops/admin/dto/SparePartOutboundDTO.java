package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "备件出库DTO")
public class SparePartOutboundDTO {

    @ApiModelProperty(value = "备件ID", required = true)
    private Long partId;

    @ApiModelProperty(value = "出库类型 21-工单出库 22-盘亏出库 23-报废出库", required = true)
    private Integer inOutType;

    @ApiModelProperty(value = "出库数量", required = true)
    private Integer quantity;

    @ApiModelProperty(value = "工单ID")
    private Long workOrderId;

    @ApiModelProperty(value = "工单编号")
    private String workOrderNo;

    @ApiModelProperty(value = "维修记录ID")
    private Long maintenanceRecordId;

    @ApiModelProperty(value = "资产ID")
    private Long assetId;

    @ApiModelProperty(value = "操作人姓名")
    private String operatorName;

    @ApiModelProperty(value = "操作时间")
    private LocalDateTime operateTime;

    @ApiModelProperty(value = "备注")
    private String remark;
}
