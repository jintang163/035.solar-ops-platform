package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(value = "工单领用备件DTO")
public class WorkOrderSparePartOutboundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工单ID", required = true)
    private Long workOrderId;

    @ApiModelProperty(value = "工单编号")
    private String workOrderNo;

    @ApiModelProperty(value = "资产ID", required = true)
    private Long assetId;

    @ApiModelProperty(value = "维修类型 1-日常维护 2-故障维修 3-定期巡检 4-备件更换")
    private Integer maintenanceType;

    @ApiModelProperty(value = "故障描述")
    private String faultDescription;

    @ApiModelProperty(value = "故障类型")
    private String faultType;

    @ApiModelProperty(value = "维修内容")
    private String maintenanceContent;

    @ApiModelProperty(value = "维修时间")
    private LocalDateTime maintenanceTime;

    @ApiModelProperty(value = "维修人员")
    private String maintenancePerson;

    @ApiModelProperty(value = "解决方案")
    private String solution;

    @ApiModelProperty(value = "维修照片(JSON数组)")
    private String photos;

    @ApiModelProperty(value = "维修费用")
    private BigDecimal cost;

    @ApiModelProperty(value = "操作人姓名")
    private String operatorName;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "领用备件列表", required = true)
    private List<SparePartItem> spareParts;

    @Data
    @ApiModel(value = "领用备件项")
    public static class SparePartItem implements Serializable {

        @ApiModelProperty(value = "备件库存ID", required = true)
        private Long partId;

        @ApiModelProperty(value = "备件编号")
        private String partCode;

        @ApiModelProperty(value = "备件名称")
        private String partName;

        @ApiModelProperty(value = "备件型号")
        private String partModel;

        @ApiModelProperty(value = "品牌")
        private String brand;

        @ApiModelProperty(value = "规格")
        private String specification;

        @ApiModelProperty(value = "领用数量", required = true)
        private Integer quantity;

        @ApiModelProperty(value = "单价")
        private BigDecimal unitPrice;

        @ApiModelProperty(value = "供应商")
        private String supplier;
    }
}
