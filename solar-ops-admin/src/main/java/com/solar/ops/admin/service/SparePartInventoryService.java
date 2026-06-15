package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.dto.SparePartInboundDTO;
import com.solar.ops.admin.dto.SparePartInventoryQueryDTO;
import com.solar.ops.admin.dto.SparePartOutboundDTO;
import com.solar.ops.admin.entity.SparePartInOutRecord;
import com.solar.ops.admin.entity.SparePartInventory;
import com.solar.ops.admin.enums.SparePartInOutTypeEnum;
import com.solar.ops.admin.enums.SparePartRecordTypeEnum;
import com.solar.ops.admin.enums.SparePartWarnStatusEnum;
import com.solar.ops.admin.excel.SparePartInventoryExcelVO;
import com.solar.ops.admin.mapper.SparePartInOutRecordMapper;
import com.solar.ops.admin.mapper.SparePartInventoryMapper;
import com.solar.ops.admin.util.QrCodeUtil;
import com.solar.ops.admin.vo.*;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SparePartInventoryService extends ServiceImpl<SparePartInventoryMapper, SparePartInventory> {

    @Resource
    private QrCodeUtil qrCodeUtil;

    @Resource
    private SparePartInOutRecordMapper inOutRecordMapper;

    public PageResult<SparePartInventoryVO> page(PageQuery pageQuery, SparePartInventoryQueryDTO queryDTO) {
        Page<SparePartInventoryVO> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        baseMapper.selectInventoryPage(page, queryDTO);
        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public SparePartInventoryVO getDetailById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return baseMapper.selectInventoryDetailById(id);
    }

    public SparePartInventory getByCode(String partCode) {
        if (!StringUtils.hasText(partCode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return getOne(new LambdaQueryWrapper<SparePartInventory>()
                .eq(SparePartInventory::getPartCode, partCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public void addInventory(SparePartInventory inventory) {
        String partCode = generatePartCode();
        inventory.setPartCode(partCode);

        SparePart existInventory = getOne(new LambdaQueryWrapper<SparePartInventory>()
                .eq(SparePartInventory::getPartCode, partCode));
        if (existInventory != null) {
            throw new BusinessException("备件编号已存在");
        }

        String qrCodeUrl = qrCodeUtil.generateQrCodeFile(partCode, partCode);
        inventory.setQrCodeUrl(qrCodeUrl);

        if (inventory.getQuantity() == null) {
            inventory.setQuantity(0);
        }
        if (inventory.getSafeQuantity() == null) {
            inventory.setSafeQuantity(10);
        }
        if (inventory.getStatus() == null) {
            inventory.setStatus(1);
        }
        if (inventory.getUnit() == null) {
            inventory.setUnit("个");
        }

        updateWarnStatus(inventory);
        save(inventory);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateInventory(SparePartInventory inventory) {
        if (inventory.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        SparePart existInventory = getById(inventory.getId());
        if (existInventory == null) {
            throw new BusinessException("备件不存在");
        }

        if (StringUtils.hasText(inventory.getPartCode()) && !inventory.getPartCode().equals(existInventory.getPartCode())) {
            SparePart sameCodeInventory = getOne(new LambdaQueryWrapper<SparePartInventory>()
                    .eq(SparePartInventory::getPartCode, inventory.getPartCode()));
            if (sameCodeInventory != null) {
                throw new BusinessException("备件编号已存在");
            }
        }

        updateWarnStatus(inventory);
        updateById(inventory);
    }

    public void deleteInventory(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        removeById(id);
    }

    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        removeByIds(ids);
    }

    @Transactional(rollbackFor = Exception.class)
    public void inbound(SparePartInboundDTO inboundDTO) {
        if (inboundDTO.getPartId() == null || inboundDTO.getQuantity() == null || inboundDTO.getQuantity() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        if (!SparePartInOutTypeEnum.isInType(inboundDTO.getInOutType())) {
            throw new BusinessException("入库类型不正确");
        }

        SparePartInventory inventory = getById(inboundDTO.getPartId());
        if (inventory == null) {
            throw new BusinessException("备件不存在");
        }

        int beforeQuantity = inventory.getQuantity();
        int afterQuantity = beforeQuantity + inboundDTO.getQuantity();

        inventory.setQuantity(afterQuantity);
        updateWarnStatus(inventory);
        updateById(inventory);

        String recordNo = generateRecordNo("IN");
        SparePartInOutRecord record = new SparePartInOutRecord();
        record.setRecordNo(recordNo);
        record.setRecordType(SparePartRecordTypeEnum.IN.getCode());
        record.setInOutType(inboundDTO.getInOutType());
        record.setPartId(inventory.getId());
        record.setPartCode(inventory.getPartCode());
        record.setPartName(inventory.getPartName());
        record.setPartModel(inventory.getPartModel());
        record.setQuantity(inboundDTO.getQuantity());
        record.setUnitPrice(inboundDTO.getUnitPrice() != null ? inboundDTO.getUnitPrice() : inventory.getUnitPrice());
        if (record.getUnitPrice() != null) {
            record.setTotalPrice(record.getUnitPrice().multiply(BigDecimal.valueOf(inboundDTO.getQuantity())));
        }
        record.setBeforeQuantity(beforeQuantity);
        record.setAfterQuantity(afterQuantity);
        record.setSupplier(inboundDTO.getSupplier());
        record.setOperatorName(inboundDTO.getOperatorName());
        record.setOperateTime(inboundDTO.getOperateTime() != null ? inboundDTO.getOperateTime() : LocalDateTime.now());
        record.setStorageLocation(inventory.getStorageLocation());
        record.setBatchNo(inboundDTO.getBatchNo());
        record.setRemark(inboundDTO.getRemark());
        inOutRecordMapper.insert(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public void outbound(SparePartOutboundDTO outboundDTO) {
        if (outboundDTO.getPartId() == null || outboundDTO.getQuantity() == null || outboundDTO.getQuantity() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        if (!SparePartInOutTypeEnum.isOutType(outboundDTO.getInOutType())) {
            throw new BusinessException("出库类型不正确");
        }

        SparePartInventory inventory = getById(outboundDTO.getPartId());
        if (inventory == null) {
            throw new BusinessException("备件不存在");
        }

        if (inventory.getQuantity() < outboundDTO.getQuantity()) {
            throw new BusinessException("库存不足，当前库存：" + inventory.getQuantity());
        }

        int beforeQuantity = inventory.getQuantity();
        int afterQuantity = beforeQuantity - outboundDTO.getQuantity();

        inventory.setQuantity(afterQuantity);
        updateWarnStatus(inventory);
        updateById(inventory);

        String recordNo = generateRecordNo("OUT");
        SparePartInOutRecord record = new SparePartInOutRecord();
        record.setRecordNo(recordNo);
        record.setRecordType(SparePartRecordTypeEnum.OUT.getCode());
        record.setInOutType(outboundDTO.getInOutType());
        record.setPartId(inventory.getId());
        record.setPartCode(inventory.getPartCode());
        record.setPartName(inventory.getPartName());
        record.setPartModel(inventory.getPartModel());
        record.setQuantity(outboundDTO.getQuantity());
        record.setUnitPrice(inventory.getUnitPrice());
        if (inventory.getUnitPrice() != null) {
            record.setTotalPrice(inventory.getUnitPrice().multiply(BigDecimal.valueOf(outboundDTO.getQuantity())));
        }
        record.setBeforeQuantity(beforeQuantity);
        record.setAfterQuantity(afterQuantity);
        record.setWorkOrderId(outboundDTO.getWorkOrderId());
        record.setWorkOrderNo(outboundDTO.getWorkOrderNo());
        record.setMaintenanceRecordId(outboundDTO.getMaintenanceRecordId());
        record.setAssetId(outboundDTO.getAssetId());
        record.setOperatorName(outboundDTO.getOperatorName());
        record.setOperateTime(outboundDTO.getOperateTime() != null ? outboundDTO.getOperateTime() : LocalDateTime.now());
        record.setStorageLocation(inventory.getStorageLocation());
        record.setRemark(outboundDTO.getRemark());
        inOutRecordMapper.insert(record);
    }

    public String getQrCodeBase64(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        SparePartInventory inventory = getById(id);
        if (inventory == null) {
            throw new BusinessException("备件不存在");
        }
        return qrCodeUtil.generateQrCodeBase64(inventory.getPartCode(), 300, 300);
    }

    public List<String> batchGenerateQrCode(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        List<String> base64List = new ArrayList<>();
        for (Long id : ids) {
            SparePartInventory inventory = getById(id);
            if (inventory != null) {
                String base64 = qrCodeUtil.generateQrCodeBase64(inventory.getPartCode(), 300, 300);
                base64List.add(base64);
            }
        }
        return base64List;
    }

    public SparePartInventoryVO scanQuery(String partCode) {
        if (!StringUtils.hasText(partCode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        SparePartInventory inventory = getByCode(partCode);
        if (inventory == null) {
            return null;
        }
        return getDetailById(inventory.getId());
    }

    public InventoryDashboardVO getDashboard() {
        InventoryDashboardVO dashboard = new InventoryDashboardVO();

        List<SparePartInventory> allInventory = list(new LambdaQueryWrapper<SparePartInventory>()
                .eq(SparePartInventory::getStatus, 1)
                .eq(SparePartInventory::getDeleted, 0));

        dashboard.setTotalSkuCount(allInventory.size());
        dashboard.setTotalQuantity(allInventory.stream().mapToInt(SparePartInventory::getQuantity).sum());
        BigDecimal totalAmount = allInventory.stream()
                .filter(i -> i.getUnitPrice() != null)
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTotalAmount(totalAmount);

        long lowWarnCount = allInventory.stream()
                .filter(i -> SparePartWarnStatusEnum.LOW_WARN.getCode().equals(i.getWarnStatus()))
                .count();
        dashboard.setLowWarnCount((int) lowWarnCount);

        long insufficientCount = allInventory.stream()
                .filter(i -> SparePartWarnStatusEnum.INSUFFICIENT.getCode().equals(i.getWarnStatus()))
                .count();
        dashboard.setInsufficientCount((int) insufficientCount);

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(23, 59, 59);

        LambdaQueryWrapper<SparePartInOutRecord> inWrapper = new LambdaQueryWrapper<>();
        inWrapper.eq(SparePartInOutRecord::getRecordType, SparePartRecordTypeEnum.IN.getCode());
        inWrapper.between(SparePartInOutRecord::getOperateTime, dayStart, dayEnd);
        inWrapper.eq(SparePartInOutRecord::getDeleted, 0);
        List<SparePartInOutRecord> todayInRecords = inOutRecordMapper.selectList(inWrapper);
        dashboard.setTodayInboundCount(todayInRecords.stream().mapToInt(SparePartInOutRecord::getQuantity).sum());

        LambdaQueryWrapper<SparePartInOutRecord> outWrapper = new LambdaQueryWrapper<>();
        outWrapper.eq(SparePartInOutRecord::getRecordType, SparePartRecordTypeEnum.OUT.getCode());
        outWrapper.between(SparePartInOutRecord::getOperateTime, dayStart, dayEnd);
        outWrapper.eq(SparePartInOutRecord::getDeleted, 0);
        List<SparePartInOutRecord> todayOutRecords = inOutRecordMapper.selectList(outWrapper);
        dashboard.setTodayOutboundCount(todayOutRecords.stream().mapToInt(SparePartInOutRecord::getQuantity).sum());

        List<InventoryByTypeVO> typeStats = new ArrayList<>();
        String[] types = {"fan", "capacitor", "board", "other"};
        for (String type : types) {
            List<SparePartInventory> typeList = allInventory.stream()
                    .filter(i -> type.equals(i.getPartType()))
                    .collect(Collectors.toList());
            InventoryByTypeVO typeVO = new InventoryByTypeVO();
            typeVO.setPartType(type);
            typeVO.setPartTypeDesc(getPartTypeDesc(type));
            typeVO.setSkuCount(typeList.size());
            typeVO.setQuantity(typeList.stream().mapToInt(SparePartInventory::getQuantity).sum());
            BigDecimal typeAmount = typeList.stream()
                    .filter(i -> i.getUnitPrice() != null)
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            typeVO.setAmount(typeAmount);
            typeStats.add(typeVO);
        }
        dashboard.setTypeStats(typeStats);

        Page<SparePartInventoryVO> warnPage = new Page<>(1, 10);
        SparePartInventoryQueryDTO warnQuery = new SparePartInventoryQueryDTO();
        baseMapper.selectInventoryPage(warnPage, warnQuery);

        List<SparePartInventoryVO> warnParts = warnPage.getRecords().stream()
                .filter(v -> v.getWarnStatus() != null && v.getWarnStatus() > 0)
                .limit(10)
                .collect(Collectors.toList());
        dashboard.setWarnParts(warnParts);

        return dashboard;
    }

    public List<SparePartInventoryExcelVO> exportList(SparePartInventoryQueryDTO queryDTO) {
        Page<SparePartInventoryVO> page = new Page<>(1, Integer.MAX_VALUE);
        List<SparePartInventoryVO> list = baseMapper.selectInventoryPage(page, queryDTO).getRecords();
        return list.stream().map(this::convertToExcelVO).collect(Collectors.toList());
    }

    private SparePartInventoryExcelVO convertToExcelVO(SparePartInventoryVO vo) {
        SparePartInventoryExcelVO excelVO = new SparePartInventoryExcelVO();
        excelVO.setPartCode(vo.getPartCode());
        excelVO.setPartName(vo.getPartName());
        excelVO.setPartTypeDesc(vo.getPartTypeDesc());
        excelVO.setPartModel(vo.getPartModel());
        excelVO.setBrand(vo.getBrand());
        excelVO.setSpecification(vo.getSpecification());
        excelVO.setUnit(vo.getUnit());
        excelVO.setUnitPrice(vo.getUnitPrice());
        excelVO.setQuantity(vo.getQuantity());
        excelVO.setSafeQuantity(vo.getSafeQuantity());
        excelVO.setTotalAmount(vo.getTotalAmount());
        excelVO.setStorageLocation(vo.getStorageLocation());
        excelVO.setWarehouse(vo.getWarehouse());
        excelVO.setSupplier(vo.getSupplier());
        excelVO.setWarnStatusDesc(vo.getWarnStatusDesc());
        excelVO.setRemark(vo.getRemark());
        return excelVO;
    }

    private void updateWarnStatus(SparePartInventory inventory) {
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

    private String generatePartCode() {
        String prefix = "SP";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String codePrefix = prefix + dateStr;

        LambdaQueryWrapper<SparePartInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SparePartInventory::getPartCode, codePrefix);
        wrapper.orderByDesc(SparePartInventory::getPartCode);
        wrapper.last("LIMIT 1");
        SparePartInventory lastInventory = getOne(wrapper);

        int sequence = 1;
        if (lastInventory != null && StringUtils.hasText(lastInventory.getPartCode())) {
            String lastCode = lastInventory.getPartCode();
            String seqStr = lastCode.substring(codePrefix.length());
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
        if (lastRecord != null && StringUtils.hasText(lastRecord.getRecordNo())) {
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

    private String getPartTypeDesc(String type) {
        if (type == null) {
            return "未知";
        }
        switch (type) {
            case "fan":
                return "风扇";
            case "capacitor":
                return "电容";
            case "board":
                return "板卡";
            case "other":
                return "其他";
            default:
                return "未知";
        }
    }
}
