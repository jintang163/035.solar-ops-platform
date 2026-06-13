package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.dto.AssetQueryDTO;
import com.solar.ops.admin.entity.Asset;
import com.solar.ops.admin.vo.AssetDetailVO;
import org.apache.ibatis.annotations.Param;

public interface AssetMapper extends BaseMapper<Asset> {

    IPage<AssetDetailVO> selectAssetPage(Page<AssetDetailVO> page, @Param("query") AssetQueryDTO queryDTO);

    AssetDetailVO selectAssetDetailById(@Param("id") Long id);
}
