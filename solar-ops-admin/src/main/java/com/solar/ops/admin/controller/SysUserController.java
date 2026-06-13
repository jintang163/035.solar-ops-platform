package com.solar.ops.admin.controller;

import com.solar.ops.admin.dto.LoginDTO;
import com.solar.ops.admin.entity.SysUser;
import com.solar.ops.admin.service.SysUserService;
import com.solar.ops.admin.vo.LoginVO;
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
@RequestMapping("/api")
@Api(tags = "用户管理")
public class SysUserController {

    @Resource
    private SysUserService sysUserService;

    @PostMapping("/auth/login")
    @ApiOperation(value = "用户登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = sysUserService.login(loginDTO);
        return Result.success(loginVO);
    }

    @GetMapping("/users")
    @ApiOperation(value = "分页查询用户列表")
    public Result<PageResult<SysUser>> page(@ApiParam(value = "分页参数") PageQuery pageQuery,
                                            @ApiParam(value = "关键词") @RequestParam(required = false) String keyword) {
        PageResult<SysUser> pageResult = sysUserService.page(pageQuery, keyword);
        return Result.success(pageResult);
    }

    @GetMapping("/users/{id}")
    @ApiOperation(value = "根据ID查询用户")
    public Result<SysUser> getById(@ApiParam(value = "用户ID") @PathVariable Long id) {
        SysUser user = sysUserService.getUserById(id);
        return Result.success(user);
    }

    @PostMapping("/users")
    @ApiOperation(value = "新增用户")
    public Result<Void> add(@RequestBody SysUser user) {
        sysUserService.addUser(user);
        return Result.success();
    }

    @PutMapping("/users")
    @ApiOperation(value = "更新用户")
    public Result<Void> update(@RequestBody SysUser user) {
        sysUserService.updateUser(user);
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    @ApiOperation(value = "删除用户")
    public Result<Void> delete(@ApiParam(value = "用户ID") @PathVariable Long id) {
        sysUserService.deleteUser(id);
        return Result.success();
    }
}
