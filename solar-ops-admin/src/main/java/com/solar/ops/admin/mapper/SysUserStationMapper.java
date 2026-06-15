package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.admin.entity.SysUserStation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserStationMapper extends BaseMapper<SysUserStation> {

    @Select("SELECT station_id FROM sys_user_station WHERE user_id = #{userId}")
    List<Long> getStationIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_station WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Insert("<script>" +
            "INSERT INTO sys_user_station (user_id, station_id, station_name, permission_type, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.userId}, #{item.stationId}, #{item.stationName}, #{item.permissionType}, NOW())" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<SysUserStation> list);
}
