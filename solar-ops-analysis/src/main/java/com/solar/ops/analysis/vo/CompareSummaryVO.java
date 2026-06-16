package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel(value = "对比汇总VO", description = "电站对比分析汇总结果")
public class CompareSummaryVO {

    @ApiModelProperty(value = "PR表现最佳电站名称")
    private String bestStation;

    @ApiModelProperty(value = "PR表现最差电站名称")
    private String worstStation;

    @ApiModelProperty(value = "平均PR值")
    private BigDecimal avgPr;

    @ApiModelProperty(value = "PR差距（最佳与最差的差值）")
    private BigDecimal prGap;

    @ApiModelProperty(value = "优化建议列表")
    private List<String> recommendations;
}
