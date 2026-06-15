package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.dto.SparePartInOutRecordQueryDTO;
import com.solar.ops.admin.entity.SparePartInOutRecord;
import com.solar.ops.admin.excel.SparePartInOutRecordExcelVO;
import com.solar.ops.admin.mapper.SparePartInOutRecordMapper;
import com.solar.ops.admin.vo.SparePartInOutRecordVO;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SparePartInOutRecordService extends ServiceImpl<SparePartInOutRecordMapper, SparePartInOutRecord> {

    public PageResult<SparePartInOutRecordVO> page(PageQuery pageQuery, SparePartInOutRecordQueryDTO queryDTO) {
        Page<SparePartInOutRecordVO> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        baseMapper.selectRecordPage(page, queryDTO);
        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public SparePartInOutRecordVO getDetailById(Long id) {
        if (id == null) {
            return null;
        }
        SparePartInOutRecord record = getById(id);
        if (record == null) {
            return null;
        }
        return convertToVO(record);
    }

    public List<SparePartInOutRecordExcelVO> exportList(SparePartInOutRecordQueryDTO queryDTO) {
        Page<SparePartInOutRecordVO> page = new Page<>(1, Integer.MAX_VALUE);
        List<SparePartInOutRecordVO> list = baseMapper.selectRecordPage(page, queryDTO).getRecords();
        return list.stream().map(this::convertToExcelVO).collect(Collectors.toList());
    }

    private SparePartInOutRecordVO convertToVO(SparePartInOutRecord record) {
        SparePartInOutRecordVO vo = new SparePartInOutRecordVO();
        vo.setId(record.getId());
        vo.setRecordNo(record.getRecordNo());
        vo.setRecordType(record.getRecordType());
        vo.setRecordTypeDesc(getRecordTypeDesc(record.getRecordType()));
        vo.setInOutType(record.getInOutType());
        vo.setInOutTypeDesc(getInOutTypeDesc(record.getInOutType()));
        vo.setPartId(record.getPartId());
        vo.setPartCode(record.getPartCode());
        vo.setPartName(record.getPartName());
        vo.setPartModel(record.getPartModel());
        vo.setQuantity(record.getQuantity());
        vo.setUnitPrice(record.getUnitPrice());
        vo.setTotalPrice(record.getTotalPrice());
        vo.setBeforeQuantity(record.getBeforeQuantity());
        vo.setAfterQuantity(record.getAfterQuantity());
        vo.setWorkOrderId(record.getWorkOrderId());
        vo.setWorkOrderNo(record.getWorkOrderNo());
        vo.setMaintenanceRecordId(record.getMaintenanceRecordId());
        vo.setAssetId(record.getAssetId());
        vo.setSupplier(record.getSupplier());
        vo.setOperatorId(record.getOperatorId());
        vo.setOperatorName(record.getOperatorName());
        vo.setOperateTime(record.getOperateTime());
        vo.setStorageLocation(record.getStorageLocation());
        vo.setBatchNo(record.getBatchNo());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private SparePartInOutRecordExcelVO convertToExcelVO(SparePartInOutRecordVO vo) {
        SparePartInOutRecordExcelVO excelVO = new SparePartInOutRecordExcelVO();
        excelVO.setRecordNo(vo.getRecordNo());
        excelVO.setRecordTypeDesc(vo.getRecordTypeDesc());
        excelVO.setInOutTypeDesc(vo.getInOutTypeDesc());
        excelVO.setPartCode(vo.getPartCode());
        excelVO.setPartName(vo.getPartName());
        excelVO.setPartModel(vo.getPartModel());
        excelVO.setQuantity(vo.getQuantity());
        excelVO.setUnitPrice(vo.getUnitPrice());
        excelVO.setTotalPrice(vo.getTotalPrice());
        excelVO.setBeforeQuantity(vo.getBeforeQuantity());
        excelVO.setAfterQuantity(vo.getAfterQuantity());
        excelVO.setWorkOrderNo(vo.getWorkOrderNo());
        excelVO.setOperatorName(vo.getOperatorName());
        excelVO.setOperateTime(vo.getOperateTime());
        excelVO.setStorageLocation(vo.getStorageLocation());
        excelVO.setRemark(vo.getRemark());
        return excelVO;
    }

    private String getRecordTypeDesc(Integer type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case 1:
                return "入库";
            case 2:
                return "出库";
            default:
                return "未知";
        }
    }

    private String getInOutTypeDesc(Integer type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case 11:
                return "采购入库";
            case 12:
                return "盘盈入库";
            case 13:
                return "退库入库";
            case 21:
                return "工单出库";
            case 22:
                return "盘亏出库";
            case 23:
                return "报废出库";
            default:
                return "未知";
        }
    }
}
