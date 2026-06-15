package com.solar.ops.admin.controller;

import com.solar.ops.admin.dto.OrgQueryDTO;
import com.solar.ops.admin.dto.SysOrgDTO;
import com.solar.ops.admin.entity.SysOrg;
import com.solar.ops.admin.holder.LoginUserHolder;
import com.solar.ops.admin.service.SysOrgService;
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
@RequestMapping("/api")
@Api(tags = "组织架构管理")
public class SysOrgController {

    @Resource
    private SysOrgService sysOrgService;

    @Resource
    private LoginUserHolder loginUserHolder;

    @GetMapping("/orgs")
    @ApiOperation(value = "分页查询组织列表")
    public Result<PageResult<SysOrg>> page(@ApiParam(value = "分页参数") PageQuery pageQuery,
                                           @ApiParam(value = "查询参数") OrgQueryDTO queryDTO) {
        PageResult<SysOrg> pageResult = sysOrgService.page(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/orgs/{id}")
    @ApiOperation(value = "根据ID查询组织")
    public Result<SysOrg> getById(@ApiParam(value = "组织ID") @PathVariable Long id) {
        SysOrg org = sysOrgService.getOrgById(id);
        return Result.success(org);
    }

    @PostMapping("/orgs")
    @ApiOperation(value = "新增组织")
    public Result<Void> add(@RequestBody SysOrgDTO dto) {
        sysOrgService.addOrg(dto);
        return Result.success();
    }

    @PutMapping("/orgs")
    @ApiOperation(value = "更新组织")
    public Result<Void> update(@RequestBody SysOrgDTO dto) {
        sysOrgService.updateOrg(dto);
        return Result.success();
    }

    @DeleteMapping("/orgs/{id}")
    @ApiOperation(value = "删除组织")
    public Result<Void> delete(@ApiParam(value = "组织ID") @PathVariable Long id) {
        sysOrgService.deleteOrg(id);
        return Result.success();
    }

    @GetMapping("/orgs/tree")
    @ApiOperation(value = "获取组织树")
    public Result<List<SysOrg>> getOrgTree() {
        List<SysOrg> tree = sysOrgService.getOrgTree();
        return Result.success(tree);
    }
}
