package com.solar.ops.admin.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.solar.ops.admin.enums.DataScopeEnum;
import com.solar.ops.admin.holder.LoginUserHolder;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.admin.mapper.SysOrgMapper;
import com.solar.ops.admin.mapper.SysUserStationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataScopeHelper {

    @Autowired
    private LoginUserHolder loginUserHolder;

    @Autowired
    private SysOrgMapper sysOrgMapper;

    @Autowired
    private SysUserStationMapper sysUserStationMapper;

    @Autowired
    private StationMapper stationMapper;

    public void injectDataScope(QueryWrapper<?> wrapper, String stationIdColumn) {
        List<Long> stationIds = getAccessibleStationIds();
        if (stationIds == null) {
            return;
        }
        if (stationIds.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(stationIdColumn, stationIds);
    }

    public void injectOrgDataScope(QueryWrapper<?> wrapper, String orgIdColumn) {
        List<Long> orgIds = getAccessibleOrgIds();
        if (orgIds == null) {
            return;
        }
        if (orgIds.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(orgIdColumn, orgIds);
    }

    public List<Long> getAccessibleStationIds() {
        if (isSuperAdmin()) {
            return null;
        }

        Integer dataScope = loginUserHolder.getDataScope();
        if (DataScopeEnum.ALL.getCode().equals(dataScope)) {
            return null;
        }

        List<Long> stationIds = loginUserHolder.getStationIds();
        if (stationIds != null && !stationIds.isEmpty()) {
            return stationIds;
        }

        Long userId = loginUserHolder.getUserId();
        if (userId == null) {
            return new ArrayList<>();
        }

        if (DataScopeEnum.ORG_AND_CHILD.getCode().equals(dataScope)) {
            List<Long> orgIds = getAccessibleOrgIds();
            if (orgIds == null || orgIds.isEmpty()) {
                return new ArrayList<>();
            }
            stationIds = stationMapper.getStationIdsByOrgIds(orgIds);
        } else {
            stationIds = sysUserStationMapper.getStationIdsByUserId(userId);
        }

        if (stationIds == null) {
            stationIds = new ArrayList<>();
        }

        loginUserHolder.setStationIds(stationIds);
        return stationIds;
    }

    public List<Long> getAccessibleOrgIds() {
        if (isSuperAdmin()) {
            return null;
        }

        Long orgId = loginUserHolder.getOrgId();
        if (orgId == null) {
            return new ArrayList<>();
        }

        Set<Long> allOrgIds = new HashSet<>();
        allOrgIds.add(orgId);

        List<Long> currentLevel = new ArrayList<>();
        currentLevel.add(orgId);

        while (!currentLevel.isEmpty()) {
            List<Long> childIds = sysOrgMapper.getChildOrgIdsBatch(currentLevel);
            if (childIds == null || childIds.isEmpty()) {
                break;
            }
            allOrgIds.addAll(childIds);
            currentLevel = childIds;
        }

        return new ArrayList<>(allOrgIds);
    }

    public boolean isSuperAdmin() {
        Integer isAdmin = loginUserHolder.getIsAdmin();
        return isAdmin != null && isAdmin == 1;
    }

    public Long getCurrentStationId() {
        return loginUserHolder.getCurrentStationId();
    }
}
