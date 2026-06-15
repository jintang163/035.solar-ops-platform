package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "备件出入库记录查询DTO")
public class SparePartInOutRecordQueryDTO {

    @ApiModelProperty(value = "关键词(备件编号/名称/单号)")
    private String keyword;

    @ApiModelProperty(value = "记录类型 1-入库 2-出库")
    private Integer recordType;

    @ApiModelProperty(value = "出入库类型")
    private Integer inOutType;

    @ApiModelProperty(value = "备件ID")
    private Long partId;

    @ApiModelProperty(value = "工单ID")
    private Long workOrderId;

    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "操作人")
    private String operatorName;
}
