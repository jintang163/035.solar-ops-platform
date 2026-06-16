package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.entity.InspectionResult;
import org.apache.ibatis.annotations.Param;

public interface InspectionResultMapper extends BaseMapper<InspectionResult> {

    IPage<InspectionResult> selectResultPage(Page<InspectionResult> page, @Param("stationId") Long stationId, @Param("inspectorId") Long inspectorId, @Param("resultStatus") Integer resultStatus);
}
