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
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;

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

    @PostMapping("/{id}/photos")
    @ApiOperation(value = "上传巡检照片")
    public Result<Long> uploadPhoto(@ApiParam(value = "结果ID") @PathVariable Long id,
                                    @ApiParam(value = "检查项结果ID") @RequestParam(required = false) Long resultItemId,
                                    @ApiParam(value = "照片类型 1-普通 2-红外") @RequestParam(required = false, defaultValue = "1") Integer photoType,
                                    @ApiParam(value = "经度") @RequestParam(required = false) BigDecimal longitude,
                                    @ApiParam(value = "纬度") @RequestParam(required = false) BigDecimal latitude,
                                    @ApiParam(value = "是否有水印") @RequestParam(required = false) Boolean hasWatermark,
                                    @ApiParam(value = "备注") @RequestParam(required = false) String remark,
                                    @ApiParam(value = "照片文件") @RequestParam("file") MultipartFile file) {
        Long photoId = resultService.uploadPhoto(id, resultItemId, file, photoType, longitude, latitude, hasWatermark, remark);
        return Result.success(photoId);
    }

    @PostMapping("/{id}/audios")
    @ApiOperation(value = "上传巡检录音")
    public Result<Long> uploadAudio(@ApiParam(value = "结果ID") @PathVariable Long id,
                                    @ApiParam(value = "检查项结果ID") @RequestParam(required = false) Long resultItemId,
                                    @ApiParam(value = "经度") @RequestParam(required = false) BigDecimal longitude,
                                    @ApiParam(value = "纬度") @RequestParam(required = false) BigDecimal latitude,
                                    @ApiParam(value = "备注") @RequestParam(required = false) String remark,
                                    @ApiParam(value = "录音文件") @RequestParam("file") MultipartFile file) {
        Long audioId = resultService.uploadAudio(id, resultItemId, file, longitude, latitude, remark);
        return Result.success(audioId);
    }
}
