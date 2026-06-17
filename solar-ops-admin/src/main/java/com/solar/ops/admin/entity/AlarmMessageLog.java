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
@TableName("alarm_message_log")
@ApiModel(value = "告警消息消费日志实体")
public class AlarmMessageLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "MQ主题")
    private String topic;

    @ApiModelProperty(value = "消费组")
    private String consumerGroup;

    @ApiModelProperty(value = "消息Key")
    private String messageKey;

    @ApiModelProperty(value = "消息内容JSON")
    private String messageContent;

    @ApiModelProperty(value = "消费状态：0-待消费 1-消费成功 2-消费失败 3-已重试")
    private Integer consumeStatus;

    @ApiModelProperty(value = "重试次数")
    private Integer retryCount;

    @ApiModelProperty(value = "失败原因")
    private String errorMsg;

    @ApiModelProperty(value = "消费完成时间")
    private LocalDateTime consumeTime;
}
