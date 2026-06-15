package com.solar.ops.analysis.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ApiModel(value = "电价方案对比DTO")
public class PriceSchemeCompareDTO {

    @ApiModelProperty(value = "电站ID", required = true)
    @NotNull(message = "电站ID不能为空")
    private Long stationId;

    @ApiModelProperty(value = "待对比的方案ID列表")
    private List<Long> schemeIds;
}
