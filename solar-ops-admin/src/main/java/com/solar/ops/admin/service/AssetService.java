package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.dto.AssetQueryDTO;
import com.solar.ops.admin.entity.Asset;
import com.solar.ops.admin.enums.AssetStatusEnum;
import com.solar.ops.admin.excel.AssetExcelVO;
import com.solar.ops.admin.mapper.AssetMapper;
import com.solar.ops.admin.util.QrCodeUtil;
import com.solar.ops.admin.vo.AssetDetailVO;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssetService extends ServiceImpl<AssetMapper, Asset> {

    @Resource
    private QrCodeUtil qrCodeUtil;

    public PageResult<AssetDetailVO> page(PageQuery pageQuery, AssetQueryDTO queryDTO) {
        Page<AssetDetailVO> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        baseMapper.selectAssetPage(page, queryDTO);
        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public AssetDetailVO getAssetDetailById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return baseMapper.selectAssetDetailById(id);
    }

    public Asset getAssetByCode(String assetCode) {
        if (!StringUtils.hasText(assetCode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return getOne(new LambdaQueryWrapper<Asset>()
                .eq(Asset::getAssetCode, assetCode));
    }

    public void addAsset(Asset asset) {
        String assetCode = generateAssetCode();
        asset.setAssetCode(assetCode);

        Asset existAsset = getOne(new LambdaQueryWrapper<Asset>()
                .eq(Asset::getAssetCode, assetCode));
        if (existAsset != null) {
            throw new BusinessException("资产编号已存在");
        }

        String qrCodeUrl = qrCodeUtil.generateQrCodeFile(assetCode, assetCode);
        asset.setQrCodeUrl(qrCodeUrl);

        if (asset.getAssetStatus() == null) {
            asset.setAssetStatus(AssetStatusEnum.NORMAL.getCode());
        }

        save(asset);
    }

    public void updateAsset(Asset asset) {
        if (asset.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        Asset existAsset = getById(asset.getId());
        if (existAsset == null) {
            throw new BusinessException("资产不存在");
        }

        if (StringUtils.hasText(asset.getAssetCode()) && !asset.getAssetCode().equals(existAsset.getAssetCode())) {
            Asset sameCodeAsset = getOne(new LambdaQueryWrapper<Asset>()
                    .eq(Asset::getAssetCode, asset.getAssetCode()));
            if (sameCodeAsset != null) {
                throw new BusinessException("资产编号已存在");
            }
        }

        updateById(asset);
    }

    public void deleteAsset(Long id) {
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

    public void retireAsset(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        Asset asset = getById(id);
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }
        asset.setAssetStatus(AssetStatusEnum.RETIRED.getCode());
        updateById(asset);
    }

    public void scrapAsset(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        Asset asset = getById(id);
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }
        asset.setAssetStatus(AssetStatusEnum.SCRAPPED.getCode());
        updateById(asset);
    }

    private String generateAssetCode() {
        String prefix = "ZC";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String codePrefix = prefix + dateStr;

        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Asset::getAssetCode, codePrefix);
        wrapper.orderByDesc(Asset::getAssetCode);
        wrapper.last("LIMIT 1");
        Asset lastAsset = getOne(wrapper);

        int sequence = 1;
        if (lastAsset != null && StringUtils.hasText(lastAsset.getAssetCode())) {
            String lastCode = lastAsset.getAssetCode();
            String seqStr = lastCode.substring(codePrefix.length());
            try {
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }

        return codePrefix + String.format("%04d", sequence);
    }

    public String getQrCodeBase64(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        Asset asset = getById(id);
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }
        return qrCodeUtil.generateQrCodeBase64(asset.getAssetCode(), 300, 300);
    }

    public List<String> batchGenerateQrCode(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        List<String> base64List = new ArrayList<>();
        for (Long id : ids) {
            Asset asset = getById(id);
            if (asset != null) {
                String base64 = qrCodeUtil.generateQrCodeBase64(asset.getAssetCode(), 300, 300);
                base64List.add(base64);
            }
        }
        return base64List;
    }

    public List<AssetExcelVO> exportAssetList(AssetQueryDTO queryDTO) {
        List<AssetDetailVO> list = baseMapper.selectAssetPage(new Page<>(1, Integer.MAX_VALUE), queryDTO).getRecords();
        return list.stream().map(this::convertToExcelVO).collect(Collectors.toList());
    }

    private AssetExcelVO convertToExcelVO(AssetDetailVO detailVO) {
        AssetExcelVO excelVO = new AssetExcelVO();
        excelVO.setAssetCode(detailVO.getAssetCode());
        excelVO.setAssetName(detailVO.getAssetName());
        excelVO.setAssetTypeDesc(detailVO.getAssetTypeDesc());
        excelVO.setStationName(detailVO.getStationName());
        excelVO.setDeviceSn(detailVO.getDeviceSn());
        excelVO.setDeviceModel(detailVO.getDeviceModel());
        excelVO.setBrand(detailVO.getBrand());
        excelVO.setSpecification(detailVO.getSpecification());
        excelVO.setCapacity(detailVO.getCapacity());
        excelVO.setInstallDate(detailVO.getInstallDate());
        excelVO.setWarrantyEndDate(detailVO.getWarrantyEndDate());
        excelVO.setWarrantyMonths(detailVO.getWarrantyMonths());
        excelVO.setSupplier(detailVO.getSupplier());
        excelVO.setManufacturer(detailVO.getManufacturer());
        excelVO.setInstallLocation(detailVO.getInstallLocation());
        excelVO.setPurchaseAmount(detailVO.getPurchaseAmount());
        excelVO.setResponsiblePerson(detailVO.getResponsiblePerson());
        excelVO.setAssetStatusDesc(detailVO.getAssetStatusDesc());
        excelVO.setRemark(detailVO.getRemark());
        return excelVO;
    }

    public void importAsset(List<AssetExcelVO> list) {
        if (list == null || list.isEmpty()) {
            throw new BusinessException("导入数据不能为空");
        }
        for (AssetExcelVO excelVO : list) {
            Asset asset = new Asset();
            asset.setAssetName(excelVO.getAssetName());
            asset.setDeviceSn(excelVO.getDeviceSn());
            asset.setDeviceModel(excelVO.getDeviceModel());
            asset.setBrand(excelVO.getBrand());
            asset.setSpecification(excelVO.getSpecification());
            asset.setCapacity(excelVO.getCapacity());
            asset.setInstallDate(excelVO.getInstallDate());
            asset.setWarrantyEndDate(excelVO.getWarrantyEndDate());
            asset.setWarrantyMonths(excelVO.getWarrantyMonths());
            asset.setSupplier(excelVO.getSupplier());
            asset.setManufacturer(excelVO.getManufacturer());
            asset.setInstallLocation(excelVO.getInstallLocation());
            asset.setPurchaseAmount(excelVO.getPurchaseAmount());
            asset.setResponsiblePerson(excelVO.getResponsiblePerson());
            asset.setRemark(excelVO.getRemark());

            String assetCode = generateAssetCode();
            asset.setAssetCode(assetCode);

            String qrCodeUrl = qrCodeUtil.generateQrCodeFile(assetCode, assetCode);
            asset.setQrCodeUrl(qrCodeUrl);

            asset.setAssetStatus(AssetStatusEnum.NORMAL.getCode());

            save(asset);
        }
    }
}
