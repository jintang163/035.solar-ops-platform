package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspection_task_item")
@ApiModel(value = "InspectionTaskItem对象", description = "巡检任务-检查项关联")
public class InspectionTaskItem {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "检查项ID")
    private Long itemId;

    @ApiModelProperty(value = "关联资产ID")
    private Long assetId;

    @ApiModelProperty(value = "资产名称")
    private String assetName;

    @ApiModelProperty(value = "资产编号")
    private String assetCode;

    @ApiModelProperty(value = "排序号")
    private Integer sortOrder;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
