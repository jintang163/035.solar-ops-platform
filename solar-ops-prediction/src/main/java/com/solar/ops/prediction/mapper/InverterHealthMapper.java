package com.solar.ops.prediction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.prediction.entity.InverterHealth;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InverterHealthMapper extends BaseMapper<InverterHealth> {
}
