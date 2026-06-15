package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "清洗提升趋势VO")
public class CleaningTrendVO {

    @ApiModelProperty(value = "日期")
    private LocalDate date;

    @ApiModelProperty(value = "清洗次数")
    private Integer cleaningCount;

    @ApiModelProperty(value = "提升发电量（kWh）")
    private BigDecimal improvedEnergy;
}
