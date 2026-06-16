package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.admin.entity.SysUserSkill;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserSkillMapper extends BaseMapper<SysUserSkill> {

    @Delete("DELETE FROM sys_user_skill WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
