package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.entity.Asset;
import com.solar.ops.admin.entity.MaintenanceRecord;
import com.solar.ops.admin.entity.SparePart;
import com.solar.ops.admin.enums.AssetStatusEnum;
import com.solar.ops.admin.mapper.AssetMapper;
import com.solar.ops.admin.mapper.MaintenanceRecordMapper;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MaintenanceRecordService extends ServiceImpl<MaintenanceRecordMapper, MaintenanceRecord> {

    @Resource
    private AssetMapper assetMapper;

    @Resource
    private SparePartService sparePartService;

    public PageResult<MaintenanceRecord> pageByAssetId(PageQuery pageQuery, Long assetId) {
        if (assetId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        Page<MaintenanceRecord> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());

        LambdaQueryWrapper<MaintenanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaintenanceRecord::getAssetId, assetId);
        wrapper.orderByDesc(MaintenanceRecord::getCreateTime);

        page(page, wrapper);
        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public MaintenanceRecord getDetailById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        MaintenanceRecord record = getById(id);
        if (record == null) {
            throw new BusinessException("维修记录不存在");
        }
        List<SparePart> spareParts = sparePartService.listByMaintenanceRecordId(id);
        record.setSpareParts(spareParts);
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addMaintenanceRecord(MaintenanceRecord record) {
        if (record.getAssetId() == null) {
            throw new BusinessException("资产ID不能为空");
        }

        Asset asset = assetMapper.selectById(record.getAssetId());
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }

        record.setRecordNo(generateRecordNo());
        save(record);

        asset.setId(record.getAssetId());
        asset.setAssetStatus(AssetStatusEnum.IN_MAINTENANCE.getCode());
        assetMapper.updateById(asset);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addMaintenanceRecordWithSparePart(MaintenanceRecord record, List<SparePart> spareParts) {
        addMaintenanceRecord(record);

        if (spareParts != null && !spareParts.isEmpty()) {
            for (SparePart sparePart : spareParts) {
                sparePart.setMaintenanceRecordId(record.getId());
                sparePart.setAssetId(record.getAssetId());
                if (sparePart.getReplaceTime() == null) {
                    sparePart.setReplaceTime(record.getMaintenanceTime() != null ? record.getMaintenanceTime() : LocalDateTime.now());
                }
                if (sparePart.getQuantity() == null) {
                    sparePart.setQuantity(1);
                }
                if (sparePart.getUnitPrice() != null && sparePart.getQuantity() != null) {
                    sparePart.setTotalPrice(sparePart.getUnitPrice().multiply(new BigDecimal(sparePart.getQuantity())));
                }
                sparePartService.save(sparePart);
            }
        }
    }

    public void updateMaintenanceRecord(MaintenanceRecord record) {
        if (record.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        MaintenanceRecord existRecord = getById(record.getId());
        if (existRecord == null) {
            throw new BusinessException("维修记录不存在");
        }

        updateById(record);
    }

    public void deleteMaintenanceRecord(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeMaintenance(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        MaintenanceRecord record = getById(id);
        if (record == null) {
            throw new BusinessException("维修记录不存在");
        }

        Asset asset = assetMapper.selectById(record.getAssetId());
        if (asset != null) {
            asset.setAssetStatus(AssetStatusEnum.NORMAL.getCode());
            assetMapper.updateById(asset);
        }
    }

    private String generateRecordNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "WX" + dateStr;

        LambdaQueryWrapper<MaintenanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(MaintenanceRecord::getRecordNo, prefix);
        wrapper.orderByDesc(MaintenanceRecord::getRecordNo);
        wrapper.last("limit 1");

        MaintenanceRecord lastRecord = getOne(wrapper);

        int sequence = 1;
        if (lastRecord != null && lastRecord.getRecordNo() != null) {
            String lastNo = lastRecord.getRecordNo();
            String seqStr = lastNo.substring(prefix.length());
            try {
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }

        return prefix + String.format("%04d", sequence);
    }
}
