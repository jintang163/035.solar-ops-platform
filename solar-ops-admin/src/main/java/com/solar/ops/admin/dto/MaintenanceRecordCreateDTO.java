package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "维修记录创建DTO")
public class MaintenanceRecordCreateDTO {

    @ApiModelProperty(value = "资产ID", required = true)
    private Long assetId;

    @ApiModelProperty(value = "故障描述")
    private String faultDescription;

    @ApiModelProperty(value = "故障类型")
    private String faultType;

    @ApiModelProperty(value = "维修类型 1-日常维护 2-故障维修 3-定期巡检 4-备件更换", required = true)
    private Integer maintenanceType;

    @ApiModelProperty(value = "维修时间")
    private LocalDateTime maintenanceTime;

    @ApiModelProperty(value = "维修人员")
    private String maintenancePerson;

    @ApiModelProperty(value = "维修内容")
    private String maintenanceContent;

    @ApiModelProperty(value = "解决方案")
    private String solution;

    @ApiModelProperty(value = "维修照片(JSON数组)")
    private String photos;

    @ApiModelProperty(value = "维修费用")
    private BigDecimal cost;

    @ApiModelProperty(value = "关联工单ID")
    private Long workOrderId;

    @ApiModelProperty(value = "备注")
    private String remark;
}
