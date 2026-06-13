package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset")
@ApiModel(value = "Asset对象", description = "资产台账")
public class Asset extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "资产编号")
    private String assetCode;

    @ApiModelProperty(value = "资产名称")
    private String assetName;

    @ApiModelProperty(value = "资产类型")
    private String assetType;

    @ApiModelProperty(value = "所属电站ID")
    private Long stationId;

    @ApiModelProperty(value = "设备序列号")
    private String deviceSn;

    @ApiModelProperty(value = "设备型号")
    private String deviceModel;

    @ApiModelProperty(value = "品牌")
    private String brand;

    @ApiModelProperty(value = "规格参数")
    private String specification;

    @ApiModelProperty(value = "容量(kW)")
    private BigDecimal capacity;

    @ApiModelProperty(value = "安装日期")
    private LocalDate installDate;

    @ApiModelProperty(value = "质保开始日期")
    private LocalDate warrantyStartDate;

    @ApiModelProperty(value = "质保到期日期")
    private LocalDate warrantyEndDate;

    @ApiModelProperty(value = "质保期限(月)")
    private Integer warrantyMonths;

    @ApiModelProperty(value = "供应商")
    private String supplier;

    @ApiModelProperty(value = "生产厂家")
    private String manufacturer;

    @ApiModelProperty(value = "安装位置")
    private String installLocation;

    @ApiModelProperty(value = "经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "采购金额")
    private BigDecimal purchaseAmount;

    @ApiModelProperty(value = "责任人")
    private String responsiblePerson;

    @ApiModelProperty(value = "资产状态")
    private Integer assetStatus;

    @ApiModelProperty(value = "二维码图片地址")
    private String qrCodeUrl;

    @ApiModelProperty(value = "备注")
    private String remark;
}
