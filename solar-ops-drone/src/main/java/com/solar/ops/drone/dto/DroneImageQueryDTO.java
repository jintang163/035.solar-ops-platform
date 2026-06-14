package com.solar.ops.drone.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "巡检图像查询DTO")
public class DroneImageQueryDTO {

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "检测状态 0-待检测 1-检测中 2-检测完成 3-检测失败")
    private Integer detectStatus;

    @ApiModelProperty(value = "图像类型 visible-可见光 infrared-红外 thermal-热成像")
    private String imageType;

    @ApiModelProperty(value = "是否有缺陷")
    private Boolean hasDefect;

    @ApiModelProperty(value = "当前页码", example = "1")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页条数", example = "10")
    private Integer pageSize = 10;
}
