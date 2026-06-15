package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.dto.OrgQueryDTO;
import com.solar.ops.admin.dto.SysOrgDTO;
import com.solar.ops.admin.entity.SysOrg;
import com.solar.ops.admin.mapper.SysOrgMapper;
import com.solar.ops.admin.util.DataScopeHelper;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.ResultCode;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysOrgService extends ServiceImpl<SysOrgMapper, SysOrg> {

    @Resource
    private DataScopeHelper dataScopeHelper;

    public PageResult<SysOrg> page(PageQuery pageQuery, OrgQueryDTO queryDTO) {
        Page<SysOrg> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());

        QueryWrapper<SysOrg> wrapper = new QueryWrapper<>();
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getKeyword())) {
                wrapper.like("org_name", queryDTO.getKeyword())
                        .or().like("org_code", queryDTO.getKeyword());
            }
            if (StringUtils.hasText(queryDTO.getOrgType())) {
                wrapper.eq("org_type", queryDTO.getOrgType());
            }
            if (queryDTO.getStatus() != null) {
                wrapper.eq("status", queryDTO.getStatus());
            }
        }
        wrapper.orderByDesc("create_time");

        dataScopeHelper.injectOrgDataScope(wrapper, "id");

        page(page, wrapper);
        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public SysOrg getOrgById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return getById(id);
    }

    public void addOrg(SysOrgDTO dto) {
        SysOrg existOrg = getOne(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getOrgCode, dto.getOrgCode()));
        if (existOrg != null) {
            throw new BusinessException("组织编码已存在");
        }

        SysOrg org = new SysOrg();
        BeanUtils.copyProperties(dto, org);
        if (org.getStatus() == null) {
            org.setStatus(1);
        }
        if (org.getSortOrder() == null) {
            org.setSortOrder(0);
        }
        save(org);
    }

    public void updateOrg(SysOrgDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        SysOrg existOrg = getById(dto.getId());
        if (existOrg == null) {
            throw new BusinessException("组织不存在");
        }

        if (StringUtils.hasText(dto.getOrgCode()) && !dto.getOrgCode().equals(existOrg.getOrgCode())) {
            SysOrg sameCodeOrg = getOne(new LambdaQueryWrapper<SysOrg>()
                    .eq(SysOrg::getOrgCode, dto.getOrgCode()));
            if (sameCodeOrg != null) {
                throw new BusinessException("组织编码已存在");
            }
        }

        SysOrg org = new SysOrg();
        BeanUtils.copyProperties(dto, org);
        updateById(org);
    }

    public void deleteOrg(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        List<Long> childIds = baseMapper.getChildOrgIds(id);
        if (childIds != null && !childIds.isEmpty()) {
            throw new BusinessException("存在下级组织，无法删除");
        }

        removeById(id);
    }

    public List<SysOrg> getOrgTree() {
        List<SysOrg> allOrgs = list(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getStatus, 1)
                .orderByAsc(SysOrg::getSortOrder)
                .orderByDesc(SysOrg::getCreateTime));

        List<Long> accessibleOrgIds = dataScopeHelper.getAccessibleOrgIds();
        if (accessibleOrgIds != null) {
            allOrgs = allOrgs.stream()
                    .filter(org -> accessibleOrgIds.contains(org.getId()))
                    .collect(Collectors.toList());
        }

        return buildTree(allOrgs);
    }

    private List<SysOrg> buildTree(List<SysOrg> orgs) {
        if (orgs == null || orgs.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, SysOrg> orgMap = new HashMap<>();
        for (SysOrg org : orgs) {
            orgMap.put(org.getId(), org);
        }

        List<SysOrg> roots = new ArrayList<>();
        for (SysOrg org : orgs) {
            Long parentId = org.getParentId();
            if (parentId == null || parentId == 0 || !orgMap.containsKey(parentId)) {
                roots.add(org);
            } else {
                SysOrg parent = orgMap.get(parentId);
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(org);
            }
        }

        return roots;
    }
}
