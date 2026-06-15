package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "备件入库DTO")
public class SparePartInboundDTO {

    @ApiModelProperty(value = "备件ID", required = true)
    private Long partId;

    @ApiModelProperty(value = "入库类型 11-采购入库 12-盘盈入库 13-退库入库", required = true)
    private Integer inOutType;

    @ApiModelProperty(value = "入库数量", required = true)
    private Integer quantity;

    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "供应商")
    private String supplier;

    @ApiModelProperty(value = "批次号")
    private String batchNo;

    @ApiModelProperty(value = "操作人姓名")
    private String operatorName;

    @ApiModelProperty(value = "操作时间")
    private LocalDateTime operateTime;

    @ApiModelProperty(value = "备注")
    private String remark;
}
