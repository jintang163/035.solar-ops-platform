package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.dto.StocktakeCreateDTO;
import com.solar.ops.admin.dto.StocktakeItemUpdateDTO;
import com.solar.ops.admin.dto.StocktakeQueryDTO;
import com.solar.ops.admin.entity.SparePartInOutRecord;
import com.solar.ops.admin.entity.SparePartInventory;
import com.solar.ops.admin.entity.SparePartStocktake;
import com.solar.ops.admin.entity.SparePartStocktakeItem;
import com.solar.ops.admin.enums.*;
import com.solar.ops.admin.excel.StocktakeDiffExcelVO;
import com.solar.ops.admin.mapper.SparePartInOutRecordMapper;
import com.solar.ops.admin.mapper.SparePartInventoryMapper;
import com.solar.ops.admin.mapper.SparePartStocktakeItemMapper;
import com.solar.ops.admin.mapper.SparePartStocktakeMapper;
import com.solar.ops.admin.vo.SparePartStocktakeItemVO;
import com.solar.ops.admin.vo.SparePartStocktakeVO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SparePartStocktakeService extends ServiceImpl<SparePartStocktakeMapper, SparePartStocktake> {

    @Resource
    private SparePartInventoryMapper inventoryMapper;

    @Resource
    private SparePartStocktakeItemMapper stocktakeItemMapper;

    @Resource
    private SparePartInOutRecordMapper inOutRecordMapper;

    public PageResult<SparePartStocktakeVO> page(PageQuery pageQuery, StocktakeQueryDTO queryDTO) {
        Page<SparePartStocktakeVO> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        baseMapper.selectStocktakePage(page, queryDTO);
        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public SparePartStocktakeVO getDetailById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return baseMapper.selectStocktakeDetailById(id);
    }

    public PageResult<SparePartStocktakeItemVO> getItemPage(PageQuery pageQuery, Long stocktakeId, Integer diffType) {
        if (stocktakeId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        Page<SparePartStocktakeItemVO> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        stocktakeItemMapper.selectItemPage(page, stocktakeId, diffType);
        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public void createStocktake(StocktakeCreateDTO createDTO) {
        if (createDTO.getStocktakeName() == null || createDTO.getStocktakeType() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        String stocktakeNo = generateStocktakeNo();

        SparePartStocktake stocktake = new SparePartStocktake();
        stocktake.setStocktakeNo(stocktakeNo);
        stocktake.setStocktakeName(createDTO.getStocktakeName());
        stocktake.setStocktakeType(createDTO.getStocktakeType());
        stocktake.setWarehouse(createDTO.getWarehouse());
        stocktake.setPartType(createDTO.getPartType());
        stocktake.setStatus(StocktakeStatusEnum.PENDING.getCode());
        stocktake.setOperatorName(createDTO.getOperatorName());
        stocktake.setRemark(createDTO.getRemark());
        save(stocktake);

        generateStocktakeItems(stocktake.getId(), stocktakeNo, createDTO.getWarehouse(), createDTO.getPartType());
    }

    @Transactional(rollbackFor = Exception.class)
    public void startStocktake(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        SparePartStocktake stocktake = getById(id);
        if (stocktake == null) {
            throw new BusinessException("盘点单不存在");
        }
        if (!StocktakeStatusEnum.PENDING.getCode().equals(stocktake.getStatus())) {
            throw new BusinessException("当前状态不允许开始盘点");
        }
        stocktake.setStatus(StocktakeStatusEnum.IN_PROGRESS.getCode());
        stocktake.setStocktakeTime(LocalDateTime.now());
        updateById(stocktake);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStocktakeItem(StocktakeItemUpdateDTO updateDTO) {
        if (updateDTO.getItemId() == null || updateDTO.getActualQuantity() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        SparePartStocktakeItem item = stocktakeItemMapper.selectById(updateDTO.getItemId());
        if (item == null) {
            throw new BusinessException("盘点明细不存在");
        }

        SparePartStocktake stocktake = getById(item.getStocktakeId());
        if (stocktake == null || !StocktakeStatusEnum.IN_PROGRESS.getCode().equals(stocktake.getStatus())) {
            throw new BusinessException("当前状态不允许修改盘点明细");
        }

        int diffQuantity = updateDTO.getActualQuantity() - item.getSystemQuantity();
        int diffType = DiffTypeEnum.NO_DIFF.getCode();
        if (diffQuantity > 0) {
            diffType = DiffTypeEnum.PROFIT.getCode();
        } else if (diffQuantity < 0) {
            diffType = DiffTypeEnum.LOSS.getCode();
        }

        BigDecimal diffAmount = BigDecimal.ZERO;
        if (item.getUnitPrice() != null) {
            diffAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(Math.abs(diffQuantity)));
        }

        item.setActualQuantity(updateDTO.getActualQuantity());
        item.setDiffQuantity(diffQuantity);
        item.setDiffType(diffType);
        item.setDiffAmount(diffAmount);
        item.setRemark(updateDTO.getRemark());
        stocktakeItemMapper.updateById(item);

        updateStocktakeStats(item.getStocktakeId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeStocktake(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        SparePartStocktake stocktake = getById(id);
        if (stocktake == null) {
            throw new BusinessException("盘点单不存在");
        }
        if (!StocktakeStatusEnum.IN_PROGRESS.getCode().equals(stocktake.getStatus())) {
            throw new BusinessException("当前状态不允许完成盘点");
        }

        List<SparePartStocktakeItem> items = stocktakeItemMapper.selectList(
                new LambdaQueryWrapper<SparePartStocktakeItem>()
                        .eq(SparePartStocktakeItem::getStocktakeId, id)
                        .eq(SparePartStocktakeItem::getDeleted, 0)
        );

        for (SparePartStocktakeItem item : items) {
            SparePartInventory inventory = inventoryMapper.selectById(item.getPartId());
            if (inventory == null) {
                continue;
            }

            if (DiffTypeEnum.PROFIT.getCode().equals(item.getDiffType())) {
                inventory.setQuantity(item.getActualQuantity());
                updateInventoryWarnStatus(inventory);
                inventoryMapper.updateById(inventory);

                SparePartInOutRecord record = new SparePartInOutRecord();
                record.setRecordNo(generateRecordNo("IN"));
                record.setRecordType(SparePartRecordTypeEnum.IN.getCode());
                record.setInOutType(SparePartInOutTypeEnum.PROFIT_IN.getCode());
                record.setPartId(inventory.getId());
                record.setPartCode(inventory.getPartCode());
                record.setPartName(inventory.getPartName());
                record.setPartModel(inventory.getPartModel());
                record.setQuantity(item.getDiffQuantity());
                record.setUnitPrice(inventory.getUnitPrice());
                if (inventory.getUnitPrice() != null) {
                    record.setTotalPrice(inventory.getUnitPrice().multiply(BigDecimal.valueOf(item.getDiffQuantity())));
                }
                record.setBeforeQuantity(item.getSystemQuantity());
                record.setAfterQuantity(item.getActualQuantity());
                record.setOperatorName(stocktake.getOperatorName());
                record.setOperateTime(LocalDateTime.now());
                record.setStorageLocation(inventory.getStorageLocation());
                record.setRemark("盘点盘盈，盘点单号：" + stocktake.getStocktakeNo());
                inOutRecordMapper.insert(record);
            } else if (DiffTypeEnum.LOSS.getCode().equals(item.getDiffType())) {
                inventory.setQuantity(item.getActualQuantity());
                updateInventoryWarnStatus(inventory);
                inventoryMapper.updateById(inventory);

                SparePartInOutRecord record = new SparePartInOutRecord();
                record.setRecordNo(generateRecordNo("OUT"));
                record.setRecordType(SparePartRecordTypeEnum.OUT.getCode());
                record.setInOutType(SparePartInOutTypeEnum.LOSS_OUT.getCode());
                record.setPartId(inventory.getId());
                record.setPartCode(inventory.getPartCode());
                record.setPartName(inventory.getPartName());
                record.setPartModel(inventory.getPartModel());
                record.setQuantity(Math.abs(item.getDiffQuantity()));
                record.setUnitPrice(inventory.getUnitPrice());
                if (inventory.getUnitPrice() != null) {
                    record.setTotalPrice(inventory.getUnitPrice().multiply(BigDecimal.valueOf(Math.abs(item.getDiffQuantity()))));
                }
                record.setBeforeQuantity(item.getSystemQuantity());
                record.setAfterQuantity(item.getActualQuantity());
                record.setOperatorName(stocktake.getOperatorName());
                record.setOperateTime(LocalDateTime.now());
                record.setStorageLocation(inventory.getStorageLocation());
                record.setRemark("盘点盘亏，盘点单号：" + stocktake.getStocktakeNo());
                inOutRecordMapper.insert(record);
            }
        }

        stocktake.setStatus(StocktakeStatusEnum.COMPLETED.getCode());
        stocktake.setCompleteTime(LocalDateTime.now());
        updateById(stocktake);
    }

    public void cancelStocktake(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        SparePartStocktake stocktake = getById(id);
        if (stocktake == null) {
            throw new BusinessException("盘点单不存在");
        }
        if (StocktakeStatusEnum.COMPLETED.getCode().equals(stocktake.getStatus())) {
            throw new BusinessException("已完成的盘点单不能取消");
        }
        stocktake.setStatus(StocktakeStatusEnum.CANCELLED.getCode());
        updateById(stocktake);
    }

    public List<StocktakeDiffExcelVO> exportDiffReport(Long stocktakeId) {
        if (stocktakeId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        SparePartStocktake stocktake = getById(stocktakeId);
        if (stocktake == null) {
            throw new BusinessException("盘点单不存在");
        }

        List<SparePartStocktakeItemVO> items = stocktakeItemMapper.selectItemsByStocktakeId(stocktakeId);
        return items.stream()
                .filter(item -> item.getDiffType() != null && item.getDiffType() > 0)
                .map(item -> {
                    StocktakeDiffExcelVO excelVO = new StocktakeDiffExcelVO();
                    excelVO.setStocktakeNo(item.getStocktakeNo());
                    excelVO.setPartCode(item.getPartCode());
                    excelVO.setPartName(item.getPartName());
                    excelVO.setPartModel(item.getPartModel());
                    excelVO.setPartTypeDesc(item.getPartTypeDesc());
                    excelVO.setUnit(item.getUnit());
                    excelVO.setSystemQuantity(item.getSystemQuantity());
                    excelVO.setActualQuantity(item.getActualQuantity());
                    excelVO.setDiffQuantity(item.getDiffQuantity());
                    excelVO.setDiffTypeDesc(item.getDiffTypeDesc());
                    excelVO.setDiffAmount(item.getDiffAmount());
                    excelVO.setStorageLocation(item.getStorageLocation());
                    excelVO.setRemark(item.getRemark());
                    return excelVO;
                })
                .collect(Collectors.toList());
    }

    private void generateStocktakeItems(Long stocktakeId, String stocktakeNo, String warehouse, String partType) {
        LambdaQueryWrapper<SparePartInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SparePartInventory::getStatus, 1);
        wrapper.eq(SparePartInventory::getDeleted, 0);
        if (warehouse != null && !warehouse.isEmpty()) {
            wrapper.eq(SparePartInventory::getWarehouse, warehouse);
        }
        if (partType != null && !partType.isEmpty()) {
            wrapper.eq(SparePartInventory::getPartType, partType);
        }
        wrapper.orderByAsc(SparePartInventory::getPartCode);

        List<SparePartInventory> inventories = inventoryMapper.selectList(wrapper);

        List<SparePartStocktakeItem> items = new ArrayList<>();
        for (SparePartInventory inv : inventories) {
            SparePartStocktakeItem item = new SparePartStocktakeItem();
            item.setStocktakeId(stocktakeId);
            item.setStocktakeNo(stocktakeNo);
            item.setPartId(inv.getId());
            item.setPartCode(inv.getPartCode());
            item.setPartName(inv.getPartName());
            item.setPartModel(inv.getPartModel());
            item.setPartType(inv.getPartType());
            item.setUnit(inv.getUnit());
            item.setUnitPrice(inv.getUnitPrice());
            item.setSystemQuantity(inv.getQuantity());
            item.setActualQuantity(0);
            item.setDiffQuantity(-inv.getQuantity());
            item.setDiffType(DiffTypeEnum.LOSS.getCode());
            if (inv.getUnitPrice() != null) {
                item.setDiffAmount(inv.getUnitPrice().multiply(BigDecimal.valueOf(inv.getQuantity())));
            }
            item.setStorageLocation(inv.getStorageLocation());
            items.add(item);
        }

        for (SparePartStocktakeItem item : items) {
            stocktakeItemMapper.insert(item);
        }

        updateStocktakeStats(stocktakeId);
    }

    private void updateStocktakeStats(Long stocktakeId) {
        List<SparePartStocktakeItem> items = stocktakeItemMapper.selectList(
                new LambdaQueryWrapper<SparePartStocktakeItem>()
                        .eq(SparePartStocktakeItem::getStocktakeId, stocktakeId)
                        .eq(SparePartStocktakeItem::getDeleted, 0)
        );

        int totalCount = items.size();
        int diffCount = 0;
        int profitQuantity = 0;
        int lossQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal diffAmount = BigDecimal.ZERO;

        for (SparePartStocktakeItem item : items) {
            if (item.getSystemQuantity() > 0 && item.getUnitPrice() != null) {
                totalAmount = totalAmount.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getSystemQuantity())));
            }

            if (item.getDiffType() != null && item.getDiffType() > 0) {
                diffCount++;
            }

            if (DiffTypeEnum.PROFIT.getCode().equals(item.getDiffType())) {
                profitQuantity += item.getDiffQuantity();
                if (item.getDiffAmount() != null) {
                    diffAmount = diffAmount.add(item.getDiffAmount());
                }
            } else if (DiffTypeEnum.LOSS.getCode().equals(item.getDiffType())) {
                lossQuantity += Math.abs(item.getDiffQuantity());
                if (item.getDiffAmount() != null) {
                    diffAmount = diffAmount.add(item.getDiffAmount());
                }
            }
        }

        SparePartStocktake stocktake = new SparePartStocktake();
        stocktake.setId(stocktakeId);
        stocktake.setTotalCount(totalCount);
        stocktake.setDiffCount(diffCount);
        stocktake.setProfitQuantity(profitQuantity);
        stocktake.setLossQuantity(lossQuantity);
        stocktake.setTotalAmount(totalAmount);
        stocktake.setDiffAmount(diffAmount);
        updateById(stocktake);
    }

    private void updateInventoryWarnStatus(SparePartInventory inventory) {
        if (inventory.getQuantity() == null || inventory.getSafeQuantity() == null) {
            return;
        }
        if (inventory.getQuantity() <= 0) {
            inventory.setWarnStatus(SparePartWarnStatusEnum.INSUFFICIENT.getCode());
        } else if (inventory.getQuantity() < inventory.getSafeQuantity()) {
            if (inventory.getQuantity() < inventory.getSafeQuantity() / 2) {
                inventory.setWarnStatus(SparePartWarnStatusEnum.INSUFFICIENT.getCode());
            } else {
                inventory.setWarnStatus(SparePartWarnStatusEnum.LOW_WARN.getCode());
            }
        } else {
            inventory.setWarnStatus(SparePartWarnStatusEnum.NORMAL.getCode());
        }
    }

    private String generateStocktakeNo() {
        String prefix = "PD";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String codePrefix = prefix + dateStr;

        LambdaQueryWrapper<SparePartStocktake> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SparePartStocktake::getStocktakeNo, codePrefix);
        wrapper.orderByDesc(SparePartStocktake::getStocktakeNo);
        wrapper.last("LIMIT 1");
        SparePartStocktake lastStocktake = getOne(wrapper);

        int sequence = 1;
        if (lastStocktake != null && lastStocktake.getStocktakeNo() != null) {
            String lastNo = lastStocktake.getStocktakeNo();
            String seqStr = lastNo.substring(codePrefix.length());
            try {
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }

        return codePrefix + String.format("%04d", sequence);
    }

    private String generateRecordNo(String type) {
        String prefix = type;
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String codePrefix = prefix + "-" + dateStr;

        LambdaQueryWrapper<SparePartInOutRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SparePartInOutRecord::getRecordNo, codePrefix);
        wrapper.orderByDesc(SparePartInOutRecord::getRecordNo);
        wrapper.last("LIMIT 1");
        SparePartInOutRecord lastRecord = inOutRecordMapper.selectOne(wrapper);

        int sequence = 1;
        if (lastRecord != null && lastRecord.getRecordNo() != null) {
            String lastNo = lastRecord.getRecordNo();
            String seqStr = lastNo.substring(codePrefix.length());
            try {
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }

        return codePrefix + String.format("%03d", sequence);
    }
}
