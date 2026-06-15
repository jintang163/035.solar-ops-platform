package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "创建盘点单DTO")
public class StocktakeCreateDTO {

    @ApiModelProperty(value = "盘点名称", required = true)
    private String stocktakeName;

    @ApiModelProperty(value = "盘点类型 1-全盘 2-抽盘 3-专项盘点", required = true)
    private Integer stocktakeType;

    @ApiModelProperty(value = "盘点仓库")
    private String warehouse;

    @ApiModelProperty(value = "盘点备件类型")
    private String partType;

    @ApiModelProperty(value = "盘点人姓名")
    private String operatorName;

    @ApiModelProperty(value = "备注")
    private String remark;
}
