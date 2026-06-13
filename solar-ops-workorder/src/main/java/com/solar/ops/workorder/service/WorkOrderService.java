package com.solar.ops.workorder.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import com.solar.ops.workorder.dto.WorkOrderCreateDTO;
import com.solar.ops.workorder.dto.WorkOrderHandleDTO;
import com.solar.ops.workorder.dto.WorkOrderQueryDTO;
import com.solar.ops.workorder.engine.FaultRuleEngine;
import com.solar.ops.workorder.entity.WorkOrder;
import com.solar.ops.workorder.entity.WorkOrderLog;
import com.solar.ops.workorder.enums.FaultLevelEnum;
import com.solar.ops.workorder.enums.WorkOrderStatusEnum;
import com.solar.ops.workorder.mapper.WorkOrderMapper;
import com.solar.ops.workorder.vo.WorkOrderStatisticsVO;
import com.solar.ops.workorder.vo.WorkOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderService extends ServiceImpl<WorkOrderMapper, WorkOrder> {

    private final FaultRuleEngine faultRuleEngine;
    private final WorkOrderLogService workOrderLogService;
    private final RedissonClient redissonClient;

    private static final String ORDER_LOCK_PREFIX = "workorder:lock:";

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder createWorkOrder(WorkOrderCreateDTO dto) {
        FaultRuleEngine.FaultMatchResult matchResult = faultRuleEngine.match(dto.getFaultCode());

        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNo(generateOrderNo());
        workOrder.setStationId(dto.getStationId());
        workOrder.setInverterId(dto.getInverterId());
        workOrder.setFaultCode(dto.getFaultCode());
        workOrder.setFaultName(matchResult.getFaultName());
        workOrder.setFaultLevel(dto.getFaultLevel() != null ? dto.getFaultLevel() : matchResult.getFaultLevel());
        workOrder.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription() : matchResult.getFaultDesc());
        workOrder.setSolution(matchResult.getSolution());
        workOrder.setStatus(WorkOrderStatusEnum.PENDING.getCode());

        int expectHours = dto.getExpectHours() != null ? dto.getExpectHours() : matchResult.getExpectHours();
        workOrder.setExpectTime(LocalDateTime.now().plusHours(expectHours));

        this.save(workOrder);

        workOrderLogService.addLog(workOrder.getId(), null, "系统", "创建工单", "系统自动创建工单");

        return workOrder;
    }

    public WorkOrder createWorkOrderByEfficiency(Long stationId, Long inverterId, Double efficiency, Double threshold) {
        FaultRuleEngine.FaultMatchResult matchResult = faultRuleEngine.matchByEfficiency(efficiency, threshold);
        if (matchResult == null) {
            return null;
        }

        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getStationId, stationId);
        if (inverterId != null) {
            wrapper.eq(WorkOrder::getInverterId, inverterId);
        }
        wrapper.eq(WorkOrder::getFaultCode, matchResult.getFaultCode());
        wrapper.in(WorkOrder::getStatus, WorkOrderStatusEnum.PENDING.getCode(), WorkOrderStatusEnum.ACCEPTED.getCode(), WorkOrderStatusEnum.PROCESSING.getCode());
        wrapper.orderByDesc(WorkOrder::getCreateTime);
        wrapper.last("LIMIT 1");

        WorkOrder existOrder = this.getOne(wrapper);
        if (existOrder != null) {
            log.info("已存在同类未完成的效率类工单，stationId: {}, faultCode: {}", stationId, matchResult.getFaultCode());
            return existOrder;
        }

        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNo(generateOrderNo());
        workOrder.setStationId(stationId);
        workOrder.setInverterId(inverterId);
        workOrder.setFaultCode(matchResult.getFaultCode());
        workOrder.setFaultName(matchResult.getFaultName());
        workOrder.setFaultLevel(matchResult.getFaultLevel());
        workOrder.setDescription(matchResult.getFaultDesc());
        workOrder.setSolution(matchResult.getSolution());
        workOrder.setStatus(WorkOrderStatusEnum.PENDING.getCode());
        workOrder.setExpectTime(LocalDateTime.now().plusHours(matchResult.getExpectHours()));

        this.save(workOrder);

        workOrderLogService.addLog(workOrder.getId(), null, "系统", "创建工单", "效率偏低自动创建工单");

        return workOrder;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean acceptOrder(Long orderId, Long operatorId, String operatorName) {
        WorkOrder workOrder = getOrderById(orderId);
        if (!WorkOrderStatusEnum.PENDING.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException("工单状态不允许接单");
        }

        String lockKey = ORDER_LOCK_PREFIX + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                throw new BusinessException("工单正在被其他用户操作，请稍后再试");
            }

            workOrder.setStatus(WorkOrderStatusEnum.ACCEPTED.getCode());
            workOrder.setHandlerId(operatorId);
            workOrder.setHandlerName(operatorName);
            workOrder.setAcceptTime(LocalDateTime.now());
            this.updateById(workOrder);

            workOrderLogService.addLog(orderId, operatorId, operatorName, "接单", "接单成功");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("接单失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean assignOrder(Long orderId, Long handlerId, String handlerName, Long operatorId, String operatorName) {
        WorkOrder workOrder = getOrderById(orderId);
        if (!WorkOrderStatusEnum.PENDING.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException("工单状态不允许派单");
        }

        workOrder.setStatus(WorkOrderStatusEnum.ACCEPTED.getCode());
        workOrder.setHandlerId(handlerId);
        workOrder.setHandlerName(handlerName);
        workOrder.setAcceptTime(LocalDateTime.now());
        this.updateById(workOrder);

        workOrderLogService.addLog(orderId, operatorId, operatorName, "派单", "派单给: " + handlerName);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean startProcess(WorkOrderHandleDTO dto) {
        WorkOrder workOrder = getOrderById(dto.getOrderId());
        if (!WorkOrderStatusEnum.ACCEPTED.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException("工单状态不允许开始处理");
        }

        workOrder.setStatus(WorkOrderStatusEnum.PROCESSING.getCode());
        workOrder.setProcessTime(LocalDateTime.now());
        this.updateById(workOrder);

        workOrderLogService.addLog(dto.getOrderId(), dto.getOperatorId(), dto.getOperatorName(), "开始处理", dto.getRemark());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean submitCheck(WorkOrderHandleDTO dto) {
        WorkOrder workOrder = getOrderById(dto.getOrderId());
        if (!WorkOrderStatusEnum.PROCESSING.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException("工单状态不允许提交验收");
        }

        workOrder.setStatus(WorkOrderStatusEnum.CHECKING.getCode());
        if (StringUtils.hasText(dto.getSolution())) {
            workOrder.setSolution(dto.getSolution());
        }
        this.updateById(workOrder);

        StringBuilder remark = new StringBuilder();
        remark.append(StringUtils.hasText(dto.getRemark()) ? dto.getRemark() : "");
        if (!CollectionUtils.isEmpty(dto.getPhotoUrls())) {
            remark.append(" 维修照片: ").append(dto.getPhotoUrls().size()).append("张");
        }
        workOrderLogService.addLog(dto.getOrderId(), dto.getOperatorId(), dto.getOperatorName(), "提交验收", remark.toString());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean completeOrder(WorkOrderHandleDTO dto) {
        WorkOrder workOrder = getOrderById(dto.getOrderId());
        if (!WorkOrderStatusEnum.CHECKING.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException("工单状态不允许完成");
        }

        workOrder.setStatus(WorkOrderStatusEnum.COMPLETED.getCode());
        workOrder.setCompleteTime(LocalDateTime.now());
        this.updateById(workOrder);

        workOrderLogService.addLog(dto.getOrderId(), dto.getOperatorId(), dto.getOperatorName(), "验收通过", dto.getRemark());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean closeOrder(WorkOrderHandleDTO dto) {
        WorkOrder workOrder = getOrderById(dto.getOrderId());
        if (WorkOrderStatusEnum.COMPLETED.getCode().equals(workOrder.getStatus()) || WorkOrderStatusEnum.CLOSED.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException("工单状态不允许关闭");
        }

        workOrder.setStatus(WorkOrderStatusEnum.CLOSED.getCode());
        workOrder.setCompleteTime(LocalDateTime.now());
        this.updateById(workOrder);

        workOrderLogService.addLog(dto.getOrderId(), dto.getOperatorId(), dto.getOperatorName(), "关闭工单", dto.getRemark());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean rejectCheck(WorkOrderHandleDTO dto) {
        WorkOrder workOrder = getOrderById(dto.getOrderId());
        if (!WorkOrderStatusEnum.CHECKING.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException("工单状态不允许驳回");
        }

        workOrder.setStatus(WorkOrderStatusEnum.PROCESSING.getCode());
        this.updateById(workOrder);

        workOrderLogService.addLog(dto.getOrderId(), dto.getOperatorId(), dto.getOperatorName(), "验收驳回", dto.getRemark());
        return true;
    }

    public PageResult<WorkOrderVO> page(WorkOrderQueryDTO dto) {
        LambdaQueryWrapper<WorkOrder> wrapper = buildQueryWrapper(dto);

        Page<WorkOrder> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<WorkOrder> result = this.page(page, wrapper);

        List<WorkOrderVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.build(result.getTotal(), voList, dto.getPageNum(), dto.getPageSize());
    }

    public WorkOrderVO getDetail(Long id) {
        WorkOrder workOrder = getOrderById(id);
        WorkOrderVO vo = convertToVO(workOrder);

        List<WorkOrderLog> logs = workOrderLogService.getByOrderId(id);
        vo.setLogs(logs.stream().map(log -> {
            com.solar.ops.workorder.vo.WorkOrderLogVO logVO = new com.solar.ops.workorder.vo.WorkOrderLogVO();
            BeanUtils.copyProperties(log, logVO);
            return logVO;
        }).collect(Collectors.toList()));

        return vo;
    }

    public WorkOrderStatisticsVO getStatistics(Long stationId) {
        WorkOrderStatisticsVO statistics = new WorkOrderStatisticsVO();

        LambdaQueryWrapper<WorkOrder> totalWrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            totalWrapper.eq(WorkOrder::getStationId, stationId);
        }
        long total = this.count(totalWrapper);
        statistics.setTotalCount(total);

        statistics.setPendingCount(countByStatus(stationId, WorkOrderStatusEnum.PENDING.getCode()));
        statistics.setProcessingCount(countByStatus(stationId, WorkOrderStatusEnum.PROCESSING.getCode()));
        statistics.setCompletedCount(countByStatus(stationId, WorkOrderStatusEnum.COMPLETED.getCode()));
        statistics.setClosedCount(countByStatus(stationId, WorkOrderStatusEnum.CLOSED.getCode()));

        LambdaQueryWrapper<WorkOrder> overtimeWrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            overtimeWrapper.eq(WorkOrder::getStationId, stationId);
        }
        overtimeWrapper.lt(WorkOrder::getExpectTime, LocalDateTime.now());
        overtimeWrapper.in(WorkOrder::getStatus, WorkOrderStatusEnum.PENDING.getCode(),
                WorkOrderStatusEnum.ACCEPTED.getCode(), WorkOrderStatusEnum.PROCESSING.getCode());
        long overtimeCount = this.count(overtimeWrapper);
        statistics.setOvertimeCount(overtimeCount);

        Map<String, Long> levelMap = new LinkedHashMap<>();
        for (FaultLevelEnum level : FaultLevelEnum.values()) {
            levelMap.put(level.getDesc(), countByLevel(stationId, level.getCode()));
        }
        statistics.setLevelStatistics(levelMap);

        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (WorkOrderStatusEnum status : WorkOrderStatusEnum.values()) {
            statusMap.put(status.getDesc(), countByStatus(stationId, status.getCode()));
        }
        statistics.setStatusStatistics(statusMap);

        return statistics;
    }

    private Long countByStatus(Long stationId, Integer status) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(WorkOrder::getStationId, stationId);
        }
        wrapper.eq(WorkOrder::getStatus, status);
        return this.count(wrapper);
    }

    private Long countByLevel(Long stationId, Integer level) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(WorkOrder::getStationId, stationId);
        }
        wrapper.eq(WorkOrder::getFaultLevel, level);
        return this.count(wrapper);
    }

    private WorkOrder getOrderById(Long id) {
        WorkOrder workOrder = this.getById(id);
        if (workOrder == null) {
            throw new BusinessException("工单不存在");
        }
        return workOrder;
    }

    private WorkOrderVO convertToVO(WorkOrder workOrder) {
        WorkOrderVO vo = new WorkOrderVO();
        BeanUtils.copyProperties(workOrder, vo);

        WorkOrderStatusEnum statusEnum = WorkOrderStatusEnum.getByCode(workOrder.getStatus());
        if (statusEnum != null) {
            vo.setStatusDesc(statusEnum.getDesc());
        }

        FaultLevelEnum levelEnum = FaultLevelEnum.getByCode(workOrder.getFaultLevel());
        if (levelEnum != null) {
            vo.setFaultLevelDesc(levelEnum.getDesc());
        }

        if (workOrder.getExpectTime() != null && !WorkOrderStatusEnum.COMPLETED.getCode().equals(workOrder.getStatus())
                && !WorkOrderStatusEnum.CLOSED.getCode().equals(workOrder.getStatus())) {
            boolean overtime = workOrder.getExpectTime().isBefore(LocalDateTime.now());
            vo.setOvertime(overtime);
            long minutes = Duration.between(LocalDateTime.now(), workOrder.getExpectTime()).toMinutes();
            vo.setRemainingMinutes(minutes);
        } else {
            vo.setOvertime(false);
            vo.setRemainingMinutes(0L);
        }

        return vo;
    }

    private LambdaQueryWrapper<WorkOrder> buildQueryWrapper(WorkOrderQueryDTO dto) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        if (dto.getStationId() != null) {
            wrapper.eq(WorkOrder::getStationId, dto.getStationId());
        }
        if (dto.getInverterId() != null) {
            wrapper.eq(WorkOrder::getInverterId, dto.getInverterId());
        }
        if (StringUtils.hasText(dto.getOrderNo())) {
            wrapper.like(WorkOrder::getOrderNo, dto.getOrderNo());
        }
        if (StringUtils.hasText(dto.getFaultCode())) {
            wrapper.like(WorkOrder::getFaultCode, dto.getFaultCode());
        }
        if (dto.getFaultLevel() != null) {
            wrapper.eq(WorkOrder::getFaultLevel, dto.getFaultLevel());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(WorkOrder::getStatus, dto.getStatus());
        }
        if (dto.getHandlerId() != null) {
            wrapper.eq(WorkOrder::getHandlerId, dto.getHandlerId());
        }
        if (StringUtils.hasText(dto.getHandlerName())) {
            wrapper.like(WorkOrder::getHandlerName, dto.getHandlerName());
        }
        wrapper.orderByDesc(WorkOrder::getCreateTime);
        return wrapper;
    }

    private String generateOrderNo() {
        String dateStr = DateUtil.format(new Date(), "yyyyMMdd");
        String random = IdUtil.randomUUID().substring(0, 8).toUpperCase();
        return "WO" + dateStr + random;
    }
}
