package com.solar.ops.drone.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.drone.dto.DroneTaskCreateDTO;
import com.solar.ops.drone.dto.DroneTaskQueryDTO;
import com.solar.ops.drone.entity.DroneDefect;
import com.solar.ops.drone.entity.DroneInspectionImage;
import com.solar.ops.drone.entity.DroneInspectionTask;
import com.solar.ops.drone.enums.TaskStatusEnum;
import com.solar.ops.drone.mapper.DroneDefectMapper;
import com.solar.ops.drone.mapper.DroneInspectionImageMapper;
import com.solar.ops.drone.mapper.DroneInspectionTaskMapper;
import com.solar.ops.drone.service.DroneInspectionTaskService;
import com.solar.ops.drone.vo.DroneStatisticsVO;
import com.solar.ops.drone.vo.DroneTaskVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DroneInspectionTaskServiceImpl extends ServiceImpl<DroneInspectionTaskMapper, DroneInspectionTask> implements DroneInspectionTaskService {

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private DroneInspectionImageMapper imageMapper;

    @Autowired
    private DroneDefectMapper defectMapper;

    @Override
    public IPage<DroneTaskVO> page(DroneTaskQueryDTO dto) {
        Page<DroneInspectionTask> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<DroneInspectionTask> wrapper = new LambdaQueryWrapper<>();

        if (dto.getStationId() != null) {
            wrapper.eq(DroneInspectionTask::getStationId, dto.getStationId());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(DroneInspectionTask::getStatus, dto.getStatus());
        }
        if (dto.getTaskName() != null && !dto.getTaskName().isEmpty()) {
            wrapper.like(DroneInspectionTask::getTaskName, dto.getTaskName());
        }
        if (dto.getStartTime() != null) {
            wrapper.ge(DroneInspectionTask::getInspectionTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            wrapper.le(DroneInspectionTask::getInspectionTime, dto.getEndTime());
        }

        wrapper.orderByDesc(DroneInspectionTask::getCreateTime);
        Page<DroneInspectionTask> taskPage = this.page(page, wrapper);

        return taskPage.convert(task -> {
            DroneTaskVO vo = new DroneTaskVO();
            BeanUtils.copyProperties(task, vo);

            Station station = stationMapper.selectById(task.getStationId());
            if (station != null) {
                vo.setStationName(station.getStationName());
            }

            LambdaQueryWrapper<DroneInspectionImage> imageWrapper = new LambdaQueryWrapper<>();
            imageWrapper.eq(DroneInspectionImage::getTaskId, task.getId());
            List<DroneInspectionImage> images = imageMapper.selectList(imageWrapper);
            vo.setImageCount(images.size());
            vo.setDetectedImageCount((int) images.stream().filter(img -> img.getDetectStatus() == 2).count());

            LambdaQueryWrapper<DroneDefect> defectWrapper = new LambdaQueryWrapper<>();
            defectWrapper.eq(DroneDefect::getTaskId, task.getId());
            List<DroneDefect> defects = defectMapper.selectList(defectWrapper);
            vo.setDefectCount(defects.size());
            vo.setConfirmedDefectCount((int) defects.stream().filter(d -> d.getConfirmed() == 1).count());

            return vo;
        });
    }

    @Override
    public DroneTaskVO getDetail(Long id) {
        DroneInspectionTask task = this.getById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        DroneTaskVO vo = new DroneTaskVO();
        BeanUtils.copyProperties(task, vo);

        Station station = stationMapper.selectById(task.getStationId());
        if (station != null) {
            vo.setStationName(station.getStationName());
        }

        LambdaQueryWrapper<DroneInspectionImage> imageWrapper = new LambdaQueryWrapper<>();
        imageWrapper.eq(DroneInspectionImage::getTaskId, id);
        List<DroneInspectionImage> images = imageMapper.selectList(imageWrapper);
        vo.setImageCount(images.size());
        vo.setDetectedImageCount((int) images.stream().filter(img -> img.getDetectStatus() == 2).count());

        LambdaQueryWrapper<DroneDefect> defectWrapper = new LambdaQueryWrapper<>();
        defectWrapper.eq(DroneDefect::getTaskId, id);
        List<DroneDefect> defects = defectMapper.selectList(defectWrapper);
        vo.setDefectCount(defects.size());
        vo.setConfirmedDefectCount((int) defects.stream().filter(d -> d.getConfirmed() == 1).count());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DroneTaskCreateDTO dto) {
        Station station = stationMapper.selectById(dto.getStationId());
        if (station == null) {
            throw new BusinessException("电站不存在");
        }

        DroneInspectionTask task = new DroneInspectionTask();
        BeanUtils.copyProperties(dto, task);
        task.setStatus(TaskStatusEnum.PENDING.getCode());
        task.setInspectionTime(dto.getInspectionTime() != null ? dto.getInspectionTime() : LocalDateTime.now());

        this.save(task);
        return task.getId();
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        DroneInspectionTask task = this.getById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        task.setStatus(status);
        return this.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        DroneInspectionTask task = this.getById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        LambdaQueryWrapper<DroneDefect> defectWrapper = new LambdaQueryWrapper<>();
        defectWrapper.eq(DroneDefect::getTaskId, id);
        defectMapper.delete(defectWrapper);

        LambdaQueryWrapper<DroneInspectionImage> imageWrapper = new LambdaQueryWrapper<>();
        imageWrapper.eq(DroneInspectionImage::getTaskId, id);
        imageMapper.delete(imageWrapper);

        return this.removeById(id);
    }

    @Override
    public DroneStatisticsVO getStatistics(Long stationId) {
        DroneStatisticsVO vo = new DroneStatisticsVO();

        LambdaQueryWrapper<DroneInspectionTask> taskWrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            taskWrapper.eq(DroneInspectionTask::getStationId, stationId);
        }
        List<DroneInspectionTask> tasks = this.list(taskWrapper);
        vo.setTotalTasks(tasks.size());
        vo.setCompletedTasks((int) tasks.stream().filter(t -> t.getStatus() == 2).count());

        LambdaQueryWrapper<DroneInspectionImage> imageWrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            List<Long> taskIds = tasks.stream().map(DroneInspectionTask::getId).collect(java.util.stream.Collectors.toList());
            if (!taskIds.isEmpty()) {
                imageWrapper.in(DroneInspectionImage::getTaskId, taskIds);
            } else {
                imageWrapper.eq(DroneInspectionImage::getId, -1);
            }
        }
        List<DroneInspectionImage> images = imageMapper.selectList(imageWrapper);
        vo.setTotalImages(images.size());

        LambdaQueryWrapper<DroneDefect> defectWrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            List<Long> taskIds = tasks.stream().map(DroneInspectionTask::getId).collect(java.util.stream.Collectors.toList());
            if (!taskIds.isEmpty()) {
                defectWrapper.in(DroneDefect::getTaskId, taskIds);
            } else {
                defectWrapper.eq(DroneDefect::getId, -1);
            }
        }
        List<DroneDefect> defects = defectMapper.selectList(defectWrapper);
        vo.setTotalDefects(defects.size());
        vo.setPendingDefects((int) defects.stream().filter(d -> d.getConfirmed() == 0).count());
        vo.setConfirmedDefects((int) defects.stream().filter(d -> d.getConfirmed() == 1).count());
        vo.setSeriousDefects((int) defects.stream().filter(d -> d.getDefectLevel() == 3).count());

        return vo;
    }
}
