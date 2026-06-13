package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.entity.SparePart;
import com.solar.ops.admin.mapper.SparePartMapper;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.result.ResultCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SparePartService extends ServiceImpl<SparePartMapper, SparePart> {

    public List<SparePart> listByMaintenanceRecordId(Long maintenanceRecordId) {
        if (maintenanceRecordId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return list(new LambdaQueryWrapper<SparePart>()
                .eq(SparePart::getMaintenanceRecordId, maintenanceRecordId)
                .orderByDesc(SparePart::getCreateTime));
    }

    public void addSparePart(SparePart sparePart) {
        if (sparePart.getMaintenanceRecordId() == null) {
            throw new BusinessException("维修记录ID不能为空");
        }
        save(sparePart);
    }

    public void deleteSparePart(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        removeById(id);
    }
}
