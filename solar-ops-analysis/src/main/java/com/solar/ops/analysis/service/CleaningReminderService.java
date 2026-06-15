package com.solar.ops.analysis.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.analysis.dto.CleaningReminderQueryDTO;
import com.solar.ops.analysis.entity.CleaningReminder;
import com.solar.ops.analysis.entity.DustAccumulationRecord;
import com.solar.ops.analysis.enums.DustLevelEnum;
import com.solar.ops.analysis.mapper.CleaningReminderMapper;
import com.solar.ops.analysis.util.DustAccumulationCalculator;
import com.solar.ops.analysis.vo.CleaningReminderVO;
import com.solar.ops.common.page.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CleaningReminderService extends ServiceImpl<CleaningReminderMapper, CleaningReminder> {

    @Autowired
    private DustAccumulationService dustAccumulationService;

    public PageResult<CleaningReminderVO> queryReminderPage(CleaningReminderQueryDTO queryDTO) {
        LambdaQueryWrapper<CleaningReminder> wrapper = buildReminderQueryWrapper(queryDTO);
        Page<CleaningReminder> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<CleaningReminder> result = page(page, wrapper);
        List<CleaningReminderVO> voList = result.getRecords().stream()
                .map(this::convertToReminderVO)
                .collect(Collectors.toList());
        return PageResult.build(voList, result.getTotal());
    }

    public List<CleaningReminderVO> queryReminderList(CleaningReminderQueryDTO queryDTO) {
        LambdaQueryWrapper<CleaningReminder> wrapper = buildReminderQueryWrapper(queryDTO);
        wrapper.orderByDesc(CleaningReminder::getCreateTime);
        return list(wrapper).stream()
                .map(this::convertToReminderVO)
                .collect(Collectors.toList());
    }

    public CleaningReminderVO getReminderDetail(Long id) {
        CleaningReminder reminder = getById(id);
        return reminder != null ? convertToReminderVO(reminder) : null;
    }

    public List<CleaningReminder> generateCleaningReminders(LocalDate detectDate) {
        List<DustAccumulationRecord> needReminderRecords =
                dustAccumulationService.getRecordsNeedReminder(detectDate, 5);

        if (CollectionUtils.isEmpty(needReminderRecords)) {
            return new ArrayList<>();
        }

        List<CleaningReminder> reminders = new ArrayList<>();

        for (DustAccumulationRecord record : needReminderRecords) {
            LambdaQueryWrapper<CleaningReminder> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(CleaningReminder::getDustRecordId, record.getId());
            if (count(existWrapper) > 0) {
                continue;
            }

            DustLevelEnum dustLevel = DustLevelEnum.getByCode(record.getDustLevel());
            LocalDate suggestDate = detectDate.plusDays(1);
            LocalDate deadlineDate;
            if (dustLevel == DustLevelEnum.HEAVY) {
                deadlineDate = detectDate.plusDays(3);
            } else if (dustLevel == DustLevelEnum.MODERATE) {
                deadlineDate = detectDate.plusDays(7);
            } else {
                deadlineDate = detectDate.plusDays(14);
            }

            String arrayDesc = StringUtils.hasText(record.getArrayNumber())
                    ? record.getArrayNumber()
                    : (record.getInverterName() != null ? record.getInverterName() : "方阵");

            String title = buildReminderTitle(arrayDesc, dustLevel);
            String description = buildReminderDescription(record, dustLevel, suggestDate, deadlineDate);

            CleaningReminder reminder = new CleaningReminder();
            reminder.setReminderNo(generateReminderNo());
            reminder.setStationId(record.getStationId());
            reminder.setStationName(record.getStationName());
            reminder.setInverterId(record.getInverterId());
            reminder.setInverterName(record.getInverterName());
            reminder.setArrayNumber(record.getArrayNumber());
            reminder.setDustLevel(record.getDustLevel());
            reminder.setAttenuationRate(record.getAttenuationRate());
            reminder.setEstimatedDailyLoss(record.getEstimatedLossEnergy());
            reminder.setDustRecordId(record.getId());
            reminder.setSuggestCleanDate(suggestDate);
            reminder.setDeadlineDate(deadlineDate);
            reminder.setTitle(title);
            reminder.setDescription(description);
            reminder.setStatus(0);

            reminders.add(reminder);
            dustAccumulationService.markAsHasReminder(record.getId());
        }

        if (!CollectionUtils.isEmpty(reminders)) {
            saveBatch(reminders);
        }

        return reminders;
    }

    public void markAsCreatedPlan(Long reminderId, Long planId, Long handlerId, String handlerName) {
        CleaningReminder reminder = getById(reminderId);
        if (reminder != null) {
            reminder.setStatus(1);
            reminder.setCleaningPlanId(planId);
            reminder.setHandlerId(handlerId);
            reminder.setHandlerName(handlerName);
            reminder.setHandleTime(LocalDateTime.now());
            updateById(reminder);
        }
    }

    public void ignoreReminder(Long reminderId, Long handlerId, String handlerName) {
        CleaningReminder reminder = getById(reminderId);
        if (reminder != null) {
            reminder.setStatus(2);
            reminder.setHandlerId(handlerId);
            reminder.setHandlerName(handlerName);
            reminder.setHandleTime(LocalDateTime.now());
            updateById(reminder);
        }
    }

    public long countUnhandledReminders(Long stationId) {
        LambdaQueryWrapper<CleaningReminder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CleaningReminder::getStatus, 0);
        if (stationId != null) {
            wrapper.eq(CleaningReminder::getStationId, stationId);
        }
        wrapper.le(CleaningReminder::getDeadlineDate, LocalDate.now().plusDays(3));
        return count(wrapper);
    }

    private LambdaQueryWrapper<CleaningReminder> buildReminderQueryWrapper(CleaningReminderQueryDTO queryDTO) {
        LambdaQueryWrapper<CleaningReminder> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getStationId() != null) {
            wrapper.eq(CleaningReminder::getStationId, queryDTO.getStationId());
        }
        if (queryDTO.getInverterId() != null) {
            wrapper.eq(CleaningReminder::getInverterId, queryDTO.getInverterId());
        }
        if (StringUtils.hasText(queryDTO.getArrayNumber())) {
            wrapper.like(CleaningReminder::getArrayNumber, queryDTO.getArrayNumber());
        }
        if (queryDTO.getDustLevel() != null) {
            wrapper.eq(CleaningReminder::getDustLevel, queryDTO.getDustLevel());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(CleaningReminder::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getStartDate() != null && queryDTO.getEndDate() != null) {
            wrapper.between(CleaningReminder::getSuggestCleanDate,
                    queryDTO.getStartDate(), queryDTO.getEndDate());
        }
        wrapper.orderByDesc(CleaningReminder::getCreateTime);
        return wrapper;
    }

    private CleaningReminderVO convertToReminderVO(CleaningReminder reminder) {
        CleaningReminderVO vo = new CleaningReminderVO();
        vo.setId(reminder.getId());
        vo.setReminderNo(reminder.getReminderNo());
        vo.setStationId(reminder.getStationId());
        vo.setStationName(reminder.getStationName());
        vo.setInverterId(reminder.getInverterId());
        vo.setInverterName(reminder.getInverterName());
        vo.setArrayNumber(reminder.getArrayNumber());
        vo.setDustLevel(reminder.getDustLevel());
        vo.setAttenuationRate(reminder.getAttenuationRate());
        vo.setAttenuationRatePercent(
                DustAccumulationCalculator.calculateAttenuationRatePercentage(reminder.getAttenuationRate()));
        vo.setEstimatedDailyLoss(reminder.getEstimatedDailyLoss());
        vo.setSuggestCleanDate(reminder.getSuggestCleanDate());
        vo.setDeadlineDate(reminder.getDeadlineDate());
        vo.setTitle(reminder.getTitle());
        vo.setDescription(reminder.getDescription());
        vo.setStatus(reminder.getStatus());
        vo.setCleaningPlanId(reminder.getCleaningPlanId());
        vo.setCreateTime(reminder.getCreateTime());

        DustLevelEnum levelEnum = DustLevelEnum.getByCode(reminder.getDustLevel());
        if (levelEnum != null) {
            vo.setDustLevelDesc(levelEnum.getDesc());
            vo.setDustLevelColor(levelEnum.getColor());
        }

        if (reminder.getStatus() != null) {
            switch (reminder.getStatus()) {
                case 0:
                    vo.setStatusDesc("未处理");
                    break;
                case 1:
                    vo.setStatusDesc("已创建计划");
                    break;
                case 2:
                    vo.setStatusDesc("已忽略");
                    break;
                default:
                    vo.setStatusDesc("未知");
            }
        }

        return vo;
    }

    private String generateReminderNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = IdUtil.getSnowflakeNextIdStr().substring(10);
        return "CR" + dateStr + suffix.toUpperCase();
    }

    private String buildReminderTitle(String arrayDesc, DustLevelEnum dustLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append(arrayDesc);
        if (dustLevel == DustLevelEnum.HEAVY) {
            sb.append("清洗紧急推荐");
        } else {
            sb.append("清洗推荐");
        }
        return sb.toString();
    }

    private String buildReminderDescription(DustAccumulationRecord record,
                                             DustLevelEnum dustLevel,
                                             LocalDate suggestDate,
                                             LocalDate deadlineDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("检测到").append(record.getArrayNumber() != null ? record.getArrayNumber() : "方阵");
        sb.append("存在").append(dustLevel != null ? dustLevel.getDesc() : "积灰问题");
        sb.append("，发电量衰减率约")
                .append(DustAccumulationCalculator.calculateAttenuationRatePercentage(
                        record.getAttenuationRate())).append("%");
        sb.append("，预估每日损失电量约").append(record.getEstimatedLossEnergy()).append("kWh");
        sb.append("。建议于").append(suggestDate).append("至").append(deadlineDate);
        sb.append("期间安排清洗作业，以避免发电量进一步损失。");
        if (record.getContinuousDeclineDays() > 0) {
            sb.append("该方阵发电量已连续下降").append(record.getContinuousDeclineDays()).append("天。");
        }
        return sb.toString();
    }
}
