package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "语音音箱设备VO")
public class VoiceSpeakerDeviceVO {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "音箱设备ID（MAC或SN）")
    private String deviceId;

    @ApiModelProperty(value = "设备名称")
    private String deviceName;

    @ApiModelProperty(value = "位置/所属运维中心")
    private String location;

    @ApiModelProperty(value = "设备类型")
    private String deviceType;

    @ApiModelProperty(value = "IP地址")
    private String ipAddress;

    @ApiModelProperty(value = "在线状态：0离线 1在线")
    private Boolean online;

    @ApiModelProperty(value = "音量设置 0-100")
    private Integer volume;

    @ApiModelProperty(value = "最后心跳时间")
    private LocalDateTime lastHeartbeatTime;

    @ApiModelProperty(value = "备注")
    private String description;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
