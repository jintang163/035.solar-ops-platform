package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel(value = "库存仪表盘VO")
public class InventoryDashboardVO {

    @ApiModelProperty(value = "备件总数")
    private Integer totalSkuCount;

    @ApiModelProperty(value = "库存总数量")
    private Integer totalQuantity;

    @ApiModelProperty(value = "库存总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "低库存预警数量")
    private Integer lowWarnCount;

    @ApiModelProperty(value = "库存不足数量")
    private Integer insufficientCount;

    @ApiModelProperty(value = "待处理采购建议数量")
    private Integer pendingSuggestionCount;

    @ApiModelProperty(value = "今日入库数量")
    private Integer todayInboundCount;

    @ApiModelProperty(value = "今日出库数量")
    private Integer todayOutboundCount;

    @ApiModelProperty(value = "各类型库存数量")
    private List<InventoryByTypeVO> typeStats;

    @ApiModelProperty(value = "预警备件列表（TOP 10）")
    private List<SparePartInventoryVO> warnParts;
}
