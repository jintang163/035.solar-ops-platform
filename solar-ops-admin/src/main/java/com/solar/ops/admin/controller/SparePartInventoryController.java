package com.solar.ops.admin.controller;

import com.alibaba.excel.EasyExcel;
import com.solar.ops.admin.dto.SparePartInboundDTO;
import com.solar.ops.admin.dto.SparePartInventoryQueryDTO;
import com.solar.ops.admin.dto.SparePartOutboundDTO;
import com.solar.ops.admin.entity.SparePartInventory;
import com.solar.ops.admin.excel.SparePartInventoryExcelVO;
import com.solar.ops.admin.service.SparePartInventoryService;
import com.solar.ops.admin.vo.InventoryDashboardVO;
import com.solar.ops.admin.vo.SparePartInventoryVO;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/spare-parts")
@Api(tags = "备件库存管理")
public class SparePartInventoryController {

    @Resource
    private SparePartInventoryService inventoryService;

    @GetMapping
    @ApiOperation(value = "分页查询备件库存列表")
    public Result<PageResult<SparePartInventoryVO>> page(@ApiParam(value = "分页参数") PageQuery pageQuery,
                                                          @ApiParam(value = "查询条件") SparePartInventoryQueryDTO queryDTO) {
        PageResult<SparePartInventoryVO> pageResult = inventoryService.page(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据ID查询备件详情")
    public Result<SparePartInventoryVO> getById(@ApiParam(value = "备件ID") @PathVariable Long id) {
        SparePartInventoryVO detailVO = inventoryService.getDetailById(id);
        return Result.success(detailVO);
    }

    @GetMapping("/code/{partCode}")
    @ApiOperation(value = "根据备件编号查询")
    public Result<SparePartInventory> getByCode(@ApiParam(value = "备件编号") @PathVariable String partCode) {
        SparePartInventory inventory = inventoryService.getByCode(partCode);
        return Result.success(inventory);
    }

    @PostMapping
    @ApiOperation(value = "新增备件")
    public Result<Void> add(@RequestBody SparePartInventory inventory) {
        inventoryService.addInventory(inventory);
        return Result.success();
    }

    @PutMapping
    @ApiOperation(value = "更新备件")
    public Result<Void> update(@RequestBody SparePartInventory inventory) {
        inventoryService.updateInventory(inventory);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除备件")
    public Result<Void> delete(@ApiParam(value = "备件ID") @PathVariable Long id) {
        inventoryService.deleteInventory(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @ApiOperation(value = "批量删除备件")
    public Result<Void> deleteBatch(@ApiParam(value = "备件ID列表") @RequestBody List<Long> ids) {
        inventoryService.deleteBatch(ids);
        return Result.success();
    }

    @PostMapping("/inbound")
    @ApiOperation(value = "备件入库")
    public Result<Void> inbound(@RequestBody SparePartInboundDTO inboundDTO) {
        inventoryService.inbound(inboundDTO);
        return Result.success();
    }

    @PostMapping("/outbound")
    @ApiOperation(value = "备件出库")
    public Result<Void> outbound(@RequestBody SparePartOutboundDTO outboundDTO) {
        inventoryService.outbound(outboundDTO);
        return Result.success();
    }

    @GetMapping("/{id}/qrcode")
    @ApiOperation(value = "获取备件二维码Base64")
    public Result<String> getQrCode(@ApiParam(value = "备件ID") @PathVariable Long id) {
        String base64 = inventoryService.getQrCodeBase64(id);
        return Result.success(base64);
    }

    @PostMapping("/qrcode/batch")
    @ApiOperation(value = "批量生成二维码")
    public Result<List<String>> batchGenerateQrCode(@ApiParam(value = "备件ID列表") @RequestBody List<Long> ids) {
        List<String> base64List = inventoryService.batchGenerateQrCode(ids);
        return Result.success(base64List);
    }

    @GetMapping("/scan/{partCode}")
    @ApiOperation(value = "扫码查询备件详情")
    public Result<SparePartInventoryVO> scanQuery(@ApiParam(value = "备件编号") @PathVariable String partCode) {
        SparePartInventoryVO detailVO = inventoryService.scanQuery(partCode);
        return Result.success(detailVO);
    }

    @GetMapping("/dashboard")
    @ApiOperation(value = "库存仪表盘数据")
    public Result<InventoryDashboardVO> dashboard() {
        InventoryDashboardVO dashboard = inventoryService.getDashboard();
        return Result.success(dashboard);
    }

    @GetMapping("/export")
    @ApiOperation(value = "导出备件库存列表")
    public void export(@ApiParam(value = "查询条件") SparePartInventoryQueryDTO queryDTO,
                       HttpServletResponse response) throws IOException {
        List<SparePartInventoryExcelVO> list = inventoryService.exportList(queryDTO);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("备件库存台账", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), SparePartInventoryExcelVO.class)
                .sheet("备件库存台账")
                .doWrite(list);
    }
}
