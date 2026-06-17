package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("voice_broadcast_config")
@ApiModel(value = "语音播报配置实体")
public class VoiceBroadcastConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "配置键")
    private String configKey;

    @ApiModelProperty(value = "是否启用")
    private Boolean enabled;

    @ApiModelProperty(value = "最小告警级别")
    private Integer minAlarmLevel;

    @ApiModelProperty(value = "启用播报类型，逗号分隔")
    private String enabledTypes;

    @ApiModelProperty(value = "音量 0-100")
    private Integer volume;

    @ApiModelProperty(value = "语速 0-100")
    private Integer speed;

    @ApiModelProperty(value = "发音人")
    private String voiceName;

    @ApiModelProperty(value = "播报开始时间")
    private String broadcastStartTime;

    @ApiModelProperty(value = "播报结束时间")
    private String broadcastEndTime;

    @ApiModelProperty(value = "夜间是否播报")
    private Boolean nightBroadcast;

    @ApiModelProperty(value = "TTS服务商")
    private String ttsProvider;

    @ApiModelProperty(value = "音箱API地址")
    private String speakerApiUrl;

    @ApiModelProperty(value = "音箱API密钥")
    private String speakerApiKey;
}
