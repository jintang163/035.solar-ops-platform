package com.solar.ops.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.workorder.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {
}
