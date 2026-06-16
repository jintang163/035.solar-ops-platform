package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.dto.InspectionTaskQueryDTO;
import com.solar.ops.admin.entity.InspectionItem;
import com.solar.ops.admin.entity.InspectionTask;
import com.solar.ops.admin.entity.InspectionTaskItem;
import com.solar.ops.admin.enums.InspectionTaskStatusEnum;
import com.solar.ops.admin.holder.LoginUserHolder;
import com.solar.ops.admin.mapper.InspectionItemMapper;
import com.solar.ops.admin.mapper.InspectionTaskItemMapper;
import com.solar.ops.admin.mapper.InspectionTaskMapper;
import com.solar.ops.admin.vo.InspectionTaskDetailVO;
import com.solar.ops.admin.vo.TaskItemWithDetailVO;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InspectionTaskService extends ServiceImpl<InspectionTaskMapper, InspectionTask> {

    @Resource
    private InspectionTaskItemMapper taskItemMapper;

    @Resource
    private InspectionItemMapper itemMapper;

    @Resource
    private LoginUserHolder loginUserHolder;

    public PageResult<InspectionTask> page(PageQuery pageQuery, InspectionTaskQueryDTO queryDTO) {
        Long currentUserId = loginUserHolder.getCurrentUserId();
        Page<InspectionTask> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        baseMapper.selectTaskPage(page, currentUserId, queryDTO.getStatus(), queryDTO.getStationId());
        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public InspectionTaskDetailVO getTaskDetail(Long taskId) {
        if (taskId == null) {
            throw new BusinessException("任务ID不能为空");
        }

        InspectionTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        InspectionTaskDetailVO detailVO = new InspectionTaskDetailVO();
        detailVO.setId(task.getId());
        detailVO.setTaskNo(task.getTaskNo());
        detailVO.setTaskName(task.getTaskName());
        detailVO.setStationId(task.getStationId());
        detailVO.setStationName(task.getStationName());
        detailVO.setTaskType(task.getTaskType());
        detailVO.setPriority(task.getPriority());
        detailVO.setPlanStartTime(task.getPlanStartTime());
        detailVO.setPlanEndTime(task.getPlanEndTime());
        detailVO.setStatus(task.getStatus());
        detailVO.setAssigneeId(task.getAssigneeId());
        detailVO.setAssigneeName(task.getAssigneeName());
        detailVO.setDescription(task.getDescription());

        List<InspectionTaskItem> taskItems = taskItemMapper.selectByTaskId(taskId);
        List<TaskItemWithDetailVO> itemDetailList = new ArrayList<>();
        for (InspectionTaskItem taskItem : taskItems) {
            InspectionItem item = itemMapper.selectById(taskItem.getItemId());
            if (item != null) {
                TaskItemWithDetailVO itemDetail = new TaskItemWithDetailVO();
                itemDetail.setTaskItemId(taskItem.getId());
                itemDetail.setItemId(item.getId());
                itemDetail.setItemCode(item.getItemCode());
                itemDetail.setItemName(item.getItemName());
                itemDetail.setItemType(item.getItemType());
                itemDetail.setAssetId(taskItem.getAssetId());
                itemDetail.setAssetName(taskItem.getAssetName());
                itemDetail.setAssetCode(taskItem.getAssetCode());
                itemDetail.setStandardValue(item.getStandardValue());
                itemDetail.setMinValue(item.getMinValue());
                itemDetail.setMaxValue(item.getMaxValue());
                itemDetail.setUnit(item.getUnit());
                itemDetail.setIsRequired(item.getIsRequired());
                itemDetail.setSortOrder(taskItem.getSortOrder());
                itemDetail.setDescription(item.getDescription());
                itemDetailList.add(itemDetail);
            }
        }
        detailVO.setItems(itemDetailList);

        return detailVO;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAsDownloaded(Long taskId) {
        InspectionTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (InspectionTaskStatusEnum.PENDING_DOWNLOAD.getCode().equals(task.getStatus())) {
            task.setStatus(InspectionTaskStatusEnum.DOWNLOADED.getCode());
            updateById(task);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void startTask(Long taskId) {
        InspectionTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (InspectionTaskStatusEnum.DOWNLOADED.getCode().equals(task.getStatus())
                || InspectionTaskStatusEnum.PENDING_DOWNLOAD.getCode().equals(task.getStatus())) {
            task.setStatus(InspectionTaskStatusEnum.IN_PROGRESS.getCode());
            task.setActualStartTime(LocalDateTime.now());
            updateById(task);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId) {
        InspectionTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        task.setStatus(InspectionTaskStatusEnum.COMPLETED.getCode());
        task.setActualEndTime(LocalDateTime.now());
        updateById(task);
    }

    public List<InspectionTaskDetailVO> getTasksForDownload(Long userId) {
        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionTask::getAssigneeId, userId);
        wrapper.in(InspectionTask::getStatus, 
                InspectionTaskStatusEnum.PENDING_DOWNLOAD.getCode(),
                InspectionTaskStatusEnum.DOWNLOADED.getCode(),
                InspectionTaskStatusEnum.IN_PROGRESS.getCode());
        wrapper.orderByDesc(InspectionTask::getPriority);
        wrapper.orderByAsc(InspectionTask::getPlanStartTime);
        
        List<InspectionTask> tasks = list(wrapper);
        List<InspectionTaskDetailVO> result = new ArrayList<>();
        for (InspectionTask task : tasks) {
            result.add(getTaskDetail(task.getId()));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createTask(InspectionTask task, List<Long> itemIds) {
        String taskNo = generateTaskNo();
        task.setTaskNo(taskNo);
        task.setStatus(InspectionTaskStatusEnum.PENDING_DOWNLOAD.getCode());
        save(task);

        if (itemIds != null && !itemIds.isEmpty()) {
            List<InspectionTaskItem> taskItems = new ArrayList<>();
            int sortOrder = 1;
            for (Long itemId : itemIds) {
                InspectionItem item = itemMapper.selectById(itemId);
                if (item != null) {
                    InspectionTaskItem taskItem = new InspectionTaskItem();
                    taskItem.setTaskId(task.getId());
                    taskItem.setItemId(itemId);
                    taskItem.setSortOrder(sortOrder++);
                    taskItem.setCreateTime(LocalDateTime.now());
                    taskItems.add(taskItem);
                }
            }
            if (!taskItems.isEmpty()) {
                taskItemMapper.batchInsert(taskItems);
            }
        }

        return task.getId();
    }

    private String generateTaskNo() {
        String prefix = "IT";
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String codePrefix = prefix + dateStr;

        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(InspectionTask::getTaskNo, codePrefix);
        wrapper.orderByDesc(InspectionTask::getTaskNo);
        wrapper.last("LIMIT 1");
        InspectionTask lastTask = getOne(wrapper);

        int sequence = 1;
        if (lastTask != null && StringUtils.hasText(lastTask.getTaskNo())) {
            String lastCode = lastTask.getTaskNo();
            String seqStr = lastCode.substring(codePrefix.length());
            try {
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }

        return codePrefix + String.format("%03d", sequence);
    }
}
