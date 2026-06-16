package com.solar.ops.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.workorder.entity.OperatorLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface OperatorLocationMapper extends BaseMapper<OperatorLocation> {

    @Select("SELECT * FROM operator_location WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT 1")
    OperatorLocation getLatestByUserId(@Param("userId") Long userId);

    List<OperatorLocation> selectWithinRadius(@Param("longitude") BigDecimal longitude,
                                               @Param("latitude") BigDecimal latitude,
                                               @Param("radiusKm") Double radiusKm);
}
