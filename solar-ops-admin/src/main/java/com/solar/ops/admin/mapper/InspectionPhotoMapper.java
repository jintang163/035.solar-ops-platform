package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.admin.entity.InspectionPhoto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InspectionPhotoMapper extends BaseMapper<InspectionPhoto> {

    List<InspectionPhoto> selectByResultId(@Param("resultId") Long resultId);

    List<InspectionPhoto> selectByResultItemId(@Param("resultItemId") Long resultItemId);

    int batchInsert(@Param("list") List<InspectionPhoto> list);
}
