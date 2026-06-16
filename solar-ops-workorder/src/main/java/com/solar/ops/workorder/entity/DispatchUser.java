package com.solar.ops.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@ApiModel(value = "系统用户（调度用）")
public class DispatchUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "昵称")
    private String nickname;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "状态 0禁用 1启用")
    private Integer status;

    @ApiModelProperty(value = "角色/技能标签")
    private String role;

    @ApiModelProperty(value = "组织ID")
    private Long orgId;
}
