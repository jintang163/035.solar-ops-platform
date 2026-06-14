package com.solar.ops.prediction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.prediction.entity.PowerPrediction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PowerPredictionMapper extends BaseMapper<PowerPrediction> {

    @Select("SELECT * FROM power_prediction WHERE station_id = #{stationId} " +
            "AND target_time >= #{startTime} AND target_time <= #{endTime} " +
            "ORDER BY target_time ASC")
    List<PowerPrediction> selectByStationAndTimeRange(
            @Param("stationId") Long stationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM power_prediction WHERE inverter_id = #{inverterId} " +
            "AND target_time >= #{startTime} AND target_time <= #{endTime} " +
            "ORDER BY target_time ASC")
    List<PowerPrediction> selectByInverterAndTimeRange(
            @Param("inverterId") Long inverterId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
