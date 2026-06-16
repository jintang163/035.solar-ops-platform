package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.admin.entity.InspectionResultItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InspectionResultItemMapper extends BaseMapper<InspectionResultItem> {

    List<InspectionResultItem> selectByResultId(@Param("resultId") Long resultId);

    int batchInsert(@Param("list") List<InspectionResultItem> list);
}
