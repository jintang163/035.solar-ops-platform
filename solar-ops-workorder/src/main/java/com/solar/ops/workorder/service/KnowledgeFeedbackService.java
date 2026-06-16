package com.solar.ops.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.workorder.dto.KnowledgeFeedbackDTO;
import com.solar.ops.workorder.entity.KnowledgeFeedback;
import com.solar.ops.workorder.mapper.KnowledgeFeedbackMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeFeedbackService extends ServiceImpl<KnowledgeFeedbackMapper, KnowledgeFeedback> {

    private final FaultLibraryService faultLibraryService;

    @Transactional(rollbackFor = Exception.class)
    public boolean submitFeedback(KnowledgeFeedbackDTO dto) {
        LambdaQueryWrapper<KnowledgeFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeFeedback::getKnowledgeId, dto.getKnowledgeId())
                .eq(KnowledgeFeedback::getUserId, dto.getUserId());
        KnowledgeFeedback existing = this.getOne(wrapper);

        boolean isNewLike = false;
        boolean isNewDislike = false;

        if (existing != null) {
            if (existing.getFeedbackType().equals(dto.getFeedbackType())) {
                return true;
            }
            Integer oldType = existing.getFeedbackType();
            existing.setFeedbackType(dto.getFeedbackType());
            existing.setRemark(dto.getRemark());
            this.updateById(existing);

            if (oldType == 1) {
                isNewDislike = true;
                cancelLike(dto.getKnowledgeId());
            } else if (oldType == 2) {
                isNewLike = true;
                cancelDislike(dto.getKnowledgeId());
            }
        } else {
            KnowledgeFeedback feedback = new KnowledgeFeedback();
            feedback.setKnowledgeId(dto.getKnowledgeId());
            feedback.setWorkOrderId(dto.getWorkOrderId());
            feedback.setUserId(dto.getUserId());
            feedback.setUserName(dto.getUserName());
            feedback.setFeedbackType(dto.getFeedbackType());
            feedback.setRemark(dto.getRemark());
            this.save(feedback);

            if (dto.getFeedbackType() == 1) {
                isNewLike = true;
            } else if (dto.getFeedbackType() == 2) {
                isNewDislike = true;
            }
        }

        if (isNewLike) {
            faultLibraryService.updateLikeCount(dto.getKnowledgeId(), true);
        }
        if (isNewDislike) {
            faultLibraryService.updateLikeCount(dto.getKnowledgeId(), false);
        }

        return true;
    }

    private void cancelLike(Long knowledgeId) {
        faultLibraryService.lambdaUpdate()
                .eq(com.solar.ops.workorder.entity.FaultLibrary::getId, knowledgeId)
                .setSql("like_count = GREATEST(like_count - 1, 0)")
                .update();
    }

    private void cancelDislike(Long knowledgeId) {
        faultLibraryService.lambdaUpdate()
                .eq(com.solar.ops.workorder.entity.FaultLibrary::getId, knowledgeId)
                .setSql("dislike_count = GREATEST(dislike_count - 1, 0)")
                .update();
    }

    public KnowledgeFeedback getUserFeedback(Long knowledgeId, Long userId) {
        if (knowledgeId == null || userId == null) {
            return null;
        }
        LambdaQueryWrapper<KnowledgeFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeFeedback::getKnowledgeId, knowledgeId)
                .eq(KnowledgeFeedback::getUserId, userId);
        return this.getOne(wrapper);
    }
}
