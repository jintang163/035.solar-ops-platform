package com.solar.ops.admin.controller;

import com.solar.ops.admin.dto.InspectionResultSubmitDTO;
import com.solar.ops.admin.entity.InspectionResult;
import com.solar.ops.admin.entity.InspectionReport;
import com.solar.ops.admin.service.InspectionResultService;
import com.solar.ops.admin.vo.InspectionResultDetailVO;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/inspection/results")
@Api(tags = "巡检结果管理")
public class InspectionResultController {

    @Resource
    private InspectionResultService resultService;

    @GetMapping
    @ApiOperation(value = "分页查询巡检结果列表")
    public Result<PageResult<InspectionResult>> page(@ApiParam(value = "分页参数") PageQuery pageQuery,
                                                      @ApiParam(value = "电站ID") @RequestParam(required = false) Long stationId,
                                                      @ApiParam(value = "结果状态") @RequestParam(required = false) Integer resultStatus) {
        PageResult<InspectionResult> pageResult = resultService.page(pageQuery, stationId, resultStatus);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "获取巡检结果详情")
    public Result<InspectionResultDetailVO> getDetail(@ApiParam(value = "结果ID") @PathVariable Long id) {
        InspectionResultDetailVO detail = resultService.getResultDetail(id);
        return Result.success(detail);
    }

    @PostMapping
    @ApiOperation(value = "提交巡检结果（移动端上传）")
    public Result<Long> submit(@Valid @RequestBody InspectionResultSubmitDTO submitDTO) {
        Long resultId = resultService.submitResult(submitDTO);
        return Result.success(resultId);
    }

    @PostMapping("/{id}/report")
    @ApiOperation(value = "生成巡检报告")
    public Result<Long> generateReport(@ApiParam(value = "结果ID") @PathVariable Long id) {
        Long reportId = resultService.generateReport(id);
        return Result.success(reportId);
    }
}
