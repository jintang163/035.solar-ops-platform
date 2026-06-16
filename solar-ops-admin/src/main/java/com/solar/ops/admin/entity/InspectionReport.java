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
@TableName("inspection_report")
@ApiModel(value = "InspectionReport对象", description = "巡检报告")
public class InspectionReport extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "报告编号")
    private String reportNo;

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "结果ID")
    private Long resultId;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "报告标题")
    private String reportTitle;

    @ApiModelProperty(value = "报告类型 1-日常巡检报告 2-专项巡检报告")
    private Integer reportType;

    @ApiModelProperty(value = "综合评分")
    private BigDecimal totalScore;

    @ApiModelProperty(value = "健康等级 1-优秀 2-良好 3-一般 4-较差")
    private Integer healthLevel;

    @ApiModelProperty(value = "总检查项")
    private Integer totalItems;

    @ApiModelProperty(value = "通过率(%)")
    private BigDecimal passRate;

    @ApiModelProperty(value = "异常项数量")
    private Integer abnormalCount;

    @ApiModelProperty(value = "问题汇总")
    private String problemSummary;

    @ApiModelProperty(value = "处理建议")
    private String suggestions;

    @ApiModelProperty(value = "报告内容(JSON格式)")
    private String reportContent;

    @ApiModelProperty(value = "生成时间")
    private LocalDateTime generatedTime;

    @ApiModelProperty(value = "生成人ID")
    private Long generatorId;

    @ApiModelProperty(value = "生成人姓名")
    private String generatorName;
}
