package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "调度总览VO")
public class GridDispatchSummaryVO {

    @ApiModelProperty("今日指令总数")
    private Long totalCommandCount;

    @ApiModelProperty("执行成功数")
    private Long successCommandCount;

    @ApiModelProperty("执行失败数")
    private Long failCommandCount;

    @ApiModelProperty("待执行数")
    private Long pendingCommandCount;

    @ApiModelProperty("执行中数")
    private Long executingCommandCount;

    @ApiModelProperty("今日上传成功数")
    private Long successUploadCount;

    @ApiModelProperty("今日上传失败数")
    private Long failUploadCount;

    @ApiModelProperty("指令执行成功率(%)")
    private BigDecimal successRate;

    @ApiModelProperty("协议连接状态：0未连接 1已连接 2异常")
    private Integer protocolConnectionStatus;

    @ApiModelProperty("协议连接状态描述")
    private String protocolConnectionStatusDesc;

    @ApiModelProperty("最后上传时间")
    private String latestUploadTime;

    @ApiModelProperty("最后指令时间")
    private String latestCommandTime;

    @ApiModelProperty("已取消指令数")
    private Long cancelledCommandCount;
}
