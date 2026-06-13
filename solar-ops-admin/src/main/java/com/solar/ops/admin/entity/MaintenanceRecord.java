package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("maintenance_record")
@ApiModel(value = "MaintenanceRecord对象", description = "设备维修记录")
public class MaintenanceRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "资产ID")
    private Long assetId;

    @ApiModelProperty(value = "维修记录编号")
    private String recordNo;

    @ApiModelProperty(value = "故障描述")
    private String faultDescription;

    @ApiModelProperty(value = "故障类型")
    private String faultType;

    @ApiModelProperty(value = "维修类型")
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

    @TableField(exist = false)
    @ApiModelProperty(value = "备件列表")
    private List<SparePart> spareParts;
}
