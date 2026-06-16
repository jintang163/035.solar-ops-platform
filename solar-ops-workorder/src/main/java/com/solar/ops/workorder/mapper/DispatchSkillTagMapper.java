package com.solar.ops.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.workorder.entity.DispatchSkillTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DispatchSkillTagMapper extends BaseMapper<DispatchSkillTag> {

    @Select("SELECT t.tag_name FROM sys_skill_tag t " +
            "INNER JOIN sys_user_skill us ON t.id = us.tag_id " +
            "WHERE us.user_id = #{userId} AND t.deleted = 0 AND t.status = 1 " +
            "ORDER BY us.proficiency DESC, t.sort ASC")
    List<String> selectTagNamesByUserId(@Param("userId") Long userId);
}
