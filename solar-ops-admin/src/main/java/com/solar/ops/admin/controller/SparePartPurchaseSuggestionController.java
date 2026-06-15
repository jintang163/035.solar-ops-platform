package com.solar.ops.admin.controller;

import com.solar.ops.admin.dto.PurchaseSuggestionQueryDTO;
import com.solar.ops.admin.service.SparePartPurchaseSuggestionService;
import com.solar.ops.admin.vo.SparePartPurchaseSuggestionVO;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/purchase-suggestions")
@Api(tags = "采购建议管理")
public class SparePartPurchaseSuggestionController {

    @Resource
    private SparePartPurchaseSuggestionService suggestionService;

    @GetMapping
    @ApiOperation(value = "分页查询采购建议列表")
    public Result<PageResult<SparePartPurchaseSuggestionVO>> page(@ApiParam(value = "分页参数") PageQuery pageQuery,
                                                                     @ApiParam(value = "查询条件") PurchaseSuggestionQueryDTO queryDTO) {
        PageResult<SparePartPurchaseSuggestionVO> pageResult = suggestionService.page(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据ID查询采购建议详情")
    public Result<SparePartPurchaseSuggestionVO> getById(@ApiParam(value = "建议ID") @PathVariable Long id) {
        SparePartPurchaseSuggestionVO detailVO = suggestionService.getDetailById(id);
        return Result.success(detailVO);
    }

    @PostMapping("/generate")
    @ApiOperation(value = "手动生成采购建议")
    public Result<Void> generate() {
        suggestionService.generateSuggestions();
        return Result.success();
    }

    @PutMapping("/{id}/process")
    @ApiOperation(value = "处理采购建议")
    public Result<Void> process(@ApiParam(value = "建议ID") @PathVariable Long id,
                                 @ApiParam(value = "处理状态 1-已采购 2-已忽略") @RequestParam Integer status,
                                 @ApiParam(value = "处理人姓名") @RequestParam(required = false) String processorName,
                                 @ApiParam(value = "备注") @RequestParam(required = false) String remark) {
        suggestionService.processSuggestion(id, status, processorName, remark);
        return Result.success();
    }

    @PutMapping("/batch/process")
    @ApiOperation(value = "批量处理采购建议")
    public Result<Void> batchProcess(@ApiParam(value = "建议ID列表") @RequestBody List<Long> ids,
                                      @ApiParam(value = "处理状态 1-已采购 2-已忽略") @RequestParam Integer status,
                                      @ApiParam(value = "处理人姓名") @RequestParam(required = false) String processorName) {
        suggestionService.batchProcess(ids, status, processorName);
        return Result.success();
    }

    @GetMapping("/pending/count")
    @ApiOperation(value = "待处理采购建议数量")
    public Result<Long> pendingCount() {
        long count = suggestionService.getPendingCount();
        return Result.success(count);
    }
}
