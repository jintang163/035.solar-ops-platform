package com.solar.ops.prediction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.prediction.entity.LifetimePrediction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LifetimePredictionMapper extends BaseMapper<LifetimePrediction> {
}
