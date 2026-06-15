package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "库存盘点查询DTO")
public class StocktakeQueryDTO {

    @ApiModelProperty(value = "关键词(盘点单号/名称)")
    private String keyword;

    @ApiModelProperty(value = "盘点类型 1-全盘 2-抽盘 3-专项盘点")
    private Integer stocktakeType;

    @ApiModelProperty(value = "状态 0-待盘点 1-盘点中 2-已完成 3-已取消")
    private Integer status;

    @ApiModelProperty(value = "仓库")
    private String warehouse;

    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endTime;
}
