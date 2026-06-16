package com.solar.ops.device.vo;

import com.solar.ops.device.dto.InverterDataDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 历史数据回放VO
 */
@Data
@ApiModel(value = "历史数据回放")
public class DataPlaybackVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "聚合后的历史数据列表")
    private List<InverterDataDTO> aggregatedData;

    @ApiModelProperty(value = "故障点列表")
    private List<FaultPointVO> faultPoints;

    @ApiModelProperty(value = "数据统计信息")
    private DataStatisticsVO statistics;

    @ApiModelProperty(value = "根因分析列表")
    private List<RootCauseVO> rootCauseAnalysis;
}
