package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.admin.entity.InspectionTaskItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InspectionTaskItemMapper extends BaseMapper<InspectionTaskItem> {

    List<InspectionTaskItem> selectByTaskId(@Param("taskId") Long taskId);

    int batchInsert(@Param("list") List<InspectionTaskItem> list);
}
