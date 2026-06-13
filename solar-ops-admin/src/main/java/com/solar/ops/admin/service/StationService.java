package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class StationService extends ServiceImpl<StationMapper, Station> {

    public PageResult<Station> page(PageQuery pageQuery, String keyword, Integer status) {
        Page<Station> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());

        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Station::getStationName, keyword)
                    .or().like(Station::getStationCode, keyword);
        }
        if (status != null) {
            wrapper.eq(Station::getStatus, status);
        }
        wrapper.orderByDesc(Station::getCreateTime);

        page(page, wrapper);
        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public List<Station> listAll() {
        return list(new LambdaQueryWrapper<Station>()
                .eq(Station::getStatus, 1)
                .orderByDesc(Station::getCreateTime));
    }

    public void addStation(Station station) {
        Station existStation = getOne(new LambdaQueryWrapper<Station>()
                .eq(Station::getStationCode, station.getStationCode()));
        if (existStation != null) {
            throw new BusinessException("电站编号已存在");
        }

        if (station.getStatus() == null) {
            station.setStatus(1);
        }
        save(station);
    }

    public void updateStation(Station station) {
        if (station.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        Station existStation = getById(station.getId());
        if (existStation == null) {
            throw new BusinessException("电站不存在");
        }

        if (StringUtils.hasText(station.getStationCode()) && !station.getStationCode().equals(existStation.getStationCode())) {
            Station sameCodeStation = getOne(new LambdaQueryWrapper<Station>()
                    .eq(Station::getStationCode, station.getStationCode()));
            if (sameCodeStation != null) {
                throw new BusinessException("电站编号已存在");
            }
        }

        updateById(station);
    }

    public void deleteStation(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        removeById(id);
    }

    public Station getStationById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return getById(id);
    }
}
