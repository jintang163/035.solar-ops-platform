package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.admin.entity.InspectionItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InspectionItemMapper extends BaseMapper<InspectionItem> {

    List<InspectionItem> selectByAssetType(@Param("assetType") String assetType);
}
