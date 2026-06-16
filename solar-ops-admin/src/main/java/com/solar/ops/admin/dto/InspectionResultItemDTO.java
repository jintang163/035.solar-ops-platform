package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "巡检结果明细DTO")
public class InspectionResultItemDTO {

    @ApiModelProperty(value = "任务检查项ID")
    private Long taskItemId;

    @ApiModelProperty(value = "检查项ID")
    private Long itemId;

    @ApiModelProperty(value = "检查项名称")
    private String itemName;

    @ApiModelProperty(value = "检查项类型")
    private Integer itemType;

    @ApiModelProperty(value = "资产ID")
    private Long assetId;

    @ApiModelProperty(value = "资产名称")
    private String assetName;

    @ApiModelProperty(value = "资产编号")
    private String assetCode;

    @ApiModelProperty(value = "检查值")
    private String checkValue;

    @ApiModelProperty(value = "标准值")
    private String standardValue;

    @ApiModelProperty(value = "是否正常 0-异常 1-正常")
    private Integer isNormal;

    @ApiModelProperty(value = "异常描述")
    private String abnormalDesc;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "检查时间")
    private LocalDateTime checkTime;

    @ApiModelProperty(value = "经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private BigDecimal latitude;
}
