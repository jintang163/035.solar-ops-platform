package com.solar.ops.analysis.dto;

import com.solar.ops.common.page.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "电价方案查询DTO")
public class PriceSchemeQueryDTO extends PageQuery {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "状态：0停用 1启用")
    private Integer status;

    @ApiModelProperty(value = "关键字（方案名称）")
    private String keyword;
}
