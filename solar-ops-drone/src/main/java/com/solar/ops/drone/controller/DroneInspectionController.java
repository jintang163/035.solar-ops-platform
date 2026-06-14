package com.solar.ops.drone.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.solar.ops.common.result.R;
import com.solar.ops.drone.dto.*;
import com.solar.ops.drone.service.DroneDefectService;
import com.solar.ops.drone.service.DroneInspectionImageService;
import com.solar.ops.drone.service.DroneInspectionTaskService;
import com.solar.ops.drone.vo.DroneDefectVO;
import com.solar.ops.drone.vo.DroneImageVO;
import com.solar.ops.drone.vo.DroneStatisticsVO;
import com.solar.ops.drone.vo.DroneTaskVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Api(tags = "无人机巡检管理")
@RestController
@RequestMapping("/api/drone")
public class DroneInspectionController {

    @Autowired
    private DroneInspectionTaskService taskService;

    @Autowired
    private DroneInspectionImageService imageService;

    @Autowired
    private DroneDefectService defectService;

    @ApiOperation("获取统计信息")
    @GetMapping("/statistics")
    public R<DroneStatisticsVO> getStatistics(@RequestParam(required = false) Long stationId) {
        return R.ok(taskService.getStatistics(stationId));
    }

    @ApiOperation("分页查询巡检任务")
    @PostMapping("/task/page")
    public R<IPage<DroneTaskVO>> taskPage(@RequestBody DroneTaskQueryDTO dto) {
        return R.ok(taskService.page(dto));
    }

    @ApiOperation("获取巡检任务详情")
    @GetMapping("/task/{id}")
    public R<DroneTaskVO> getTaskDetail(@PathVariable Long id) {
        return R.ok(taskService.getDetail(id));
    }

    @ApiOperation("创建巡检任务")
    @PostMapping("/task/create")
    public R<Long> createTask(@RequestBody DroneTaskCreateDTO dto) {
        return R.ok(taskService.create(dto));
    }

    @ApiOperation("更新任务状态")
    @PostMapping("/task/{id}/status")
    public R<Boolean> updateTaskStatus(@PathVariable Long id, @RequestParam Integer status) {
        return R.ok(taskService.updateStatus(id, status));
    }

    @ApiOperation("删除巡检任务")
    @DeleteMapping("/task/{id}")
    public R<Boolean> deleteTask(@PathVariable Long id) {
        return R.ok(taskService.delete(id));
    }

    @ApiOperation("分页查询巡检图片")
    @PostMapping("/image/page")
    public R<IPage<DroneImageVO>> imagePage(@RequestBody DroneImageQueryDTO dto) {
        return R.ok(imageService.page(dto));
    }

    @ApiOperation("获取图片详情")
    @GetMapping("/image/{id}")
    public R<DroneImageVO> getImageDetail(@PathVariable Long id) {
        return R.ok(imageService.getDetail(id));
    }

    @ApiOperation("根据任务ID获取图片列表")
    @GetMapping("/image/task/{taskId}")
    public R<List<DroneImageVO>> getImagesByTaskId(@PathVariable Long taskId) {
        return R.ok(imageService.getByTaskId(taskId));
    }

    @ApiOperation("上传巡检图片")
    @PostMapping("/image/upload")
    public R<String> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long taskId,
            @RequestParam Long stationId) {
        return R.ok(imageService.uploadImage(file, taskId, stationId));
    }

    @ApiOperation("批量上传巡检图片")
    @PostMapping("/image/batch-upload")
    public R<Boolean> batchUploadImage(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) Long taskId,
            @RequestParam Long stationId) {
        return R.ok(imageService.batchUpload(files, taskId, stationId));
    }

    @ApiOperation("触发图片AI检测")
    @PostMapping("/image/{id}/detect")
    public R<Boolean> triggerDetect(@PathVariable Long id) {
        return R.ok(imageService.triggerDetect(id));
    }

    @ApiOperation("批量触发任务下所有图片检测")
    @PostMapping("/task/{taskId}/detect-all")
    public R<Boolean> triggerBatchDetect(@PathVariable Long taskId) {
        return R.ok(imageService.triggerBatchDetect(taskId));
    }

    @ApiOperation("更新检测状态")
    @PostMapping("/image/{id}/detect-status")
    public R<Boolean> updateDetectStatus(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String result) {
        return R.ok(imageService.updateDetectStatus(id, status, result));
    }

    @ApiOperation("分页查询缺陷")
    @PostMapping("/defect/page")
    public R<IPage<DroneDefectVO>> defectPage(@RequestBody DroneDefectQueryDTO dto) {
        return R.ok(defectService.page(dto));
    }

    @ApiOperation("获取缺陷详情")
    @GetMapping("/defect/{id}")
    public R<DroneDefectVO> getDefectDetail(@PathVariable Long id) {
        return R.ok(defectService.getDetail(id));
    }

    @ApiOperation("根据图片ID获取缺陷列表")
    @GetMapping("/defect/image/{imageId}")
    public R<List<DroneDefectVO>> getDefectsByImageId(@PathVariable Long imageId) {
        return R.ok(defectService.getByImageId(imageId));
    }

    @ApiOperation("根据任务ID获取缺陷列表")
    @GetMapping("/defect/task/{taskId}")
    public R<List<DroneDefectVO>> getDefectsByTaskId(@PathVariable Long taskId) {
        return R.ok(defectService.getByTaskId(taskId));
    }

    @ApiOperation("确认/取消确认缺陷")
    @PostMapping("/defect/confirm")
    public R<Boolean> confirmDefect(@RequestBody DroneDefectHandleDTO dto) {
        return R.ok(defectService.confirmDefect(dto));
    }

    @ApiOperation("生成工单")
    @PostMapping("/defect/generate-work-order")
    public R<Long> generateWorkOrder(@RequestBody DroneDefectHandleDTO dto) {
        return R.ok(defectService.generateWorkOrder(dto));
    }

    @ApiOperation("统计未确认缺陷数量")
    @GetMapping("/defect/count-unconfirmed")
    public R<Integer> countUnconfirmed(@RequestParam(required = false) Long stationId) {
        return R.ok(defectService.countUnconfirmed(stationId));
    }

    @ApiOperation("统计严重缺陷数量")
    @GetMapping("/defect/count-serious")
    public R<Integer> countSerious(@RequestParam(required = false) Long stationId) {
        return R.ok(defectService.countSerious(stationId));
    }
}
