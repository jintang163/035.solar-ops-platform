package com.solar.ops.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.workorder.entity.WorkOrderLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkOrderLogMapper extends BaseMapper<WorkOrderLog> {
}
