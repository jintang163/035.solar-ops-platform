package com.solar.ops.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.entity.SysSkillTag;
import com.solar.ops.admin.service.SysSkillTagService;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/skill")
@Api(tags = "技能标签管理")
@RequiredArgsConstructor
public class SysSkillTagController {

    private final SysSkillTagService sysSkillTagService;

    @GetMapping("/tags")
    @ApiOperation("获取所有技能标签列表")
    public Result<List<SysSkillTag>> listTags(
            @ApiParam("分类") @RequestParam(required = false) String category) {
        List<SysSkillTag> list;
        if (category != null && !category.isEmpty()) {
            list = sysSkillTagService.listByCategory(category);
        } else {
            list = sysSkillTagService.listAll();
        }
        return Result.success(list);
    }

    @GetMapping("/tags/page")
    @ApiOperation("分页查询技能标签")
    public Result<Page<SysSkillTag>> pageTags(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam("每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("分类") @RequestParam(required = false) String category,
            @ApiParam("关键词") @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<SysSkillTag> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(SysSkillTag::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysSkillTag::getTagName, keyword)
                    .or().like(SysSkillTag::getTagCode, keyword);
        }
        wrapper.eq(SysSkillTag::getDeleted, 0);
        wrapper.orderByAsc(SysSkillTag::getSort, SysSkillTag::getId);

        Page<SysSkillTag> page = sysSkillTagService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(page);
    }

    @GetMapping("/tags/{id}")
    @ApiOperation("获取技能标签详情")
    public Result<SysSkillTag> getTag(@PathVariable Long id) {
        SysSkillTag tag = sysSkillTagService.getById(id);
        return Result.success(tag);
    }

    @PostMapping("/tags")
    @ApiOperation("新增技能标签")
    public Result<Boolean> addTag(@RequestBody SysSkillTag tag) {
        boolean success = sysSkillTagService.save(tag);
        return Result.success(success);
    }

    @PutMapping("/tags/{id}")
    @ApiOperation("更新技能标签")
    public Result<Boolean> updateTag(@PathVariable Long id, @RequestBody SysSkillTag tag) {
        tag.setId(id);
        boolean success = sysSkillTagService.updateById(tag);
        return Result.success(success);
    }

    @DeleteMapping("/tags/{id}")
    @ApiOperation("删除技能标签")
    public Result<Boolean> deleteTag(@PathVariable Long id) {
        boolean success = sysSkillTagService.removeById(id);
        return Result.success(success);
    }

    @GetMapping("/categories")
    @ApiOperation("获取所有标签分类")
    public Result<List<String>> listCategories() {
        List<String> categories = sysSkillTagService.getAllCategories();
        return Result.success(categories);
    }

    @GetMapping("/user/{userId}/skills")
    @ApiOperation("获取用户的技能标签")
    public Result<List<SysSkillTag>> getUserSkills(@PathVariable Long userId) {
        List<SysSkillTag> skills = sysSkillTagService.getUserSkills(userId);
        return Result.success(skills);
    }

    @PostMapping("/user/{userId}/skills")
    @ApiOperation("保存用户技能标签")
    public Result<Boolean> saveUserSkills(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> params) {

        @SuppressWarnings("unchecked")
        List<Long> tagIds = (List<Long>) params.get("tagIds");
        @SuppressWarnings("unchecked")
        List<Integer> proficiencies = (List<Integer>) params.get("proficiencies");

        boolean success = sysSkillTagService.saveUserSkills(userId, tagIds, proficiencies);
        return Result.success(success);
    }
}
