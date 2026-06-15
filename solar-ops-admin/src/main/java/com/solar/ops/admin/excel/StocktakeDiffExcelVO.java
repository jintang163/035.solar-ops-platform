package com.solar.ops.admin.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "库存盘点差异ExcelVO")
public class StocktakeDiffExcelVO {

    @ExcelProperty("盘点单号")
    @ApiModelProperty(value = "盘点单号")
    private String stocktakeNo;

    @ExcelProperty("备件编号")
    @ApiModelProperty(value = "备件编号")
    private String partCode;

    @ExcelProperty("备件名称")
    @ApiModelProperty(value = "备件名称")
    private String partName;

    @ExcelProperty("备件型号")
    @ApiModelProperty(value = "备件型号")
    private String partModel;

    @ExcelProperty("备件类型")
    @ApiModelProperty(value = "备件类型")
    private String partTypeDesc;

    @ExcelProperty("单位")
    @ApiModelProperty(value = "单位")
    private String unit;

    @ExcelProperty("系统库存")
    @ApiModelProperty(value = "系统库存数量")
    private Integer systemQuantity;

    @ExcelProperty("实际盘点")
    @ApiModelProperty(value = "实际盘点数量")
    private Integer actualQuantity;

    @ExcelProperty("差异数量")
    @ApiModelProperty(value = "差异数量")
    private Integer diffQuantity;

    @ExcelProperty("差异类型")
    @ApiModelProperty(value = "差异类型")
    private String diffTypeDesc;

    @ExcelProperty("差异金额(元)")
    @ApiModelProperty(value = "差异金额")
    private BigDecimal diffAmount;

    @ExcelProperty("存放位置")
    @ApiModelProperty(value = "存放位置")
    private String storageLocation;

    @ExcelProperty("差异原因")
    @ApiModelProperty(value = "差异原因")
    private String remark;
}
