package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spare_part_stocktake")
@ApiModel(value = "SparePartStocktake对象", description = "备件库存盘点主表")
public class SparePartStocktake extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "盘点单号")
    private String stocktakeNo;

    @ApiModelProperty(value = "盘点名称")
    private String stocktakeName;

    @ApiModelProperty(value = "盘点类型 1-全盘 2-抽盘 3-专项盘点")
    private Integer stocktakeType;

    @ApiModelProperty(value = "盘点仓库")
    private String warehouse;

    @ApiModelProperty(value = "盘点备件类型")
    private String partType;

    @ApiModelProperty(value = "状态 0-待盘点 1-盘点中 2-已完成 3-已取消")
    private Integer status;

    @ApiModelProperty(value = "盘点备件总数")
    private Integer totalCount;

    @ApiModelProperty(value = "差异数量")
    private Integer diffCount;

    @ApiModelProperty(value = "盘盈总数")
    private Integer profitQuantity;

    @ApiModelProperty(value = "盘亏总数")
    private Integer lossQuantity;

    @ApiModelProperty(value = "库存总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "差异总金额")
    private BigDecimal diffAmount;

    @ApiModelProperty(value = "盘点时间")
    private LocalDateTime stocktakeTime;

    @ApiModelProperty(value = "盘点人ID")
    private Long operatorId;

    @ApiModelProperty(value = "盘点人姓名")
    private String operatorName;

    @ApiModelProperty(value = "完成时间")
    private LocalDateTime completeTime;

    @ApiModelProperty(value = "备注")
    private String remark;
}
