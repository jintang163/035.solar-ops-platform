package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.entity.InspectionReport;
import org.apache.ibatis.annotations.Param;

public interface InspectionReportMapper extends BaseMapper<InspectionReport> {

    IPage<InspectionReport> selectReportPage(Page<InspectionReport> page, @Param("stationId") Long stationId, @Param("reportType") Integer reportType);

    InspectionReport selectByResultId(@Param("resultId") Long resultId);
}
