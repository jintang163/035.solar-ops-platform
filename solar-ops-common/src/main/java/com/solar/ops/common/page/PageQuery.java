package com.solar.ops.common.page;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "分页查询参数")
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "页码", example = "1")
    private Long pageNum = 1L;

    @ApiModelProperty(value = "每页条数", example = "10")
    private Long pageSize = 10L;

    @ApiModelProperty(value = "排序字段")
    private String orderBy;

    @ApiModelProperty(value = "排序方式 asc/desc")
    private String orderType = "desc";
}
