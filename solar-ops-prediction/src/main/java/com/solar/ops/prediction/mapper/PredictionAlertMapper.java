package com.solar.ops.prediction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.prediction.entity.PredictionAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PredictionAlertMapper extends BaseMapper<PredictionAlert> {

    @Select("SELECT * FROM prediction_alert WHERE station_id = #{stationId} " +
            "AND alert_time >= #{startTime} AND alert_time <= #{endTime} " +
            "ORDER BY alert_time DESC")
    List<PredictionAlert> selectByStationAndTimeRange(
            @Param("stationId") Long stationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM prediction_alert WHERE status = 0")
    int countPendingAlerts();
}
