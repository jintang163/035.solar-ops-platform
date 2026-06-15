package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_station")
@ApiModel(value = "SysUserStation对象", description = "用户电站权限关联")
public class SysUserStation {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称（冗余）")
    private String stationName;

    @ApiModelProperty(value = "权限类型 1-只读 2-读写 3-管理")
    private Integer permissionType;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
