package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@ApiModel(value = "资产查询DTO")
public class AssetQueryDTO {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "资产类型")
    private String assetType;

    @ApiModelProperty(value = "资产状态")
    private Integer assetStatus;

    @ApiModelProperty(value = "关键词(资产名称/编号/型号)")
    private String keyword;

    @ApiModelProperty(value = "质保状态 1-正常 2-30天内到期 3-已过期")
    private Integer warrantyStatus;
}
