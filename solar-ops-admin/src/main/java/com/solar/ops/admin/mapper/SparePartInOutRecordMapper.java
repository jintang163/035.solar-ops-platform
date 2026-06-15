package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.dto.SparePartInOutRecordQueryDTO;
import com.solar.ops.admin.entity.SparePartInOutRecord;
import com.solar.ops.admin.vo.SparePartInOutRecordVO;
import org.apache.ibatis.annotations.Param;

public interface SparePartInOutRecordMapper extends BaseMapper<SparePartInOutRecord> {

    Page<SparePartInOutRecordVO> selectRecordPage(Page<SparePartInOutRecordVO> page,
                                                   @Param("query") SparePartInOutRecordQueryDTO queryDTO);
}
