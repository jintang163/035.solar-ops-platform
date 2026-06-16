package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "任务检查项详情VO")
public class TaskItemWithDetailVO {

    @ApiModelProperty(value = "关联ID")
    private Long taskItemId;

    @ApiModelProperty(value = "检查项ID")
    private Long itemId;

    @ApiModelProperty(value = "检查项编号")
    private String itemCode;

    @ApiModelProperty(value = "检查项名称")
    private String itemName;

    @ApiModelProperty(value = "检查项类型 1-外观检查 2-仪表读数 3-声音检查 4-红外测温 5-功能测试")
    private Integer itemType;

    @ApiModelProperty(value = "资产ID")
    private Long assetId;

    @ApiModelProperty(value = "资产名称")
    private String assetName;

    @ApiModelProperty(value = "资产编号")
    private String assetCode;

    @ApiModelProperty(value = "标准值")
    private String standardValue;

    @ApiModelProperty(value = "最小值阈值")
    private BigDecimal minValue;

    @ApiModelProperty(value = "最大值阈值")
    private BigDecimal maxValue;

    @ApiModelProperty(value = "单位")
    private String unit;

    @ApiModelProperty(value = "是否必填")
    private Integer isRequired;

    @ApiModelProperty(value = "排序号")
    private Integer sortOrder;

    @ApiModelProperty(value = "检查说明")
    private String description;
}
