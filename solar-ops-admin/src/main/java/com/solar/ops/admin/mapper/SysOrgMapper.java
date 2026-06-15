package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.admin.entity.SysOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {

    @Select("SELECT id FROM sys_org WHERE parent_id = #{parentId} AND deleted = 0")
    List<Long> getChildOrgIds(@Param("parentId") Long parentId);

    @Select("<script>" +
            "SELECT id FROM sys_org WHERE deleted = 0 " +
            "<if test='orgIds != null and orgIds.size > 0'>" +
            "AND parent_id IN " +
            "<foreach collection='orgIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</if>" +
            "</script>")
    List<Long> getChildOrgIdsBatch(@Param("orgIds") List<Long> orgIds);

    @Select("SELECT id FROM station WHERE org_id IN " +
            "<foreach collection='orgIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND deleted = 0")
    List<Long> getStationIdsByOrgIds(@Param("orgIds") List<Long> orgIds);
}
