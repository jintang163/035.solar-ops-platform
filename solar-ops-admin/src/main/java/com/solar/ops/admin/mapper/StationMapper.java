package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.admin.entity.Station;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StationMapper extends BaseMapper<Station> {

    @Select("<script>" +
            "SELECT id FROM station WHERE deleted = 0 " +
            "<if test='orgIds != null and orgIds.size > 0'>" +
            "AND org_id IN " +
            "<foreach collection='orgIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</if>" +
            "</script>")
    List<Long> getStationIdsByOrgIds(@Param("orgIds") List<Long> orgIds);
}
