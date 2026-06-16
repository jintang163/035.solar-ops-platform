package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_item")
@ApiModel(value = "InspectionItem对象", description = "巡检项")
public class InspectionItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "检查项编号")
    private String itemCode;

    @ApiModelProperty(value = "检查项名称")
    private String itemName;

    @ApiModelProperty(value = "检查项类型 1-外观检查 2-仪表读数 3-声音检查 4-红外测温 5-功能测试")
    private Integer itemType;

    @ApiModelProperty(value = "适用资产类型")
    private String assetType;

    @ApiModelProperty(value = "标准值")
    private String standardValue;

    @ApiModelProperty(value = "最小值阈值")
    private BigDecimal minValue;

    @ApiModelProperty(value = "最大值阈值")
    private BigDecimal maxValue;

    @ApiModelProperty(value = "单位")
    private String unit;

    @ApiModelProperty(value = "是否必填 0-否 1-是")
    private Integer isRequired;

    @ApiModelProperty(value = "排序号")
    private Integer sortOrder;

    @ApiModelProperty(value = "检查说明")
    private String description;
}
