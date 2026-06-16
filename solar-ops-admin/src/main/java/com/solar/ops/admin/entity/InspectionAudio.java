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
@TableName("inspection_audio")
@ApiModel(value = "InspectionAudio对象", description = "巡检录音")
public class InspectionAudio extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "录音编号")
    private String audioNo;

    @ApiModelProperty(value = "结果ID")
    private Long resultId;

    @ApiModelProperty(value = "结果明细ID")
    private Long resultItemId;

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "资产ID")
    private Long assetId;

    @ApiModelProperty(value = "录音URL")
    private String audioUrl;

    @ApiModelProperty(value = "文件大小(字节)")
    private Long fileSize;

    @ApiModelProperty(value = "录音时长(秒)")
    private Integer duration;

    @ApiModelProperty(value = "录制时间")
    private LocalDateTime recordTime;

    @ApiModelProperty(value = "录制位置经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "录制位置纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "是否离线录制 0-否 1-是")
    private Integer isOffline;

    @ApiModelProperty(value = "同步状态 0-待同步 1-已同步 2-同步失败")
    private Integer syncStatus;
}
