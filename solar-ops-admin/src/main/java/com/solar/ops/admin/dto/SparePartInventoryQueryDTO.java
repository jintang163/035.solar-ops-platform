package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel(value = "备件库存查询DTO")
public class SparePartInventoryQueryDTO {

    @ApiModelProperty(value = "备件编号/名称关键词")
    private String keyword;

    @ApiModelProperty(value = "备件类型")
    private String partType;

    @ApiModelProperty(value = "仓库")
    private String warehouse;

    @ApiModelProperty(value = "预警状态 0-正常 1-低库存预警 2-库存不足")
    private Integer warnStatus;

    @ApiModelProperty(value = "状态 0-停用 1-启用")
    private Integer status;

    @ApiModelProperty(value = "电站ID列表（数据权限过滤）", hidden = true)
    private List<Long> stationIds;
}
