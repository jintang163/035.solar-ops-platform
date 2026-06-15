package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.dto.SparePartInventoryQueryDTO;
import com.solar.ops.admin.entity.SparePartInventory;
import com.solar.ops.admin.vo.SparePartInventoryVO;
import org.apache.ibatis.annotations.Param;

public interface SparePartInventoryMapper extends BaseMapper<SparePartInventory> {

    Page<SparePartInventoryVO> selectInventoryPage(Page<SparePartInventoryVO> page,
                                                   @Param("query") SparePartInventoryQueryDTO queryDTO);

    SparePartInventoryVO selectInventoryDetailById(@Param("id") Long id);
}
