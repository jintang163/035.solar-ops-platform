package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
@ApiModel(value = "SysOrg对象", description = "组织架构")
public class SysOrg extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "组织编码")
    private String orgCode;

    @ApiModelProperty(value = "组织名称")
    private String orgName;

    @ApiModelProperty(value = "组织类型 1-集团总部 2-区域公司 3-电站")
    private Integer orgType;

    @ApiModelProperty(value = "父级组织ID")
    private Long parentId;

    @ApiModelProperty(value = "负责人ID")
    private Long leaderId;

    @ApiModelProperty(value = "负责人姓名")
    private String leaderName;

    @ApiModelProperty(value = "排序")
    private Integer sortOrder;

    @ApiModelProperty(value = "状态 0停用 1启用")
    private Integer status;

    @ApiModelProperty(value = "备注")
    private String remark;

    @TableField(exist = false)
    @ApiModelProperty(value = "子级组织列表")
    private List<SysOrg> children;
}
