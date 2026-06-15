package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.dto.PurchaseSuggestionQueryDTO;
import com.solar.ops.admin.entity.SparePartInventory;
import com.solar.ops.admin.entity.SparePartPurchaseSuggestion;
import com.solar.ops.admin.enums.*;
import com.solar.ops.admin.mapper.SparePartInventoryMapper;
import com.solar.ops.admin.mapper.SparePartPurchaseSuggestionMapper;
import com.solar.ops.admin.vo.SparePartPurchaseSuggestionVO;
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

@Service
public class SparePartPurchaseSuggestionService extends ServiceImpl<SparePartPurchaseSuggestionMapper, SparePartPurchaseSuggestion> {

    @Resource
    private SparePartInventoryMapper inventoryMapper;

    public PageResult<SparePartPurchaseSuggestionVO> page(PageQuery pageQuery, PurchaseSuggestionQueryDTO queryDTO) {
        Page<SparePartPurchaseSuggestionVO> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        baseMapper.selectSuggestionPage(page, queryDTO);
        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public SparePartPurchaseSuggestionVO getDetailById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        Page<SparePartPurchaseSuggestionVO> page = new Page<>(1, 1);
        PurchaseSuggestionQueryDTO queryDTO = new PurchaseSuggestionQueryDTO();
        List<SparePartPurchaseSuggestionVO> list = baseMapper.selectSuggestionPage(page, queryDTO).getRecords();
        return list.stream().findFirst().orElse(null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void generateSuggestions() {
        List<SparePartInventory> lowStockParts = inventoryMapper.selectList(
                new LambdaQueryWrapper<SparePartInventory>()
                        .eq(SparePartInventory::getStatus, 1)
                        .eq(SparePartInventory::getDeleted, 0)
                        .in(SparePartInventory::getWarnStatus,
                                SparePartWarnStatusEnum.LOW_WARN.getCode(),
                                SparePartWarnStatusEnum.INSUFFICIENT.getCode())
        );

        LocalDate today = LocalDate.now();
        for (SparePartInventory inventory : lowStockParts) {
            LambdaQueryWrapper<SparePartPurchaseSuggestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SparePartPurchaseSuggestion::getPartId, inventory.getId());
            wrapper.eq(SparePartPurchaseSuggestion::getStatus, PurchaseSuggestionStatusEnum.PENDING.getCode());
            wrapper.eq(SparePartPurchaseSuggestion::getDeleted, 0);
            SparePartPurchaseSuggestion existSuggestion = getOne(wrapper);

            if (existSuggestion != null) {
                continue;
            }

            int safeQuantity = inventory.getSafeQuantity() != null ? inventory.getSafeQuantity() : 10;
            int currentQuantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            int minPurchaseQuantity = inventory.getMinPurchaseQuantity() != null ? inventory.getMinPurchaseQuantity() : 10;

            int suggestQuantity = safeQuantity * 2 - currentQuantity;
            if (suggestQuantity < minPurchaseQuantity) {
                suggestQuantity = minPurchaseQuantity;
            }

            int urgency = UrgencyEnum.NORMAL.getCode();
            if (SparePartWarnStatusEnum.INSUFFICIENT.getCode().equals(inventory.getWarnStatus())) {
                if (currentQuantity <= 0) {
                    urgency = UrgencyEnum.VERY_URGENT.getCode();
                } else {
                    urgency = UrgencyEnum.URGENT.getCode();
                }
            } else if (SparePartWarnStatusEnum.LOW_WARN.getCode().equals(inventory.getWarnStatus())) {
                urgency = UrgencyEnum.NORMAL.getCode();
            }

            SparePartPurchaseSuggestion suggestion = new SparePartPurchaseSuggestion();
            suggestion.setSuggestionNo(generateSuggestionNo());
            suggestion.setPartId(inventory.getId());
            suggestion.setPartCode(inventory.getPartCode());
            suggestion.setPartName(inventory.getPartName());
            suggestion.setPartModel(inventory.getPartModel());
            suggestion.setPartType(inventory.getPartType());
            suggestion.setCurrentQuantity(currentQuantity);
            suggestion.setSafeQuantity(safeQuantity);
            suggestion.setSuggestQuantity(suggestQuantity);
            suggestion.setMinPurchaseQuantity(minPurchaseQuantity);
            suggestion.setUnitPrice(inventory.getUnitPrice());
            if (inventory.getUnitPrice() != null) {
                suggestion.setEstimatedAmount(inventory.getUnitPrice().multiply(BigDecimal.valueOf(suggestQuantity)));
            }
            suggestion.setSupplier(inventory.getSupplier());
            suggestion.setUrgency(urgency);
            suggestion.setStatus(PurchaseSuggestionStatusEnum.PENDING.getCode());
            suggestion.setGenerateTime(LocalDateTime.now());
            suggestion.setRemark("系统自动生成的采购建议");
            save(suggestion);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processSuggestion(Long id, Integer status, String processorName, String remark) {
        if (id == null || status == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        SparePartPurchaseSuggestion suggestion = getById(id);
        if (suggestion == null) {
            throw new BusinessException("采购建议不存在");
        }
        if (!PurchaseSuggestionStatusEnum.PENDING.getCode().equals(suggestion.getStatus())) {
            throw new BusinessException("该建议已处理，无法重复处理");
        }

        suggestion.setStatus(status);
        suggestion.setProcessorName(processorName);
        suggestion.setProcessTime(LocalDateTime.now());
        if (remark != null) {
            suggestion.setRemark(remark);
        }
        updateById(suggestion);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchProcess(List<Long> ids, Integer status, String processorName) {
        if (ids == null || ids.isEmpty() || status == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        for (Long id : ids) {
            processSuggestion(id, status, processorName, null);
        }
    }

    public long getPendingCount() {
        return count(new LambdaQueryWrapper<SparePartPurchaseSuggestion>()
                .eq(SparePartPurchaseSuggestion::getStatus, PurchaseSuggestionStatusEnum.PENDING.getCode())
                .eq(SparePartPurchaseSuggestion::getDeleted, 0));
    }

    private String generateSuggestionNo() {
        String prefix = "CG";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String codePrefix = prefix + dateStr;

        LambdaQueryWrapper<SparePartPurchaseSuggestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SparePartPurchaseSuggestion::getSuggestionNo, codePrefix);
        wrapper.orderByDesc(SparePartPurchaseSuggestion::getSuggestionNo);
        wrapper.last("LIMIT 1");
        SparePartPurchaseSuggestion lastSuggestion = getOne(wrapper);

        int sequence = 1;
        if (lastSuggestion != null && lastSuggestion.getSuggestionNo() != null) {
            String lastNo = lastSuggestion.getSuggestionNo();
            String seqStr = lastNo.substring(codePrefix.length());
            try {
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }

        return codePrefix + String.format("%04d", sequence);
    }
}
