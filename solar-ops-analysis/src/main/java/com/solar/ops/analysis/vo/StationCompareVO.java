package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "电站对比结果VO", description = "电站对比分析完整结果")
public class StationCompareVO {

    @ApiModelProperty(value = "各电站/各时期指标列表")
    private List<StationCompareItemVO> stationMetrics;

    @ApiModelProperty(value = "对比汇总信息")
    private CompareSummaryVO compareSummary;
}
