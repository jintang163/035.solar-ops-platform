package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.entity.SysSkillTag;
import com.solar.ops.admin.entity.SysUserSkill;
import com.solar.ops.admin.mapper.SysSkillTagMapper;
import com.solar.ops.admin.mapper.SysUserSkillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysSkillTagService extends ServiceImpl<SysSkillTagMapper, SysSkillTag> {

    private final SysSkillTagMapper sysSkillTagMapper;
    private final SysUserSkillMapper sysUserSkillMapper;

    public List<SysSkillTag> listAll() {
        return sysSkillTagMapper.selectList(
                new LambdaQueryWrapper<SysSkillTag>()
                        .eq(SysSkillTag::getStatus, 1)
                        .eq(SysSkillTag::getDeleted, 0)
                        .orderByAsc(SysSkillTag::getSort, SysSkillTag::getId)
        );
    }

    public List<SysSkillTag> listByCategory(String category) {
        return sysSkillTagMapper.selectList(
                new LambdaQueryWrapper<SysSkillTag>()
                        .eq(SysSkillTag::getCategory, category)
                        .eq(SysSkillTag::getStatus, 1)
                        .eq(SysSkillTag::getDeleted, 0)
                        .orderByAsc(SysSkillTag::getSort, SysSkillTag::getId)
        );
    }

    public List<SysSkillTag> getUserSkills(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return sysSkillTagMapper.selectByUserId(userId);
    }

    public List<String> getUserSkillNames(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return sysSkillTagMapper.selectTagNamesByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean saveUserSkills(Long userId, List<Long> tagIds, List<Integer> proficiencies) {
        if (userId == null) {
            return false;
        }

        sysUserSkillMapper.deleteByUserId(userId);

        if (CollectionUtils.isEmpty(tagIds)) {
            return true;
        }

        for (int i = 0; i < tagIds.size(); i++) {
            Long tagId = tagIds.get(i);
            Integer proficiency = (proficiencies != null && i < proficiencies.size()) ? proficiencies.get(i) : 2;

            SysUserSkill userSkill = new SysUserSkill();
            userSkill.setUserId(userId);
            userSkill.setTagId(tagId);
            userSkill.setProficiency(proficiency);
            sysUserSkillMapper.insert(userSkill);
        }

        return true;
    }

    public List<String> getAllCategories() {
        List<SysSkillTag> tags = listAll();
        return tags.stream()
                .map(SysSkillTag::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
