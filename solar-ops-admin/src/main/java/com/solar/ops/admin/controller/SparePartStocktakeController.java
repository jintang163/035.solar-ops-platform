package com.solar.ops.admin.controller;

import com.alibaba.excel.EasyExcel;
import com.solar.ops.admin.dto.StocktakeCreateDTO;
import com.solar.ops.admin.dto.StocktakeItemUpdateDTO;
import com.solar.ops.admin.dto.StocktakeQueryDTO;
import com.solar.ops.admin.excel.StocktakeDiffExcelVO;
import com.solar.ops.admin.service.SparePartStocktakeService;
import com.solar.ops.admin.vo.SparePartStocktakeItemVO;
import com.solar.ops.admin.vo.SparePartStocktakeVO;
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
@RequestMapping("/api/stocktakes")
@Api(tags = "库存盘点管理")
public class SparePartStocktakeController {

    @Resource
    private SparePartStocktakeService stocktakeService;

    @GetMapping
    @ApiOperation(value = "分页查询盘点单列表")
    public Result<PageResult<SparePartStocktakeVO>> page(@ApiParam(value = "分页参数") PageQuery pageQuery,
                                                           @ApiParam(value = "查询条件") StocktakeQueryDTO queryDTO) {
        PageResult<SparePartStocktakeVO> pageResult = stocktakeService.page(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据ID查询盘点单详情")
    public Result<SparePartStocktakeVO> getById(@ApiParam(value = "盘点单ID") @PathVariable Long id) {
        SparePartStocktakeVO detailVO = stocktakeService.getDetailById(id);
        return Result.success(detailVO);
    }

    @GetMapping("/{id}/items")
    @ApiOperation(value = "查询盘点明细列表")
    public Result<PageResult<SparePartStocktakeItemVO>> items(@ApiParam(value = "盘点单ID") @PathVariable Long id,
                                                                 @ApiParam(value = "分页参数") PageQuery pageQuery,
                                                                 @ApiParam(value = "差异类型 0-无差异 1-盘盈 2-盘亏")
                                                                 @RequestParam(required = false) Integer diffType) {
        PageResult<SparePartStocktakeItemVO> pageResult = stocktakeService.getItemPage(pageQuery, id, diffType);
        return Result.success(pageResult);
    }

    @PostMapping
    @ApiOperation(value = "创建盘点单")
    public Result<Void> create(@RequestBody StocktakeCreateDTO createDTO) {
        stocktakeService.createStocktake(createDTO);
        return Result.success();
    }

    @PutMapping("/{id}/start")
    @ApiOperation(value = "开始盘点")
    public Result<Void> start(@ApiParam(value = "盘点单ID") @PathVariable Long id) {
        stocktakeService.startStocktake(id);
        return Result.success();
    }

    @PutMapping("/item")
    @ApiOperation(value = "更新盘点明细")
    public Result<Void> updateItem(@RequestBody StocktakeItemUpdateDTO updateDTO) {
        stocktakeService.updateStocktakeItem(updateDTO);
        return Result.success();
    }

    @PutMapping("/{id}/complete")
    @ApiOperation(value = "完成盘点")
    public Result<Void> complete(@ApiParam(value = "盘点单ID") @PathVariable Long id) {
        stocktakeService.completeStocktake(id);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @ApiOperation(value = "取消盘点")
    public Result<Void> cancel(@ApiParam(value = "盘点单ID") @PathVariable Long id) {
        stocktakeService.cancelStocktake(id);
        return Result.success();
    }

    @GetMapping("/{id}/diff/export")
    @ApiOperation(value = "导出盘点差异报表")
    public void exportDiffReport(@ApiParam(value = "盘点单ID") @PathVariable Long id,
                                  HttpServletResponse response) throws IOException {
        List<StocktakeDiffExcelVO> list = stocktakeService.exportDiffReport(id);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("盘点差异报表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), StocktakeDiffExcelVO.class)
                .sheet("差异报表")
                .doWrite(list);
    }
}
