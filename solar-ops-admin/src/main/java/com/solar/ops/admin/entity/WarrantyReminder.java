package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warranty_reminder")
@ApiModel(value = "WarrantyReminder对象", description = "质保提醒")
public class WarrantyReminder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "资产ID")
    private Long assetId;

    @ApiModelProperty(value = "资产编号")
    private String assetCode;

    @ApiModelProperty(value = "资产名称")
    private String assetName;

    @ApiModelProperty(value = "质保到期日期")
    private LocalDate warrantyEndDate;

    @ApiModelProperty(value = "剩余天数")
    private Integer daysLeft;

    @ApiModelProperty(value = "提醒类型")
    private Integer reminderType;

    @ApiModelProperty(value = "提醒状态")
    private Integer reminderStatus;

    @ApiModelProperty(value = "提醒时间")
    private LocalDateTime reminderTime;

    @ApiModelProperty(value = "接收人(逗号分隔)")
    private String receivers;
}
