package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.dto.SparePartOutboundDTO;
import com.solar.ops.admin.dto.WorkOrderSparePartOutboundDTO;
import com.solar.ops.admin.entity.Asset;
import com.solar.ops.admin.entity.MaintenanceRecord;
import com.solar.ops.admin.entity.SparePart;
import com.solar.ops.admin.entity.SparePartInventory;
import com.solar.ops.admin.enums.AssetStatusEnum;
import com.solar.ops.admin.enums.SparePartInOutTypeEnum;
import com.solar.ops.admin.mapper.AssetMapper;
import com.solar.ops.admin.mapper.MaintenanceRecordMapper;
import com.solar.ops.admin.mapper.SparePartInventoryMapper;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MaintenanceRecordService extends ServiceImpl<MaintenanceRecordMapper, MaintenanceRecord> {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceRecordService.class);

    @Resource
    private AssetMapper assetMapper;

    @Resource
    private SparePartService sparePartService;

    @Resource
    private SparePartInventoryService sparePartInventoryService;

    @Resource
    private SparePartInventoryMapper sparePartInventoryMapper;

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

        if (!CollectionUtils.isEmpty(spareParts)) {
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

                deductInventoryForSparePart(sparePart, record.getId(), record.getWorkOrderId(),
                        record.getAssetId(), record.getMaintenancePerson());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deductInventoryForSparePart(SparePart sparePart, Long maintenanceRecordId,
                                            Long workOrderId, Long assetId, String operatorName) {
        if (sparePart == null || sparePart.getPartCode() == null || sparePart.getQuantity() == null) {
            return;
        }

        SparePartInventory inventory = sparePartInventoryMapper.selectOne(
                new LambdaQueryWrapper<SparePartInventory>()
                        .eq(SparePartInventory::getPartCode, sparePart.getPartCode())
                        .eq(SparePartInventory::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (inventory == null) {
            log.warn("备件库存不存在，跳过扣减，partCode: {}", sparePart.getPartCode());
            return;
        }

        try {
            SparePartOutboundDTO outboundDTO = new SparePartOutboundDTO();
            outboundDTO.setPartId(inventory.getId());
            outboundDTO.setInOutType(SparePartInOutTypeEnum.WORK_ORDER_OUT.getCode());
            outboundDTO.setQuantity(sparePart.getQuantity());
            outboundDTO.setWorkOrderId(workOrderId);
            outboundDTO.setMaintenanceRecordId(maintenanceRecordId);
            outboundDTO.setAssetId(assetId);
            outboundDTO.setOperatorName(operatorName);
            outboundDTO.setOperateTime(LocalDateTime.now());
            outboundDTO.setRemark("维修领用，维修记录ID: " + maintenanceRecordId);

            sparePartInventoryService.outbound(outboundDTO);

            if (sparePart.getUnitPrice() == null && inventory.getUnitPrice() != null) {
                sparePart.setUnitPrice(inventory.getUnitPrice());
                sparePart.setTotalPrice(inventory.getUnitPrice().multiply(new BigDecimal(sparePart.getQuantity())));
                sparePartService.updateById(sparePart);
            }
        } catch (BusinessException e) {
            throw new BusinessException("备件[" + sparePart.getPartName() + "]扣减库存失败: " + e.getMessage());
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

    @Transactional(rollbackFor = Exception.class)
    public Long workOrderOutboundSpareParts(WorkOrderSparePartOutboundDTO dto) {
        if (dto.getAssetId() == null) {
            throw new BusinessException("资产ID不能为空");
        }
        if (CollectionUtils.isEmpty(dto.getSpareParts())) {
            throw new BusinessException("领用备件列表不能为空");
        }

        MaintenanceRecord record = new MaintenanceRecord();
        record.setAssetId(dto.getAssetId());
        record.setWorkOrderId(dto.getWorkOrderId());
        record.setFaultDescription(dto.getFaultDescription());
        record.setFaultType(dto.getFaultType());
        record.setMaintenanceType(dto.getMaintenanceType());
        record.setMaintenanceTime(dto.getMaintenanceTime() != null ? dto.getMaintenanceTime() : LocalDateTime.now());
        record.setMaintenancePerson(dto.getMaintenancePerson() != null ? dto.getMaintenancePerson() : dto.getOperatorName());
        record.setMaintenanceContent(dto.getMaintenanceContent());
        record.setSolution(dto.getSolution());
        record.setPhotos(dto.getPhotos());
        record.setCost(dto.getCost());
        record.setRemark(dto.getRemark());

        addMaintenanceRecord(record);

        for (WorkOrderSparePartOutboundDTO.SparePartItem item : dto.getSpareParts()) {
            if (item.getPartId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }

            SparePartInventory inventory = sparePartInventoryMapper.selectById(item.getPartId());
            if (inventory == null) {
                throw new BusinessException("备件ID: " + item.getPartId() + " 不存在");
            }

            SparePart sparePart = new SparePart();
            sparePart.setMaintenanceRecordId(record.getId());
            sparePart.setAssetId(dto.getAssetId());
            sparePart.setPartCode(inventory.getPartCode());
            sparePart.setPartName(inventory.getPartName());
            sparePart.setPartModel(inventory.getPartModel());
            sparePart.setBrand(inventory.getBrand());
            sparePart.setSpecification(inventory.getSpecification());
            sparePart.setQuantity(item.getQuantity());
            sparePart.setUnitPrice(inventory.getUnitPrice());
            if (inventory.getUnitPrice() != null) {
                sparePart.setTotalPrice(inventory.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
            }
            sparePart.setSupplier(inventory.getSupplier());
            sparePart.setReplaceTime(record.getMaintenanceTime());
            sparePart.setOperator(dto.getOperatorName());
            sparePart.setRemark(dto.getRemark());
            sparePartService.save(sparePart);

            SparePartOutboundDTO outboundDTO = new SparePartOutboundDTO();
            outboundDTO.setPartId(inventory.getId());
            outboundDTO.setInOutType(SparePartInOutTypeEnum.WORK_ORDER_OUT.getCode());
            outboundDTO.setQuantity(item.getQuantity());
            outboundDTO.setWorkOrderId(dto.getWorkOrderId());
            outboundDTO.setWorkOrderNo(dto.getWorkOrderNo());
            outboundDTO.setMaintenanceRecordId(record.getId());
            outboundDTO.setAssetId(dto.getAssetId());
            outboundDTO.setOperatorName(dto.getOperatorName());
            outboundDTO.setOperateTime(LocalDateTime.now());
            outboundDTO.setRemark("工单领用，工单ID: " + dto.getWorkOrderId());

            sparePartInventoryService.outbound(outboundDTO);
        }

        return record.getId();
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
