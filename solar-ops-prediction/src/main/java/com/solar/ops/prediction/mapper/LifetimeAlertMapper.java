package com.solar.ops.prediction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.prediction.entity.LifetimeAlert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LifetimeAlertMapper extends BaseMapper<LifetimeAlert> {
}
