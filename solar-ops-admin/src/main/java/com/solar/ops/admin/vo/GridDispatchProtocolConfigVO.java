package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "调度协议配置VO")
public class GridDispatchProtocolConfigVO {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("协议类型：1-IEC104 2-Modbus TCP")
    private Integer protocolType;

    @ApiModelProperty("协议类型描述")
    private String protocolTypeDesc;

    @ApiModelProperty("配置名称")
    private String configName;

    @ApiModelProperty("调度主站IP")
    private String masterIp;

    @ApiModelProperty("调度主站端口")
    private Integer masterPort;

    @ApiModelProperty("本机IP")
    private String localIp;

    @ApiModelProperty("本机端口")
    private Integer localPort;

    @ApiModelProperty("公共地址/站地址")
    private Integer commonAddress;

    @ApiModelProperty("连接超时时间(秒)")
    private Integer connectTimeout;

    @ApiModelProperty("发送超时时间(秒)")
    private Integer sendTimeout;

    @ApiModelProperty("心跳间隔(秒)")
    private Integer heartbeatInterval;

    @ApiModelProperty("是否启用反向隔离")
    private Integer reverseIsolationEnabled;

    @ApiModelProperty("反向隔离描述")
    private String reverseIsolationDesc;

    @ApiModelProperty("反向隔离装置IP")
    private String isolationIp;

    @ApiModelProperty("反向隔离装置端口")
    private Integer isolationPort;

    @ApiModelProperty("上传间隔(秒)")
    private Integer uploadInterval;

    @ApiModelProperty("启用状态：0停用 1启用")
    private Integer enabled;

    @ApiModelProperty("启用状态描述")
    private String enabledDesc;

    @ApiModelProperty("连接状态：0未连接 1已连接 2异常")
    private Integer connectionStatus;

    @ApiModelProperty("连接状态描述")
    private String connectionStatusDesc;

    @ApiModelProperty("最后连接时间")
    private LocalDateTime lastConnectTime;

    @ApiModelProperty("最后断开时间")
    private LocalDateTime lastDisconnectTime;

    @ApiModelProperty("备注")
    private String remark;
}
