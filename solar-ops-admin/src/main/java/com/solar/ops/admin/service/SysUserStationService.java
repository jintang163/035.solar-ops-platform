package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.dto.UserStationAssignDTO;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.entity.SysUserStation;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.admin.mapper.SysUserStationMapper;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.result.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysUserStationService extends ServiceImpl<SysUserStationMapper, SysUserStation> {

    @Resource
    private StationMapper stationMapper;

    public List<SysUserStation> getByUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return list(new LambdaQueryWrapper<SysUserStation>()
                .eq(SysUserStation::getUserId, userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignStations(UserStationAssignDTO dto) {
        if (dto.getUserId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        baseMapper.deleteByUserId(dto.getUserId());

        List<Long> stationIds = dto.getStationIds();
        if (stationIds == null || stationIds.isEmpty()) {
            return;
        }

        List<Station> stations = stationMapper.selectBatchIds(stationIds);
        Map<Long, String> stationNameMap = stations.stream()
                .collect(Collectors.toMap(Station::getId, Station::getStationName));

        Integer permissionType = 2;
        if (dto.getPermissionType() != null) {
            try {
                permissionType = Integer.parseInt(dto.getPermissionType());
            } catch (NumberFormatException e) {
                permissionType = 2;
            }
        }

        List<SysUserStation> userStations = new ArrayList<>();
        for (Long stationId : stationIds) {
            SysUserStation userStation = new SysUserStation();
            userStation.setUserId(dto.getUserId());
            userStation.setStationId(stationId);
            userStation.setStationName(stationNameMap.get(stationId));
            userStation.setPermissionType(permissionType);
            userStations.add(userStation);
        }

        baseMapper.batchInsert(userStations);
    }
}
