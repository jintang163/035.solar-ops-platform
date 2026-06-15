package com.solar.ops.admin.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "出入库记录ExcelVO")
public class SparePartInOutRecordExcelVO {

    @ExcelProperty("出入库单号")
    @ApiModelProperty(value = "出入库单号")
    private String recordNo;

    @ExcelProperty("记录类型")
    @ApiModelProperty(value = "记录类型")
    private String recordTypeDesc;

    @ExcelProperty("出入库类型")
    @ApiModelProperty(value = "出入库类型")
    private String inOutTypeDesc;

    @ExcelProperty("备件编号")
    @ApiModelProperty(value = "备件编号")
    private String partCode;

    @ExcelProperty("备件名称")
    @ApiModelProperty(value = "备件名称")
    private String partName;

    @ExcelProperty("备件型号")
    @ApiModelProperty(value = "备件型号")
    private String partModel;

    @ExcelProperty("数量")
    @ApiModelProperty(value = "数量")
    private Integer quantity;

    @ExcelProperty("单价(元)")
    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ExcelProperty("总价(元)")
    @ApiModelProperty(value = "总价")
    private BigDecimal totalPrice;

    @ExcelProperty("变动前库存")
    @ApiModelProperty(value = "变动前库存")
    private Integer beforeQuantity;

    @ExcelProperty("变动后库存")
    @ApiModelProperty(value = "变动后库存")
    private Integer afterQuantity;

    @ExcelProperty("工单编号")
    @ApiModelProperty(value = "工单编号")
    private String workOrderNo;

    @ExcelProperty("操作人")
    @ApiModelProperty(value = "操作人姓名")
    private String operatorName;

    @ExcelProperty("操作时间")
    @ApiModelProperty(value = "操作时间")
    private LocalDateTime operateTime;

    @ExcelProperty("存放位置")
    @ApiModelProperty(value = "存放位置")
    private String storageLocation;

    @ExcelProperty("备注")
    @ApiModelProperty(value = "备注")
    private String remark;
}
