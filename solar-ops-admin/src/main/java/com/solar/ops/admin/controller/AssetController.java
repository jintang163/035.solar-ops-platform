package com.solar.ops.admin.controller;

import com.alibaba.excel.EasyExcel;
import com.solar.ops.admin.dto.AssetQueryDTO;
import com.solar.ops.admin.entity.Asset;
import com.solar.ops.admin.excel.AssetExcelVO;
import com.solar.ops.admin.service.AssetService;
import com.solar.ops.admin.vo.AssetDetailVO;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
@Api(tags = "资产管理")
public class AssetController {

    @Resource
    private AssetService assetService;

    @GetMapping
    @ApiOperation(value = "分页查询资产列表")
    public Result<PageResult<AssetDetailVO>> page(@ApiParam(value = "分页参数") PageQuery pageQuery,
                                                   @ApiParam(value = "查询条件") AssetQueryDTO queryDTO) {
        PageResult<AssetDetailVO> pageResult = assetService.page(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据ID查询资产详情")
    public Result<AssetDetailVO> getById(@ApiParam(value = "资产ID") @PathVariable Long id) {
        AssetDetailVO detailVO = assetService.getAssetDetailById(id);
        return Result.success(detailVO);
    }

    @GetMapping("/code/{assetCode}")
    @ApiOperation(value = "根据资产编号查询资产")
    public Result<Asset> getByCode(@ApiParam(value = "资产编号") @PathVariable String assetCode) {
        Asset asset = assetService.getAssetByCode(assetCode);
        return Result.success(asset);
    }

    @PostMapping
    @ApiOperation(value = "新增资产")
    public Result<Void> add(@RequestBody Asset asset) {
        assetService.addAsset(asset);
        return Result.success();
    }

    @PutMapping
    @ApiOperation(value = "更新资产")
    public Result<Void> update(@RequestBody Asset asset) {
        assetService.updateAsset(asset);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除资产")
    public Result<Void> delete(@ApiParam(value = "资产ID") @PathVariable Long id) {
        assetService.deleteAsset(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @ApiOperation(value = "批量删除资产")
    public Result<Void> deleteBatch(@ApiParam(value = "资产ID列表") @RequestBody List<Long> ids) {
        assetService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping("/{id}/retire")
    @ApiOperation(value = "资产退役")
    public Result<Void> retire(@ApiParam(value = "资产ID") @PathVariable Long id) {
        assetService.retireAsset(id);
        return Result.success();
    }

    @PutMapping("/{id}/scrap")
    @ApiOperation(value = "资产报废")
    public Result<Void> scrap(@ApiParam(value = "资产ID") @PathVariable Long id) {
        assetService.scrapAsset(id);
        return Result.success();
    }

    @GetMapping("/{id}/qrcode")
    @ApiOperation(value = "获取资产二维码Base64")
    public Result<String> getQrCode(@ApiParam(value = "资产ID") @PathVariable Long id) {
        String base64 = assetService.getQrCodeBase64(id);
        return Result.success(base64);
    }

    @PostMapping("/qrcode/batch")
    @ApiOperation(value = "批量生成二维码")
    public Result<List<String>> batchGenerateQrCode(@ApiParam(value = "资产ID列表") @RequestBody List<Long> ids) {
        List<String> base64List = assetService.batchGenerateQrCode(ids);
        return Result.success(base64List);
    }

    @GetMapping("/export")
    @ApiOperation(value = "导出资产列表")
    public void export(@ApiParam(value = "查询条件") AssetQueryDTO queryDTO,
                       HttpServletResponse response) throws IOException {
        List<AssetExcelVO> list = assetService.exportAssetList(queryDTO);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("资产台账", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), AssetExcelVO.class)
                .sheet("资产台账")
                .doWrite(list);
    }

    @PostMapping("/import")
    @ApiOperation(value = "导入资产列表")
    public Result<Void> importAsset(@ApiParam(value = "Excel文件") @RequestParam("file") MultipartFile file) throws IOException {
        List<AssetExcelVO> list = EasyExcel.read(file.getInputStream())
                .head(AssetExcelVO.class)
                .sheet()
                .doReadSync();
        assetService.importAsset(list);
        return Result.success();
    }

    @GetMapping("/scan/{assetCode}")
    @ApiOperation(value = "扫码查询资产详情(移动端)")
    public Result<AssetDetailVO> scanQuery(@ApiParam(value = "资产编号") @PathVariable String assetCode) {
        Asset asset = assetService.getAssetByCode(assetCode);
        if (asset == null) {
            return Result.success(null);
        }
        AssetDetailVO detailVO = assetService.getAssetDetailById(asset.getId());
        return Result.success(detailVO);
    }
}
