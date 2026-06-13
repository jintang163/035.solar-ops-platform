package com.solar.ops.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.analysis.entity.StationHealth;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StationHealthMapper extends BaseMapper<StationHealth> {
}
