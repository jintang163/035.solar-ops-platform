package com.solar.ops.admin.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@HeadRowHeight(20)
@ContentRowHeight(18)
@ColumnWidth(20)
public class AssetExcelVO {

    @ExcelProperty("资产编号")
    private String assetCode;

    @ExcelProperty("资产名称")
    private String assetName;

    @ExcelProperty("资产类型")
    private String assetTypeDesc;

    @ExcelProperty("所属电站")
    private String stationName;

    @ExcelProperty("设备序列号")
    private String deviceSn;

    @ExcelProperty("设备型号")
    private String deviceModel;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("规格参数")
    private String specification;

    @ExcelProperty("容量(kW)")
    private BigDecimal capacity;

    @ExcelProperty("安装日期")
    private LocalDate installDate;

    @ExcelProperty("质保到期日期")
    private LocalDate warrantyEndDate;

    @ExcelProperty("质保期限(月)")
    private Integer warrantyMonths;

    @ExcelProperty("供应商")
    private String supplier;

    @ExcelProperty("生产厂家")
    private String manufacturer;

    @ExcelProperty("安装位置")
    private String installLocation;

    @ExcelProperty("采购金额")
    private BigDecimal purchaseAmount;

    @ExcelProperty("责任人")
    private String responsiblePerson;

    @ExcelProperty("资产状态")
    private String assetStatusDesc;

    @ExcelProperty("备注")
    private String remark;
}
