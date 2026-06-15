package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.entity.SparePartStocktakeItem;
import com.solar.ops.admin.vo.SparePartStocktakeItemVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SparePartStocktakeItemMapper extends BaseMapper<SparePartStocktakeItem> {

    Page<SparePartStocktakeItemVO> selectItemPage(Page<SparePartStocktakeItemVO> page,
                                                   @Param("stocktakeId") Long stocktakeId,
                                                   @Param("diffType") Integer diffType);

    List<SparePartStocktakeItemVO> selectItemsByStocktakeId(@Param("stocktakeId") Long stocktakeId);
}
