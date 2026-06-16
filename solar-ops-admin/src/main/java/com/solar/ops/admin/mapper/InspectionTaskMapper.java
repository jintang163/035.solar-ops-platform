package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.entity.InspectionTask;
import org.apache.ibatis.annotations.Param;

public interface InspectionTaskMapper extends BaseMapper<InspectionTask> {

    IPage<InspectionTask> selectTaskPage(Page<InspectionTask> page, @Param("assigneeId") Long assigneeId, @Param("status") Integer status, @Param("stationId") Long stationId);

    InspectionTask selectTaskDetailById(@Param("id") Long id);
}
