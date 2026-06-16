package com.solar.ops.device.vo;

import com.solar.ops.device.dto.InverterDataDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 故障点信息VO
 */
@Data
@ApiModel(value = "故障点信息")
public class FaultPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "时间戳（毫秒）")
    private Long timestamp;

    @ApiModelProperty(value = "故障码")
    private Integer faultCode;

    @ApiModelProperty(value = "故障描述")
    private String faultDesc;

    @ApiModelProperty(value = "故障对应的数据点")
    private InverterDataDTO dataPoint;
}
