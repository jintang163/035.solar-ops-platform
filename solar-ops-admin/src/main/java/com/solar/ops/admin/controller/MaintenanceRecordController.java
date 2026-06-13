package com.solar.ops.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.admin.dto.MaintenanceRecordWithSparePartDTO;
import com.solar.ops.admin.entity.MaintenanceRecord;
import com.solar.ops.admin.service.MaintenanceRecordService;
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
@RequestMapping("/api/maintenance-records")
@Api(tags = "维修记录管理")
public class MaintenanceRecordController {

    @Resource
    private MaintenanceRecordService maintenanceRecordService;

    @GetMapping
    @ApiOperation(value = "分页查询维修记录")
    public Result<PageResult<MaintenanceRecord>> page(@ApiParam(value = "分页参数") PageQuery pageQuery,
                                                       @ApiParam(value = "资产ID") @RequestParam Long assetId) {
        PageResult<MaintenanceRecord> pageResult = maintenanceRecordService.pageByAssetId(pageQuery, assetId);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据ID查询维修记录详情")
    public Result<MaintenanceRecord> getById(@ApiParam(value = "维修记录ID") @PathVariable Long id) {
        MaintenanceRecord record = maintenanceRecordService.getDetailById(id);
        return Result.success(record);
    }

    @GetMapping("/asset/{assetId}")
    @ApiOperation(value = "根据资产ID查询所有维修记录")
    public Result<List<MaintenanceRecord>> listByAssetId(@ApiParam(value = "资产ID") @PathVariable Long assetId) {
        List<MaintenanceRecord> list = maintenanceRecordService.list(new LambdaQueryWrapper<MaintenanceRecord>()
                .eq(MaintenanceRecord::getAssetId, assetId)
                .orderByDesc(MaintenanceRecord::getCreateTime));
        return Result.success(list);
    }

    @PostMapping
    @ApiOperation(value = "新增维修记录")
    public Result<Void> add(@RequestBody MaintenanceRecord record) {
        maintenanceRecordService.addMaintenanceRecord(record);
        return Result.success();
    }

    @PostMapping("/with-spare-parts")
    @ApiOperation(value = "新增维修记录(带备件)")
    public Result<Void> addWithSpareParts(@RequestBody MaintenanceRecordWithSparePartDTO dto) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setAssetId(dto.getAssetId());
        record.setFaultDescription(dto.getFaultDescription());
        record.setFaultType(dto.getFaultType());
        record.setMaintenanceType(dto.getMaintenanceType());
        record.setMaintenanceTime(dto.getMaintenanceTime());
        record.setMaintenancePerson(dto.getMaintenancePerson());
        record.setMaintenanceContent(dto.getMaintenanceContent());
        record.setSolution(dto.getSolution());
        record.setPhotos(dto.getPhotos());
        record.setCost(dto.getCost());
        record.setWorkOrderId(dto.getWorkOrderId());
        record.setRemark(dto.getRemark());

        maintenanceRecordService.addMaintenanceRecordWithSparePart(record, dto.getSpareParts());
        return Result.success();
    }

    @PutMapping
    @ApiOperation(value = "更新维修记录")
    public Result<Void> update(@RequestBody MaintenanceRecord record) {
        maintenanceRecordService.updateMaintenanceRecord(record);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除维修记录")
    public Result<Void> delete(@ApiParam(value = "维修记录ID") @PathVariable Long id) {
        maintenanceRecordService.deleteMaintenanceRecord(id);
        return Result.success();
    }

    @PutMapping("/{id}/complete")
    @ApiOperation(value = "维修完成")
    public Result<Void> complete(@ApiParam(value = "维修记录ID") @PathVariable Long id) {
        maintenanceRecordService.completeMaintenance(id);
        return Result.success();
    }
}
