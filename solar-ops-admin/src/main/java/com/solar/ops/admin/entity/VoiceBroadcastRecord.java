package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("voice_broadcast_record")
@ApiModel(value = "语音播报记录实体")
public class VoiceBroadcastRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "播报类型：1-逆变器离线 2-火灾预警 3-高级告警 4-紧急告警 5-设备异常")
    private Integer broadcastType;

    @ApiModelProperty(value = "告警级别：1-低级 2-中级 3-高级 4-紧急")
    private Integer alarmLevel;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "逆变器名称")
    private String inverterName;

    @ApiModelProperty(value = "故障码")
    private String faultCode;

    @ApiModelProperty(value = "播报内容文本")
    private String broadcastContent;

    @ApiModelProperty(value = "语音文件URL")
    private String audioUrl;

    @ApiModelProperty(value = "播报状态：0-待播报 1-已播报 2-播报失败")
    private Integer status;

    @ApiModelProperty(value = "播报时间")
    private LocalDateTime broadcastTime;

    @ApiModelProperty(value = "关联工单ID")
    private Long workOrderId;
}
