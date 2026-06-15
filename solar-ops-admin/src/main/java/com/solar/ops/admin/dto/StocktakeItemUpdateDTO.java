package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "更新盘点明细DTO")
public class StocktakeItemUpdateDTO {

    @ApiModelProperty(value = "明细ID", required = true)
    private Long itemId;

    @ApiModelProperty(value = "实际盘点数量", required = true)
    private Integer actualQuantity;

    @ApiModelProperty(value = "差异原因/备注")
    private String remark;
}
