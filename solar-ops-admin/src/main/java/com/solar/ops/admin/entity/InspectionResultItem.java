package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inspection_result_item")
@ApiModel(value = "InspectionResultItem对象", description = "巡检结果明细")
public class InspectionResultItem {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "结果ID")
    private Long resultId;

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

    @ApiModelProperty(value = "检查位置经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "检查位置纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
