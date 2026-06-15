package com.solar.ops.analysis.vo;

import com.solar.ops.analysis.entity.RevenueStatistics;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "收益统计VO")
public class RevenueStatisticsVO extends RevenueStatistics {

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "方案名称")
    private String schemeName;

    @ApiModelProperty(value = "统计类型描述")
    private String statisticsTypeDesc;

    @ApiModelProperty(value = "结算状态描述")
    private String settlementStatusDesc;
}
