package com.solar.ops.admin.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "备件库存ExcelVO")
public class SparePartInventoryExcelVO {

    @ExcelProperty("备件编号")
    @ApiModelProperty(value = "备件编号")
    private String partCode;

    @ExcelProperty("备件名称")
    @ApiModelProperty(value = "备件名称")
    private String partName;

    @ExcelProperty("备件类型")
    @ApiModelProperty(value = "备件类型")
    private String partTypeDesc;

    @ExcelProperty("备件型号")
    @ApiModelProperty(value = "备件型号")
    private String partModel;

    @ExcelProperty("品牌")
    @ApiModelProperty(value = "品牌")
    private String brand;

    @ExcelProperty("规格参数")
    @ApiModelProperty(value = "规格参数")
    private String specification;

    @ExcelProperty("单位")
    @ApiModelProperty(value = "单位")
    private String unit;

    @ExcelProperty("单价(元)")
    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ExcelProperty("库存数量")
    @ApiModelProperty(value = "库存数量")
    private Integer quantity;

    @ExcelProperty("安全库存")
    @ApiModelProperty(value = "安全库存数量")
    private Integer safeQuantity;

    @ExcelProperty("库存金额(元)")
    @ApiModelProperty(value = "库存金额")
    private BigDecimal totalAmount;

    @ExcelProperty("存放位置")
    @ApiModelProperty(value = "存放位置")
    private String storageLocation;

    @ExcelProperty("所属仓库")
    @ApiModelProperty(value = "所属仓库")
    private String warehouse;

    @ExcelProperty("供应商")
    @ApiModelProperty(value = "供应商")
    private String supplier;

    @ExcelProperty("预警状态")
    @ApiModelProperty(value = "预警状态")
    private String warnStatusDesc;

    @ExcelProperty("备注")
    @ApiModelProperty(value = "备注")
    private String remark;
}
