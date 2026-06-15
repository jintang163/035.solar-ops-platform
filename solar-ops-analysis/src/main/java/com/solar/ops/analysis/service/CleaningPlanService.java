package com.solar.ops.analysis.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.analysis.dto.CleaningPlanCreateDTO;
import com.solar.ops.analysis.dto.CleaningPlanExecuteDTO;
import com.solar.ops.analysis.dto.CleaningPlanQueryDTO;
import com.solar.ops.analysis.entity.CleaningPlan;
import com.solar.ops.analysis.entity.EfficiencyStatistics;
import com.solar.ops.analysis.enums.CleaningStatusEnum;
import com.solar.ops.analysis.mapper.CleaningPlanMapper;
import com.solar.ops.analysis.util.DustAccumulationCalculator;
import com.solar.ops.analysis.vo.CleaningPlanVO;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CleaningPlanService extends ServiceImpl<CleaningPlanMapper, CleaningPlan> {

    @Autowired
    private CleaningReminderService cleaningReminderService;

    @Autowired
    private EfficiencyAnalysisService efficiencyAnalysisService;

    @Transactional(rollbackFor = Exception.class)
    public CleaningPlan createPlan(CleaningPlanCreateDTO dto, Long creatorId, String creatorName) {
        CleaningPlan plan = new CleaningPlan();
        BeanUtils.copyProperties(dto, plan);
        plan.setPlanNo(generatePlanNo());
        plan.setStatus(CleaningStatusEnum.PENDING.getCode());
        plan.setOwnerId(dto.getOwnerId() != null ? dto.getOwnerId() : creatorId);
        plan.setOwnerName(dto.getOwnerName() != null ? dto.getOwnerName() : creatorName);
        save(plan);

        if (dto.getReminderId() != null) {
            cleaningReminderService.markAsCreatedPlan(
                    dto.getReminderId(), plan.getId(), creatorId, creatorName);
        }

        return plan;
    }

    @Transactional(rollbackFor = Exception.class)
    public CleaningPlan updatePlan(CleaningPlanCreateDTO dto, Long operatorId, String operatorName) {
        if (dto.getId() == null) {
            throw new BusinessException("计划ID不能为空");
        }
        CleaningPlan plan = getById(dto.getId());
        if (plan == null) {
            throw new BusinessException("清洗计划不存在");
        }
        if (!CleaningStatusEnum.PENDING.getCode().equals(plan.getStatus())) {
            throw new BusinessException("计划已开始执行，无法修改");
        }

        BeanUtils.copyProperties(dto, plan, "id", "planNo", "status", "createTime");
        updateById(plan);
        return plan;
    }

    public PageResult<CleaningPlanVO> queryPlanPage(CleaningPlanQueryDTO queryDTO) {
        LambdaQueryWrapper<CleaningPlan> wrapper = buildPlanQueryWrapper(queryDTO);
        Page<CleaningPlan> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<CleaningPlan> result = page(page, wrapper);
        List<CleaningPlanVO> voList = result.getRecords().stream()
                .map(this::convertToPlanVO)
                .collect(Collectors.toList());
        return PageResult.build(voList, result.getTotal());
    }

    public List<CleaningPlanVO> queryPlanList(CleaningPlanQueryDTO queryDTO) {
        LambdaQueryWrapper<CleaningPlan> wrapper = buildPlanQueryWrapper(queryDTO);
        wrapper.orderByDesc(CleaningPlan::getPlanDate);
        return list(wrapper).stream()
                .map(this::convertToPlanVO)
                .collect(Collectors.toList());
    }

    public List<CleaningPlanVO> queryPlansByDateRange(LocalDate startDate, LocalDate endDate, Long stationId) {
        LambdaQueryWrapper<CleaningPlan> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(CleaningPlan::getStationId, stationId);
        }
        wrapper.between(CleaningPlan::getPlanDate, startDate, endDate);
        wrapper.orderByAsc(CleaningPlan::getPlanDate);
        return list(wrapper).stream()
                .map(this::convertToPlanVO)
                .collect(Collectors.toList());
    }

    public CleaningPlanVO getPlanDetail(Long id) {
        CleaningPlan plan = getById(id);
        return plan != null ? convertToPlanVO(plan) : null;
    }

    @Transactional(rollbackFor = Exception.class)
    public void startExecution(CleaningPlanExecuteDTO dto) {
        CleaningPlan plan = getById(dto.getPlanId());
        if (plan == null) {
            throw new BusinessException("清洗计划不存在");
        }
        if (!CleaningStatusEnum.PENDING.getCode().equals(plan.getStatus())) {
            throw new BusinessException("只有待执行的计划可以开始");
        }

        plan.setStatus(CleaningStatusEnum.IN_PROGRESS.getCode());
        plan.setActualStartTime(LocalDateTime.now());

        if (StringUtils.hasText(dto.getBeforeCleanPhotos())) {
            plan.setBeforeCleanPhotos(dto.getBeforeCleanPhotos());
        }
        if (StringUtils.hasText(dto.getCleaningMethod())) {
            plan.setCleaningMethod(dto.getCleaningMethod());
        }
        if (dto.getWaterUsage() != null) {
            plan.setWaterUsage(dto.getWaterUsage());
        }
        if (dto.getCleaningCost() != null) {
            plan.setCleaningCost(dto.getCleaningCost());
        }

        updateById(plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeExecution(CleaningPlanExecuteDTO dto) {
        CleaningPlan plan = getById(dto.getPlanId());
        if (plan == null) {
            throw new BusinessException("清洗计划不存在");
        }
        if (!CleaningStatusEnum.IN_PROGRESS.getCode().equals(plan.getStatus())) {
            throw new BusinessException("只有执行中的计划可以完成");
        }

        plan.setStatus(CleaningStatusEnum.COMPLETED.getCode());
        plan.setActualEndTime(LocalDateTime.now());

        if (StringUtils.hasText(dto.getAfterCleanPhotos())) {
            plan.setAfterCleanPhotos(dto.getAfterCleanPhotos());
        }
        if (StringUtils.hasText(dto.getBeforeCleanPhotos()) && !StringUtils.hasText(plan.getBeforeCleanPhotos())) {
            plan.setBeforeCleanPhotos(dto.getBeforeCleanPhotos());
        }
        if (StringUtils.hasText(dto.getWorkRemark())) {
            plan.setWorkRemark(dto.getWorkRemark());
        }
        if (StringUtils.hasText(dto.getCleaningMethod())) {
            plan.setCleaningMethod(dto.getCleaningMethod());
        }
        if (dto.getWaterUsage() != null) {
            plan.setWaterUsage(dto.getWaterUsage());
        }
        if (dto.getCleaningCost() != null) {
            plan.setCleaningCost(dto.getCleaningCost());
        }
        if (dto.getOperatorId() != null) {
            plan.setInspectorId(dto.getOperatorId());
            plan.setInspectorName(dto.getOperatorName());
            plan.setInspectionTime(LocalDateTime.now());
        }
        if (StringUtils.hasText(dto.getInspectionRemark())) {
            plan.setInspectionRemark(dto.getInspectionRemark());
        }

        updateById(plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPlan(Long planId, Long operatorId, String operatorName, String reason) {
        CleaningPlan plan = getById(planId);
        if (plan == null) {
            throw new BusinessException("清洗计划不存在");
        }
        if (CleaningStatusEnum.COMPLETED.getCode().equals(plan.getStatus())
                || CleaningStatusEnum.CANCELLED.getCode().equals(plan.getStatus())) {
            throw new BusinessException("计划已完成或已取消，无法再次取消");
        }

        plan.setStatus(CleaningStatusEnum.CANCELLED.getCode());
        plan.setWorkRemark(StringUtils.hasText(plan.getWorkRemark())
                ? plan.getWorkRemark() + " | 取消原因：" + reason
                : "取消原因：" + reason);
        updateById(plan);
    }

    public void uploadPhotos(Long planId, String beforePhotos, String afterPhotos) {
        CleaningPlan plan = getById(planId);
        if (plan == null) {
            throw new BusinessException("清洗计划不存在");
        }
        if (StringUtils.hasText(beforePhotos)) {
            plan.setBeforeCleanPhotos(StringUtils.hasText(plan.getBeforeCleanPhotos())
                    ? plan.getBeforeCleanPhotos() + "," + beforePhotos
                    : beforePhotos);
        }
        if (StringUtils.hasText(afterPhotos)) {
            plan.setAfterCleanPhotos(StringUtils.hasText(plan.getAfterCleanPhotos())
                    ? plan.getAfterCleanPhotos() + "," + afterPhotos
                    : afterPhotos);
        }
        updateById(plan);
    }

    public Map<String, Integer> countPlansByStatus(Long stationId) {
        Map<String, Integer> result = new HashMap<>();
        for (CleaningStatusEnum status : CleaningStatusEnum.values()) {
            LambdaQueryWrapper<CleaningPlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CleaningPlan::getStatus, status.getCode());
            if (stationId != null) {
                wrapper.eq(CleaningPlan::getStationId, stationId);
            }
            result.put(status.name(), count(wrapper));
        }
        return result;
    }

    public boolean calculateImprovementForPlan(Long planId) {
        CleaningPlan plan = getById(planId);
        if (plan == null) {
            return false;
        }
        if (!CleaningStatusEnum.COMPLETED.getCode().equals(plan.getStatus())) {
            return false;
        }
        if (plan.getPlanDate() == null || plan.getInverterId() == null) {
            return false;
        }

        LocalDate cleanDate = plan.getPlanDate();
        LocalDate today = LocalDate.now();
        if (cleanDate.plusDays(7).isAfter(today)) {
            return false;
        }

        LocalDate beforeStart = cleanDate.minusDays(14);
        LocalDate beforeEnd = cleanDate.minusDays(1);
        LocalDate afterStart = cleanDate.plusDays(1);
        LocalDate afterEnd = cleanDate.plusDays(14).isAfter(today) ? today.minusDays(1) : cleanDate.plusDays(14);

        LambdaQueryWrapper<EfficiencyStatistics> effWrapper = new LambdaQueryWrapper<>();
        effWrapper.eq(EfficiencyStatistics::getInverterId, plan.getInverterId())
                .eq(EfficiencyStatistics::getStatisticsType, 1)
                .between(EfficiencyStatistics::getStatisticsDate, beforeStart, afterEnd);
        List<EfficiencyStatistics> statsList = efficiencyAnalysisService.list(effWrapper);

        BigDecimal beforeAvg = DustAccumulationCalculator.calculateAverageEnergy(
                statsList, beforeStart, beforeEnd);
        BigDecimal afterAvg = DustAccumulationCalculator.calculateAverageEnergy(
                statsList, afterStart, afterEnd);

        if (beforeAvg.compareTo(BigDecimal.ZERO) <= 0 || afterAvg.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        plan.setBeforeCleanEnergy(beforeAvg);
        plan.setAfterCleanEnergy(afterAvg);

        if (afterAvg.compareTo(beforeAvg) > 0) {
            plan.setImprovedEnergy(afterAvg.subtract(beforeAvg));
            plan.setImprovementRate(
                    DustAccumulationCalculator.calculateImprovementRate(beforeAvg, afterAvg));
        } else {
            plan.setImprovedEnergy(BigDecimal.ZERO);
            plan.setImprovementRate(BigDecimal.ZERO);
        }

        updateById(plan);
        return true;
    }

    public int calculatePendingImprovements() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);

        LambdaQueryWrapper<CleaningPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CleaningPlan::getStatus, CleaningStatusEnum.COMPLETED.getCode())
                .le(CleaningPlan::getPlanDate, sevenDaysAgo)
                .isNull(CleaningPlan::getImprovedEnergy);

        List<CleaningPlan> pendingPlans = list(wrapper);
        if (CollectionUtils.isEmpty(pendingPlans)) {
            return 0;
        }

        int successCount = 0;
        for (CleaningPlan plan : pendingPlans) {
            try {
                if (calculateImprovementForPlan(plan.getId())) {
                    successCount++;
                }
            } catch (Exception e) {
                continue;
            }
        }
        return successCount;
    }

    private LambdaQueryWrapper<CleaningPlan> buildPlanQueryWrapper(CleaningPlanQueryDTO queryDTO) {
        LambdaQueryWrapper<CleaningPlan> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getStationId() != null) {
            wrapper.eq(CleaningPlan::getStationId, queryDTO.getStationId());
        }
        if (queryDTO.getInverterId() != null) {
            wrapper.eq(CleaningPlan::getInverterId, queryDTO.getInverterId());
        }
        if (StringUtils.hasText(queryDTO.getArrayNumber())) {
            wrapper.like(CleaningPlan::getArrayNumber, queryDTO.getArrayNumber());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(CleaningPlan::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getOwnerId() != null) {
            wrapper.eq(CleaningPlan::getOwnerId, queryDTO.getOwnerId());
        }
        if (queryDTO.getStartDate() != null && queryDTO.getEndDate() != null) {
            wrapper.between(CleaningPlan::getPlanDate,
                    queryDTO.getStartDate(), queryDTO.getEndDate());
        }
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(CleaningPlan::getPlanNo, queryDTO.getKeyword())
                    .or().like(CleaningPlan::getTitle, queryDTO.getKeyword()));
        }
        wrapper.orderByDesc(CleaningPlan::getCreateTime);
        return wrapper;
    }

    private CleaningPlanVO convertToPlanVO(CleaningPlan plan) {
        CleaningPlanVO vo = new CleaningPlanVO();
        BeanUtils.copyProperties(plan, vo);

        CleaningStatusEnum statusEnum = CleaningStatusEnum.getByCode(plan.getStatus());
        if (statusEnum != null) {
            vo.setStatusDesc(statusEnum.getDesc());
        }

        if (StringUtils.hasText(plan.getBeforeCleanPhotos())) {
            vo.setBeforeCleanPhotoList(Arrays.asList(plan.getBeforeCleanPhotos().split(",")));
        } else {
            vo.setBeforeCleanPhotoList(Collections.emptyList());
        }
        if (StringUtils.hasText(plan.getAfterCleanPhotos())) {
            vo.setAfterCleanPhotoList(Arrays.asList(plan.getAfterCleanPhotos().split(",")));
        } else {
            vo.setAfterCleanPhotoList(Collections.emptyList());
        }

        if (plan.getImprovementRate() != null) {
            vo.setImprovementRatePercent(
                    plan.getImprovementRate().multiply(new BigDecimal("100"))
                            .setScale(2, BigDecimal.ROUND_HALF_UP));
        }

        return vo;
    }

    private String generatePlanNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = IdUtil.getSnowflakeNextIdStr().substring(10);
        return "CP" + dateStr + suffix.toUpperCase();
    }
}
