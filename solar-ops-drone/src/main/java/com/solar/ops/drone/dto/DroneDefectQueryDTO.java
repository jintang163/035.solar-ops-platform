package com.solar.ops.drone.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "缺陷查询DTO")
public class DroneDefectQueryDTO {

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "图像ID")
    private Long imageId;

    @ApiModelProperty(value = "缺陷类型 hot_spot-热斑 microcrack-隐裂 shadow-遮挡")
    private String defectType;

    @ApiModelProperty(value = "缺陷等级 1-轻微 2-一般 3-严重 4-紧急")
    private Integer defectLevel;

    @ApiModelProperty(value = "状态 0-待处理 1-处理中 2-已修复 3-已忽略")
    private Integer status;

    @ApiModelProperty(value = "是否人工确认")
    private Integer verified;

    @ApiModelProperty(value = "当前页码", example = "1")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页条数", example = "10")
    private Integer pageSize = 10;
}
