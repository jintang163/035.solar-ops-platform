package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.dto.StocktakeQueryDTO;
import com.solar.ops.admin.entity.SparePartStocktake;
import com.solar.ops.admin.vo.SparePartStocktakeVO;
import org.apache.ibatis.annotations.Param;

public interface SparePartStocktakeMapper extends BaseMapper<SparePartStocktake> {

    Page<SparePartStocktakeVO> selectStocktakePage(Page<SparePartStocktakeVO> page,
                                                    @Param("query") StocktakeQueryDTO queryDTO);

    SparePartStocktakeVO selectStocktakeDetailById(@Param("id") Long id);
}
