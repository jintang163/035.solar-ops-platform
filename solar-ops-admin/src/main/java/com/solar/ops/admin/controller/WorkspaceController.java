package com.solar.ops.admin.controller;

import com.solar.ops.admin.dto.WorkspaceSwitchDTO;
import com.solar.ops.admin.holder.LoginUserHolder;
import com.solar.ops.admin.service.WorkspaceService;
import com.solar.ops.admin.vo.StationTreeVO;
import com.solar.ops.admin.vo.UserWorkspaceVO;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api")
@Api(tags = "工作空间管理")
public class WorkspaceController {

    @Resource
    private WorkspaceService workspaceService;

    @Resource
    private LoginUserHolder loginUserHolder;

    @GetMapping("/workspace/info")
    @ApiOperation(value = "获取当前用户工作空间信息")
    public Result<UserWorkspaceVO> getInfo() {
        UserWorkspaceVO vo = workspaceService.getWorkspaceInfo();
        return Result.success(vo);
    }

    @GetMapping("/workspace/station-tree")
    @ApiOperation(value = "获取可切换的电站树")
    public Result<List<StationTreeVO>> getStationTree() {
        List<StationTreeVO> tree = workspaceService.getStationTree();
        return Result.success(tree);
    }

    @PostMapping("/workspace/switch")
    @ApiOperation(value = "切换工作空间")
    public Result<Void> switchWorkspace(@RequestBody WorkspaceSwitchDTO dto) {
        workspaceService.switchWorkspace(dto);
        return Result.success();
    }
}
