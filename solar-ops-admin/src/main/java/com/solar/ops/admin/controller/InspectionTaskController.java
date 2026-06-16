package com.solar.ops.admin.controller;

import com.solar.ops.admin.dto.InspectionTaskQueryDTO;
import com.solar.ops.admin.entity.InspectionTask;
import com.solar.ops.admin.service.InspectionTaskService;
import com.solar.ops.admin.vo.InspectionTaskDetailVO;
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
@RequestMapping("/api/inspection/tasks")
@Api(tags = "巡检任务管理")
public class InspectionTaskController {

    @Resource
    private InspectionTaskService taskService;

    @GetMapping
    @ApiOperation(value = "分页查询巡检任务列表")
    public Result<PageResult<InspectionTask>> page(@ApiParam(value = "分页参数") PageQuery pageQuery,
                                                   @ApiParam(value = "查询条件") InspectionTaskQueryDTO queryDTO) {
        PageResult<InspectionTask> pageResult = taskService.page(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "获取巡检任务详情")
    public Result<InspectionTaskDetailVO> getDetail(@ApiParam(value = "任务ID") @PathVariable Long id) {
        InspectionTaskDetailVO detail = taskService.getTaskDetail(id);
        return Result.success(detail);
    }

    @GetMapping("/download")
    @ApiOperation(value = "获取待下载的巡检任务列表（移动端）")
    public Result<List<InspectionTaskDetailVO>> getDownloadTasks(@ApiParam(value = "用户ID") @RequestParam(required = false) Long userId) {
        List<InspectionTaskDetailVO> tasks = taskService.getTasksForDownload(userId);
        return Result.success(tasks);
    }

    @PostMapping
    @ApiOperation(value = "创建巡检任务")
    public Result<Long> create(@RequestBody InspectionTask task,
                               @RequestParam(required = false) List<Long> itemIds) {
        Long id = taskService.createTask(task, itemIds);
        return Result.success(id);
    }

    @PutMapping("/{id}/downloaded")
    @ApiOperation(value = "标记任务为已下载（移动端）")
    public Result<Void> markAsDownloaded(@ApiParam(value = "任务ID") @PathVariable Long id) {
        taskService.markAsDownloaded(id);
        return Result.success();
    }

    @PutMapping("/{id}/start")
    @ApiOperation(value = "开始执行任务（移动端）")
    public Result<Void> startTask(@ApiParam(value = "任务ID") @PathVariable Long id) {
        taskService.startTask(id);
        return Result.success();
    }

    @PutMapping("/{id}/complete")
    @ApiOperation(value = "完成任务")
    public Result<Void> completeTask(@ApiParam(value = "任务ID") @PathVariable Long id) {
        taskService.completeTask(id);
        return Result.success();
    }
}
