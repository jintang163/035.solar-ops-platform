package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class InverterService extends ServiceImpl<InverterMapper, Inverter> {

    public PageResult<Inverter> page(PageQuery pageQuery, Long stationId, String keyword, Integer status, Integer onlineStatus) {
        Page<Inverter> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());

        LambdaQueryWrapper<Inverter> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(Inverter::getStationId, stationId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Inverter::getDeviceName, keyword)
                    .or().like(Inverter::getDeviceSn, keyword);
        }
        if (status != null) {
            wrapper.eq(Inverter::getStatus, status);
        }
        if (onlineStatus != null) {
            wrapper.eq(Inverter::getOnlineStatus, onlineStatus);
        }
        wrapper.orderByDesc(Inverter::getCreateTime);

        page(page, wrapper);
        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public List<Inverter> listByStationId(Long stationId) {
        return list(new LambdaQueryWrapper<Inverter>()
                .eq(Inverter::getStationId, stationId)
                .eq(Inverter::getStatus, 1)
                .orderByDesc(Inverter::getCreateTime));
    }

    public void addInverter(Inverter inverter) {
        Inverter existInverter = getOne(new LambdaQueryWrapper<Inverter>()
                .eq(Inverter::getDeviceSn, inverter.getDeviceSn()));
        if (existInverter != null) {
            throw new BusinessException("设备序列号已存在");
        }

        if (inverter.getStatus() == null) {
            inverter.setStatus(1);
        }
        if (inverter.getOnlineStatus() == null) {
            inverter.setOnlineStatus(0);
        }
        save(inverter);
    }

    public void updateInverter(Inverter inverter) {
        if (inverter.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        Inverter existInverter = getById(inverter.getId());
        if (existInverter == null) {
            throw new BusinessException("逆变器不存在");
        }

        if (StringUtils.hasText(inverter.getDeviceSn()) && !inverter.getDeviceSn().equals(existInverter.getDeviceSn())) {
            Inverter sameSnInverter = getOne(new LambdaQueryWrapper<Inverter>()
                    .eq(Inverter::getDeviceSn, inverter.getDeviceSn()));
            if (sameSnInverter != null) {
                throw new BusinessException("设备序列号已存在");
            }
        }

        updateById(inverter);
    }

    public void deleteInverter(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        removeById(id);
    }

    public Inverter getInverterById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return getById(id);
    }

    public void updateOnlineStatus(Long id, Integer onlineStatus) {
        if (id == null || onlineStatus == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        Inverter inverter = new Inverter();
        inverter.setId(id);
        inverter.setOnlineStatus(onlineStatus);
        updateById(inverter);
    }
}
