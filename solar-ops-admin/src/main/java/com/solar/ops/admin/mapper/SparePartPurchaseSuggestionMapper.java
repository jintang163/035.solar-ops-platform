package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.dto.PurchaseSuggestionQueryDTO;
import com.solar.ops.admin.entity.SparePartPurchaseSuggestion;
import com.solar.ops.admin.vo.SparePartPurchaseSuggestionVO;
import org.apache.ibatis.annotations.Param;

public interface SparePartPurchaseSuggestionMapper extends BaseMapper<SparePartPurchaseSuggestion> {

    Page<SparePartPurchaseSuggestionVO> selectSuggestionPage(Page<SparePartPurchaseSuggestionVO> page,
                                                              @Param("query") PurchaseSuggestionQueryDTO queryDTO);
}
