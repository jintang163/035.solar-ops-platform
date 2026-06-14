package com.solar.ops.drone.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

@Data
@ApiModel(value = "巡检统计VO")
public class DroneStatisticsVO {

    @ApiModelProperty(value = "总任务数")
    private Long totalTasks;

    @ApiModelProperty(value = "今日任务数")
    private Long todayTasks;

    @ApiModelProperty(value = "总图片数")
    private Long totalImages;

    @ApiModelProperty(value = "总缺陷数")
    private Long totalDefects;

    @ApiModelProperty(value = "待处理缺陷数")
    private Long pendingDefects;

    @ApiModelProperty(value = "缺陷类型分布")
    private Map<String, Long> defectTypeDistribution;

    @ApiModelProperty(value = "缺陷等级分布")
    private Map<String, Long> defectLevelDistribution;
}
