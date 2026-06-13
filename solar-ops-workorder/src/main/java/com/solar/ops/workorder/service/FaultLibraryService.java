package com.solar.ops.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.workorder.dto.FaultLibraryQueryDTO;
import com.solar.ops.workorder.entity.FaultLibrary;
import com.solar.ops.workorder.mapper.FaultLibraryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class FaultLibraryService extends ServiceImpl<FaultLibraryMapper, FaultLibrary> {

    private final Map<String, FaultLibrary> faultCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadFaultCache();
    }

    public void loadFaultCache() {
        List<FaultLibrary> list = this.list();
        faultCache.clear();
        for (FaultLibrary fault : list) {
            faultCache.put(fault.getFaultCode(), fault);
        }
    }

    public FaultLibrary getByFaultCode(String faultCode) {
        if (!StringUtils.hasText(faultCode)) {
            return null;
        }
        FaultLibrary fault = faultCache.get(faultCode);
        if (fault == null) {
            LambdaQueryWrapper<FaultLibrary> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FaultLibrary::getFaultCode, faultCode);
            fault = this.getOne(wrapper);
            if (fault != null) {
                faultCache.put(faultCode, fault);
            }
        }
        return fault;
    }

    public PageResult<FaultLibrary> page(FaultLibraryQueryDTO dto) {
        LambdaQueryWrapper<FaultLibrary> wrapper = new LambdaQueryWrapper<>();
        if (dto != null) {
            if (StringUtils.hasText(dto.getFaultCode())) {
                wrapper.like(FaultLibrary::getFaultCode, dto.getFaultCode());
            }
            if (StringUtils.hasText(dto.getFaultName())) {
                wrapper.like(FaultLibrary::getFaultName, dto.getFaultName());
            }
            if (dto.getFaultLevel() != null) {
                wrapper.eq(FaultLibrary::getFaultLevel, dto.getFaultLevel());
            }
        }
        wrapper.orderByDesc(FaultLibrary::getCreateTime);

        Page<FaultLibrary> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<FaultLibrary> result = this.page(page, wrapper);
        return PageResult.build(result.getTotal(), result.getRecords(), dto.getPageNum(), dto.getPageSize());
    }

    public boolean addFault(FaultLibrary faultLibrary) {
        boolean saved = this.save(faultLibrary);
        if (saved) {
            faultCache.put(faultLibrary.getFaultCode(), faultLibrary);
        }
        return saved;
    }

    public boolean updateFault(FaultLibrary faultLibrary) {
        boolean updated = this.updateById(faultLibrary);
        if (updated) {
            FaultLibrary updatedFault = this.getById(faultLibrary.getId());
            faultCache.put(updatedFault.getFaultCode(), updatedFault);
        }
        return updated;
    }

    public boolean deleteFault(Long id) {
        FaultLibrary fault = this.getById(id);
        boolean deleted = this.removeById(id);
        if (deleted && fault != null) {
            faultCache.remove(fault.getFaultCode());
        }
        return deleted;
    }

    public List<FaultLibrary> listAll() {
        return this.list();
    }
}
