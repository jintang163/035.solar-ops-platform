package com.solar.ops.drone.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.entity.SysUser;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.admin.mapper.SysUserMapper;
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
import com.solar.ops.drone.producer.DroneWorkOrderProducer;
import com.solar.ops.drone.service.DroneDefectService;
import com.solar.ops.drone.vo.DroneDefectVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DroneDefectServiceImpl extends ServiceImpl<DroneDefectMapper, DroneDefect> implements DroneDefectService {

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private DroneInspectionTaskMapper taskMapper;

    @Autowired
    private DroneInspectionImageMapper imageMapper;

    @Autowired(required = false)
    private DroneWorkOrderProducer workOrderProducer;

    @Autowired(required = false)
    private SysUserMapper sysUserMapper;

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

        Long workOrderId = tryGenerateByReflection(defect, dto);

        if (workOrderId == null) {
            workOrderId = tryGenerateByMQ(defect, dto);
        }

        if (workOrderId != null && workOrderId > 0) {
            pushWorkOrderNotificationAsync(defect, workOrderId);
        }

        return workOrderId;
    }

    private Long tryGenerateByReflection(DroneDefect defect, DroneDefectHandleDTO dto) {
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

            String desc = buildDefectDescription(defect, dto, typeName);
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

            log.info("反射方式创建工单成功, defectId: {}, workOrderId: {}, workOrderNo: {}",
                    defect.getId(), workOrderId, workOrderNo);
            return workOrderId;

        } catch (ClassNotFoundException e) {
            log.warn("工单模块类不存在，将尝试MQ方式, defectId: {}", defect.getId());
            return null;
        } catch (Exception e) {
            log.error("反射方式创建工单失败，将尝试MQ方式, defectId: {}", defect.getId(), e);
            return null;
        }
    }

    private Long tryGenerateByMQ(DroneDefect defect, DroneDefectHandleDTO dto) {
        if (workOrderProducer == null || !workOrderProducer.isAvailable()) {
            throw new BusinessException("工单服务不可用，请稍后重试");
        }

        DefectTypeEnum typeEnum = DefectTypeEnum.getByCode(defect.getDefectType());
        String typeName = typeEnum != null ? typeEnum.getDesc() : defect.getDefectType();

        DroneWorkOrderProducer.DroneWorkOrderMessage message = DroneWorkOrderProducer.DroneWorkOrderMessage.builder()
                .defectId(defect.getId())
                .stationId(defect.getStationId())
                .imageId(defect.getImageId())
                .taskId(defect.getTaskId())
                .defectType(defect.getDefectType())
                .defectTypeName(typeName)
                .defectLevel(defect.getDefectLevel())
                .description(buildDefectDescription(defect, dto, typeName))
                .bboxX1(defect.getBboxX1())
                .bboxY1(defect.getBboxY1())
                .bboxX2(defect.getBboxX2())
                .bboxY2(defect.getBboxY2())
                .centerX(defect.getCenterX())
                .centerY(defect.getCenterY())
                .maxTemperature(defect.getMaxTemperature())
                .minTemperature(defect.getMinTemperature())
                .avgTemperature(defect.getAvgTemperature())
                .createTime(LocalDateTime.now().toString())
                .build();

        boolean sent = workOrderProducer.sendWorkOrderCreateMessage(message);
        if (!sent) {
            throw new BusinessException("工单创建消息发送失败，请稍后重试");
        }

        defect.setWorkOrderStatus(99);
        this.updateById(defect);

        log.info("MQ方式发送工单创建消息成功, defectId: {}", defect.getId());
        return 0L;
    }

    private String buildDefectDescription(DroneDefect defect, DroneDefectHandleDTO dto, String typeName) {
        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            return dto.getDescription();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(typeName).append("缺陷");
        if (defect.getCenterX() != null && defect.getCenterY() != null) {
            sb.append("，中心位置: (").append(defect.getCenterX())
                    .append(", ").append(defect.getCenterY()).append(")");
        }
        if (defect.getMaxTemperature() != null) {
            sb.append("，最高温度: ").append(defect.getMaxTemperature()).append("℃");
        }
        return sb.toString();
    }

    @Async
    public void pushWorkOrderNotificationAsync(DroneDefect defect, Long workOrderId) {
        try {
            if (workOrderProducer == null || !workOrderProducer.isAvailable()) {
                log.debug("MQ推送不可用，跳过工单通知推送");
                return;
            }

            String receiverPhone = getStationManagerPhone(defect.getStationId());
            if (receiverPhone == null || receiverPhone.isEmpty()) {
                log.warn("未找到电站管理员手机号，跳过推送, stationId: {}", defect.getStationId());
                return;
            }

            DefectTypeEnum typeEnum = DefectTypeEnum.getByCode(defect.getDefectType());
            String typeName = typeEnum != null ? typeEnum.getDesc() : defect.getDefectType();

            String title = "新工单提醒";
            String content = "您有一条新的无人机巡检工单：" + typeName + "缺陷待处理";

            Map<String, String> extras = new HashMap<>();
            extras.put("defectId", String.valueOf(defect.getId()));
            extras.put("stationId", String.valueOf(defect.getStationId()));
            extras.put("defectType", defect.getDefectType());
            extras.put("pageType", "workorder_detail");

            workOrderProducer.sendWorkOrderCreatedPush(
                    workOrderId,
                    defect.getWorkOrderNo(),
                    title,
                    content,
                    receiverPhone,
                    extras
            );

            log.info("工单创建推送消息已发送, workOrderId: {}, phone: {}", workOrderId, receiverPhone);
        } catch (Exception e) {
            log.warn("工单创建推送失败, workOrderId: {}", workOrderId, e);
        }
    }

    private String getStationManagerPhone(Long stationId) {
        try {
            if (sysUserMapper == null) {
                return null;
            }
            Station station = stationMapper.selectById(stationId);
            if (station == null || station.getManagerId() == null) {
                return null;
            }
            SysUser user = sysUserMapper.selectById(station.getManagerId());
            return user != null ? user.getPhone() : null;
        } catch (Exception e) {
            log.warn("获取电站管理员手机号失败, stationId: {}", stationId, e);
            return null;
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
