package com.solar.ops.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order")
@ApiModel(value = "工单实体")
public class WorkOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工单编号")
    private String orderNo;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "故障码")
    private String faultCode;

    @ApiModelProperty(value = "故障名称")
    private String faultName;

    @ApiModelProperty(value = "故障级别 1-低级 2-中级 3-高级 4-紧急")
    private Integer faultLevel;

    @ApiModelProperty(value = "故障描述")
    private String description;

    @ApiModelProperty(value = "解决方案")
    private String solution;

    @ApiModelProperty(value = "工单状态 0-待接单 1-已接单 2-处理中 3-待验收 4-已完成 5-已关闭")
    private Integer status;

    @ApiModelProperty(value = "处理人ID")
    private Long handlerId;

    @ApiModelProperty(value = "处理人姓名")
    private String handlerName;

    @ApiModelProperty(value = "接单时间")
    private LocalDateTime acceptTime;

    @ApiModelProperty(value = "处理时间")
    private LocalDateTime processTime;

    @ApiModelProperty(value = "完成时间")
    private LocalDateTime completeTime;

    @ApiModelProperty(value = "预计完成时间")
    private LocalDateTime expectTime;
}
