package com.solar.ops.workorder.controller;

import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import com.solar.ops.workorder.dto.KnowledgeFeedbackDTO;
import com.solar.ops.workorder.dto.KnowledgeQueryDTO;
import com.solar.ops.workorder.dto.KnowledgeRecommendDTO;
import com.solar.ops.workorder.entity.FaultLibrary;
import com.solar.ops.workorder.entity.KnowledgeFeedback;
import com.solar.ops.workorder.service.FaultLibraryService;
import com.solar.ops.workorder.service.KnowledgeFeedbackService;
import com.solar.ops.workorder.vo.KnowledgeRecommendVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@Api(tags = "运维知识库管理")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final FaultLibraryService faultLibraryService;
    private final KnowledgeFeedbackService feedbackService;

    @PostMapping("/page")
    @ApiOperation("知识库分页查询")
    public Result<PageResult<FaultLibrary>> page(@RequestBody KnowledgeQueryDTO dto) {
        PageResult<FaultLibrary> page = faultLibraryService.page(dto);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @ApiOperation("知识库详情")
    public Result<FaultLibrary> detail(@ApiParam(value = "知识库ID", required = true) @PathVariable Long id) {
        FaultLibrary faultLibrary = faultLibraryService.getDetail(id);
        return Result.success(faultLibrary);
    }

    @GetMapping("/code/{faultCode}")
    @ApiOperation("根据故障码查询")
    public Result<FaultLibrary> getByCode(@ApiParam(value = "故障码", required = true) @PathVariable String faultCode) {
        FaultLibrary faultLibrary = faultLibraryService.getByFaultCode(faultCode);
        return Result.success(faultLibrary);
    }

    @GetMapping("/list")
    @ApiOperation("知识库列表(全部)")
    public Result<List<FaultLibrary>> list() {
        List<FaultLibrary> list = faultLibraryService.listAll();
        return Result.success(list);
    }

    @PostMapping("/add")
    @ApiOperation("新增知识库")
    public Result<Long> add(@RequestBody FaultLibrary faultLibrary) {
        faultLibraryService.addKnowledge(faultLibrary);
        return Result.success(faultLibrary.getId());
    }

    @PostMapping("/update")
    @ApiOperation("更新知识库")
    public Result<Void> update(@RequestBody FaultLibrary faultLibrary) {
        faultLibraryService.updateKnowledge(faultLibrary);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除知识库")
    public Result<Void> delete(@ApiParam(value = "知识库ID", required = true) @PathVariable Long id) {
        faultLibraryService.deleteKnowledge(id);
        return Result.success();
    }

    @PostMapping("/refreshCache")
    @ApiOperation("刷新缓存")
    public Result<Void> refreshCache() {
        faultLibraryService.loadFaultCache();
        return Result.success();
    }

    @PostMapping("/recommend")
    @ApiOperation("智能推荐相似案例")
    public Result<List<KnowledgeRecommendVO>> recommend(@RequestBody KnowledgeRecommendDTO dto) {
        List<KnowledgeRecommendVO> results = faultLibraryService.recommend(dto);
        return Result.success(results);
    }

    @PostMapping("/feedback")
    @ApiOperation("提交反馈(点赞/点踩)")
    public Result<Void> feedback(@RequestBody @Validated KnowledgeFeedbackDTO dto) {
        feedbackService.submitFeedback(dto);
        return Result.success();
    }

    @GetMapping("/feedback/{knowledgeId}/{userId}")
    @ApiOperation("获取用户反馈状态")
    public Result<KnowledgeFeedback> getFeedback(
            @ApiParam(value = "知识库ID", required = true) @PathVariable Long knowledgeId,
            @ApiParam(value = "用户ID", required = true) @PathVariable Long userId) {
        KnowledgeFeedback feedback = feedbackService.getUserFeedback(knowledgeId, userId);
        return Result.success(feedback);
    }

    @PostMapping("/recordUsage")
    @ApiOperation("记录知识库使用")
    public Result<Void> recordUsage(@RequestBody Map<String, Object> params) {
        Long knowledgeId = Long.valueOf(params.get("knowledgeId").toString());
        Long workOrderId = params.get("workOrderId") != null ? Long.valueOf(params.get("workOrderId").toString()) : null;
        Long userId = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
        String userName = params.get("userName") != null ? params.get("userName").toString() : null;
        Integer sourceType = params.get("sourceType") != null ? Integer.valueOf(params.get("sourceType").toString()) : 1;
        faultLibraryService.recordUsage(knowledgeId, workOrderId, userId, userName, sourceType);
        return Result.success();
    }
}
