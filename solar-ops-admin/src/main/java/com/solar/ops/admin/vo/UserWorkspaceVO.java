package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(value = "用户工作空间信息")
public class UserWorkspaceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "昵称")
    private String nickname;

    @ApiModelProperty(value = "角色")
    private String role;

    @ApiModelProperty(value = "是否管理员")
    private Boolean isAdmin;

    @ApiModelProperty(value = "组织ID")
    private Long orgId;

    @ApiModelProperty(value = "组织名称")
    private String orgName;

    @ApiModelProperty(value = "数据权限范围")
    private String dataScope;

    @ApiModelProperty(value = "权限电站ID列表")
    private List<Long> stationIds;

    @ApiModelProperty(value = "权限电站列表")
    private List<StationTreeVO> stations;

    @ApiModelProperty(value = "当前选中的电站ID，null表示全部")
    private Long currentStationId;
}
