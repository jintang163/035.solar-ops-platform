package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "组织查询DTO")
public class OrgQueryDTO {

    @ApiModelProperty(value = "关键词")
    private String keyword;

    @ApiModelProperty(value = "组织类型")
    private String orgType;

    @ApiModelProperty(value = "状态")
    private Integer status;
}
