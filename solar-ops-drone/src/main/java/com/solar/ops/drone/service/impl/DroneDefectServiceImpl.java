package com.solar.ops.drone.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.drone.dto.DroneDefectHandleDTO;
import com.solar.ops.drone.dto.DroneDefectQueryDTO;
import com.solar.ops.drone.entity.DroneDefect;
import com.solar.ops.drone.entity.DroneInspectionImage;
import com.solar.ops.drone.entity.DroneInspectionTask;
import com.solar.ops.drone.enums.DefectTypeEnum;
import com.solar.ops.drone.mapper.DroneDefectMapper;
import com.solar.ops.drone.mapper.DroneInspectionImageMapper;
import com.solar.ops.drone.mapper.DroneInspectionTaskMapper;
import com.solar.ops.drone.service.DroneDefectService;
import com.solar.ops.drone.vo.DroneDefectVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DroneDefectServiceImpl extends ServiceImpl<DroneDefectMapper, DroneDefect> implements DroneDefectService {

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private DroneInspectionTaskMapper taskMapper;

    @Autowired
    private DroneInspectionImageMapper imageMapper;

    @Override
    public IPage<DroneDefectVO> page(DroneDefectQueryDTO dto) {
        Page<DroneDefect> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<DroneDefect> wrapper = new LambdaQueryWrapper<>();

        if (dto.getTaskId() != null) {
            wrapper.eq(DroneDefect::getTaskId, dto.getTaskId());
        }
        if (dto.getImageId() != null) {
            wrapper.eq(DroneDefect::getImageId, dto.getImageId());
        }
        if (dto.getStationId() != null) {
            wrapper.eq(DroneDefect::getStationId, dto.getStationId());
        }
        if (dto.getDefectType() != null && !dto.getDefectType().isEmpty()) {
            wrapper.eq(DroneDefect::getDefectType, dto.getDefectType());
        }
        if (dto.getDefectLevel() != null) {
            wrapper.eq(DroneDefect::getDefectLevel, dto.getDefectLevel());
        }
        if (dto.getConfirmed() != null) {
            wrapper.eq(DroneDefect::getConfirmed, dto.getConfirmed());
        }
        if (dto.getWorkOrderId() != null) {
            wrapper.eq(DroneDefect::getWorkOrderId, dto.getWorkOrderId());
        }
        if (dto.getHasWorkOrder() != null) {
            if (dto.getHasWorkOrder()) {
                wrapper.isNotNull(DroneDefect::getWorkOrderId);
            } else {
                wrapper.isNull(DroneDefect::getWorkOrderId);
            }
        }

        wrapper.orderByDesc(DroneDefect::getCreateTime);
        Page<DroneDefect> defectPage = this.page(page, wrapper);

        return defectPage.convert(defect -> convertToVO(defect));
    }

    @Override
    public DroneDefectVO getDetail(Long id) {
        DroneDefect defect = this.getById(id);
        if (defect == null) {
            throw new BusinessException("缺陷不存在");
        }
        return convertToVO(defect);
    }

    private DroneDefectVO convertToVO(DroneDefect defect) {
        DroneDefectVO vo = new DroneDefectVO();
        BeanUtils.copyProperties(defect, vo);

        Station station = stationMapper.selectById(defect.getStationId());
        if (station != null) {
            vo.setStationName(station.getStationName());
        }

        DroneInspectionTask task = taskMapper.selectById(defect.getTaskId());
        if (task != null) {
            vo.setTaskName(task.getTaskName());
        }

        DroneInspectionImage image = imageMapper.selectById(defect.getImageId());
        if (image != null) {
            vo.setImageUrl(image.getImageUrl());
            vo.setAnnotatedImageUrl(image.getAnnotatedImageUrl());
        }

        DefectTypeEnum typeEnum = DefectTypeEnum.getByCode(defect.getDefectType());
        if (typeEnum != null) {
            vo.setDefectTypeName(typeEnum.getDesc());
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmDefect(DroneDefectHandleDTO dto) {
        DroneDefect defect = this.getById(dto.getId());
        if (defect == null) {
            throw new BusinessException("缺陷不存在");
        }

        defect.setConfirmed(dto.getConfirmed() != null ? dto.getConfirmed() : 1);
        defect.setConfirmTime(LocalDateTime.now());
        defect.setConfirmRemark(dto.getConfirmRemark());
        if (dto.getDefectLevel() != null) {
            defect.setDefectLevel(dto.getDefectLevel());
        }
        if (dto.getDescription() != null) {
            defect.setDescription(dto.getDescription());
        }

        return this.updateById(defect);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateWorkOrder(DroneDefectHandleDTO dto) {
        DroneDefect defect = this.getById(dto.getId());
        if (defect == null) {
            throw new BusinessException("缺陷不存在");
        }
        if (defect.getWorkOrderId() != null) {
            throw new BusinessException("该缺陷已生成工单");
        }

        try {
            Class<?> workOrderServiceClass = Class.forName("com.solar.ops.workorder.service.WorkOrderService");
            Object workOrderService = com.solar.ops.common.context.ApplicationContextProvider.getBean(workOrderServiceClass);

            Class<?> workOrderClass = Class.forName("com.solar.ops.workorder.entity.WorkOrder");
            Object workOrder = workOrderClass.getDeclaredConstructor().newInstance();

            java.lang.reflect.Method setStationId = workOrderClass.getMethod("setStationId", Long.class);
            setStationId.invoke(workOrder, defect.getStationId());

            java.lang.reflect.Method setOrderType = workOrderClass.getMethod("setOrderType", Integer.class);
            setOrderType.invoke(workOrder, 3);

            DefectTypeEnum typeEnum = DefectTypeEnum.getByCode(defect.getDefectType());
            String typeName = typeEnum != null ? typeEnum.getDesc() : defect.getDefectType();
            java.lang.reflect.Method setTitle = workOrderClass.getMethod("setTitle", String.class);
            setTitle.invoke(workOrder, "无人机巡检-" + typeName + "缺陷处理");

            String desc = dto.getDescription() != null ? dto.getDescription() :
                    (typeName + "缺陷，位置: (" + defect.getCenterX() + ", " + defect.getCenterY() + ")");
            java.lang.reflect.Method setDescription = workOrderClass.getMethod("setDescription", String.class);
            setDescription.invoke(workOrder, desc);

            java.lang.reflect.Method setFaultLevel = workOrderClass.getMethod("setFaultLevel", Integer.class);
            setFaultLevel.invoke(workOrder, defect.getDefectLevel());

            java.lang.reflect.Method setSource = workOrderClass.getMethod("setSource", String.class);
            setSource.invoke(workOrder, "DRONE");

            java.lang.reflect.Method setSourceId = workOrderClass.getMethod("setSourceId", Long.class);
            setSourceId.invoke(workOrder, defect.getId());

            java.lang.reflect.Method setDeviceId = workOrderClass.getMethod("setDeviceId", Long.class);
            setDeviceId.invoke(workOrder, defect.getImageId());

            java.lang.reflect.Method setDeviceType = workOrderClass.getMethod("setDeviceType", String.class);
            setDeviceType.invoke(workOrder, "DRONE_IMAGE");

            java.lang.reflect.Method saveMethod = workOrderServiceClass.getMethod("save", workOrderClass);
            saveMethod.invoke(workOrderService, workOrder);

            java.lang.reflect.Method getIdMethod = workOrderClass.getMethod("getId");
            Long workOrderId = (Long) getIdMethod.invoke(workOrder);

            java.lang.reflect.Method getOrderNoMethod = workOrderClass.getMethod("getOrderNo");
            String workOrderNo = (String) getOrderNoMethod.invoke(workOrder);

            defect.setWorkOrderId(workOrderId);
            defect.setWorkOrderNo(workOrderNo);
            defect.setWorkOrderStatus(0);
            this.updateById(defect);

            return workOrderId;
        } catch (ClassNotFoundException e) {
            throw new BusinessException("工单模块未启用，请先启用工单模块");
        } catch (Exception e) {
            log.error("生成工单失败", e);
            throw new BusinessException("生成工单失败: " + e.getMessage());
        }
    }

    @Override
    public List<DroneDefectVO> getByImageId(Long imageId) {
        LambdaQueryWrapper<DroneDefect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DroneDefect::getImageId, imageId);
        wrapper.orderByAsc(DroneDefect::getBboxY1);
        List<DroneDefect> defects = this.list(wrapper);
        return defects.stream().map(this::convertToVO).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<DroneDefectVO> getByTaskId(Long taskId) {
        LambdaQueryWrapper<DroneDefect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DroneDefect::getTaskId, taskId);
        wrapper.orderByDesc(DroneDefect::getCreateTime);
        List<DroneDefect> defects = this.list(wrapper);
        return defects.stream().map(this::convertToVO).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public int countUnconfirmed(Long stationId) {
        LambdaQueryWrapper<DroneDefect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DroneDefect::getConfirmed, 0);
        if (stationId != null) {
            wrapper.eq(DroneDefect::getStationId, stationId);
        }
        return this.count(wrapper);
    }

    @Override
    public int countSerious(Long stationId) {
        LambdaQueryWrapper<DroneDefect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DroneDefect::getDefectLevel, 3);
        if (stationId != null) {
            wrapper.eq(DroneDefect::getStationId, stationId);
        }
        return this.count(wrapper);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DroneDefectServiceImpl.class);
}
