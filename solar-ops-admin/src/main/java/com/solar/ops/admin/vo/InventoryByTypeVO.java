package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "按类型统计库存VO")
public class InventoryByTypeVO {

    @ApiModelProperty(value = "备件类型")
    private String partType;

    @ApiModelProperty(value = "类型描述")
    private String partTypeDesc;

    @ApiModelProperty(value = "SKU数量")
    private Integer skuCount;

    @ApiModelProperty(value = "库存数量")
    private Integer quantity;

    @ApiModelProperty(value = "库存金额")
    private BigDecimal amount;
}
