package com.solar.ops.analysis.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "电站对比ExcelVO")
public class StationCompareExcelVO {

    @ExcelProperty("电站ID")
    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ExcelProperty("电站名称")
    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ExcelProperty("时期标签")
    @ApiModelProperty(value = "时期标签")
    private String periodLabel;

    @ExcelProperty("装机容量(kW)")
    @ApiModelProperty(value = "装机容量")
    private BigDecimal capacity;

    @ExcelProperty("PR值")
    @ApiModelProperty(value = "PR值")
    private BigDecimal prValue;

    @ExcelProperty("PR差异百分比(%)")
    @ApiModelProperty(value = "PR差异百分比")
    private BigDecimal prDiffPercent;

    @ExcelProperty("系统效率")
    @ApiModelProperty(value = "系统效率")
    private BigDecimal systemEfficiency;

    @ExcelProperty("系统效率差异百分比(%)")
    @ApiModelProperty(value = "系统效率差异百分比")
    private BigDecimal efficiencyDiffPercent;

    @ExcelProperty("等效利用小时数")
    @ApiModelProperty(value = "等效利用小时数")
    private BigDecimal equivalentHours;

    @ExcelProperty("等效小时差异百分比(%)")
    @ApiModelProperty(value = "等效小时差异百分比")
    private BigDecimal hoursDiffPercent;

    @ExcelProperty("总发电量(kWh)")
    @ApiModelProperty(value = "总发电量")
    private BigDecimal totalEnergy;

    @ExcelProperty("发电量差异百分比(%)")
    @ApiModelProperty(value = "发电量差异百分比")
    private BigDecimal energyDiffPercent;

    @ExcelProperty("故障率(%)")
    @ApiModelProperty(value = "故障率")
    private BigDecimal faultRate;

    @ExcelProperty("健康度评分")
    @ApiModelProperty(value = "健康度评分")
    private BigDecimal healthScore;

    @ExcelProperty("逆变器数量")
    @ApiModelProperty(value = "逆变器数量")
    private Integer inverterCount;

    @ExcelProperty("在线率(%)")
    @ApiModelProperty(value = "在线率")
    private BigDecimal onlineRate;

    @ExcelProperty("收益(元)")
    @ApiModelProperty(value = "收益")
    private BigDecimal revenue;

    @ExcelProperty("收益差异百分比(%)")
    @ApiModelProperty(value = "收益差异百分比")
    private BigDecimal revenueDiffPercent;
}
