package com.solar.ops.workorder.controller;

import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import com.solar.ops.workorder.dto.FaultLibraryQueryDTO;
import com.solar.ops.workorder.entity.FaultLibrary;
import com.solar.ops.workorder.service.FaultLibraryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faultLibrary")
@Api(tags = "故障库管理")
@RequiredArgsConstructor
public class FaultLibraryController {

    private final FaultLibraryService faultLibraryService;

    @PostMapping("/page")
    @ApiOperation("故障库分页查询")
    public Result<PageResult<FaultLibrary>> page(@RequestBody FaultLibraryQueryDTO dto) {
        PageResult<FaultLibrary> page = faultLibraryService.page(dto);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @ApiOperation("故障详情")
    public Result<FaultLibrary> detail(@ApiParam(value = "故障ID", required = true) @PathVariable Long id) {
        FaultLibrary faultLibrary = faultLibraryService.getById(id);
        return Result.success(faultLibrary);
    }

    @GetMapping("/code/{faultCode}")
    @ApiOperation("根据故障码查询")
    public Result<FaultLibrary> getByCode(@ApiParam(value = "故障码", required = true) @PathVariable String faultCode) {
        FaultLibrary faultLibrary = faultLibraryService.getByFaultCode(faultCode);
        return Result.success(faultLibrary);
    }

    @GetMapping("/list")
    @ApiOperation("故障库列表")
    public Result<List<FaultLibrary>> list() {
        List<FaultLibrary> list = faultLibraryService.listAll();
        return Result.success(list);
    }

    @PostMapping("/add")
    @ApiOperation("新增故障")
    public Result<Void> add(@RequestBody FaultLibrary faultLibrary) {
        faultLibraryService.addFault(faultLibrary);
        return Result.success();
    }

    @PostMapping("/update")
    @ApiOperation("更新故障")
    public Result<Void> update(@RequestBody FaultLibrary faultLibrary) {
        faultLibraryService.updateFault(faultLibrary);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除故障")
    public Result<Void> delete(@ApiParam(value = "故障ID", required = true) @PathVariable Long id) {
        faultLibraryService.deleteFault(id);
        return Result.success();
    }

    @PostMapping("/refreshCache")
    @ApiOperation("刷新缓存")
    public Result<Void> refreshCache() {
        faultLibraryService.loadFaultCache();
        return Result.success();
    }
}
