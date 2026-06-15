package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.admin.dto.WorkspaceSwitchDTO;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.entity.SysOrg;
import com.solar.ops.admin.entity.SysUser;
import com.solar.ops.admin.enums.DataScopeEnum;
import com.solar.ops.admin.holder.LoginUserHolder;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.admin.mapper.SysOrgMapper;
import com.solar.ops.admin.mapper.SysUserMapper;
import com.solar.ops.admin.util.DataScopeHelper;
import com.solar.ops.admin.vo.StationTreeVO;
import com.solar.ops.admin.vo.UserWorkspaceVO;
import com.solar.ops.common.exception.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    @Resource
    private LoginUserHolder loginUserHolder;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysOrgMapper sysOrgMapper;

    @Resource
    private StationMapper stationMapper;

    @Resource
    private DataScopeHelper dataScopeHelper;

    public UserWorkspaceVO getWorkspaceInfo() {
        Long userId = loginUserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserWorkspaceVO vo = new UserWorkspaceVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setIsAdmin(loginUserHolder.getIsAdmin() != null && loginUserHolder.getIsAdmin() == 1);
        vo.setOrgId(loginUserHolder.getOrgId());
        vo.setDataScope(DataScopeEnum.getDesc(loginUserHolder.getDataScope()));
        vo.setCurrentStationId(loginUserHolder.getCurrentStationId());

        if (loginUserHolder.getOrgId() != null) {
            SysOrg org = sysOrgMapper.selectById(loginUserHolder.getOrgId());
            if (org != null) {
                vo.setOrgName(org.getOrgName());
            }
        }

        List<Long> stationIds = dataScopeHelper.getAccessibleStationIds();
        vo.setStationIds(stationIds);

        if (stationIds != null && !stationIds.isEmpty()) {
            List<Station> stations = stationMapper.selectBatchIds(stationIds);
            List<StationTreeVO> stationTreeVOS = stations.stream()
                    .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                    .map(this::convertToStationTreeVO)
                    .collect(Collectors.toList());
            vo.setStations(stationTreeVOS);
        } else if (stationIds == null) {
            List<Station> stations = stationMapper.selectList(new LambdaQueryWrapper<Station>()
                    .eq(Station::getStatus, 1));
            List<StationTreeVO> stationTreeVOS = stations.stream()
                    .map(this::convertToStationTreeVO)
                    .collect(Collectors.toList());
            vo.setStations(stationTreeVOS);
        } else {
            vo.setStations(new ArrayList<>());
        }

        return vo;
    }

    public List<StationTreeVO> getStationTree() {
        List<Long> stationIds = dataScopeHelper.getAccessibleStationIds();
        List<Station> stations;

        if (stationIds == null) {
            stations = stationMapper.selectList(new LambdaQueryWrapper<Station>()
                    .eq(Station::getStatus, 1));
        } else if (stationIds.isEmpty()) {
            return new ArrayList<>();
        } else {
            stations = stationMapper.selectBatchIds(stationIds);
            stations = stations.stream()
                    .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                    .collect(Collectors.toList());
        }

        return stations.stream()
                .map(this::convertToStationTreeVO)
                .collect(Collectors.toList());
    }

    public void switchWorkspace(WorkspaceSwitchDTO dto) {
        Long stationId = dto.getStationId();

        if (stationId != null) {
            List<Long> accessibleStationIds = dataScopeHelper.getAccessibleStationIds();
            if (accessibleStationIds != null && !accessibleStationIds.contains(stationId)) {
                throw new BusinessException("无权限访问该电站");
            }

            Station station = stationMapper.selectById(stationId);
            if (station == null || station.getStatus() == null || station.getStatus() != 1) {
                throw new BusinessException("电站不存在或未启用");
            }
        }

        loginUserHolder.setCurrentStationId(stationId);
    }

    private StationTreeVO convertToStationTreeVO(Station station) {
        StationTreeVO vo = new StationTreeVO();
        BeanUtils.copyProperties(station, vo);
        vo.setId(station.getId());
        vo.setStationCode(station.getStationCode());
        vo.setStationName(station.getStationName());
        vo.setCapacity(station.getCapacity());
        vo.setStatus(station.getStatus());

        if (station.getOrgId() != null) {
            vo.setOrgId(station.getOrgId());
            SysOrg org = sysOrgMapper.selectById(station.getOrgId());
            if (org != null) {
                vo.setOrgName(org.getOrgName());
            }
        }

        return vo;
    }
}
