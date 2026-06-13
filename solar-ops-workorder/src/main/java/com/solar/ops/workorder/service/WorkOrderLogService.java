package com.solar.ops.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.workorder.entity.WorkOrderLog;
import com.solar.ops.workorder.mapper.WorkOrderLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkOrderLogService extends ServiceImpl<WorkOrderLogMapper, WorkOrderLog> {

    public List<WorkOrderLog> getByOrderId(Long orderId) {
        LambdaQueryWrapper<WorkOrderLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderLog::getOrderId, orderId);
        wrapper.orderByAsc(WorkOrderLog::getCreateTime);
        return this.list(wrapper);
    }

    public boolean addLog(Long orderId, Long operatorId, String operatorName, String action, String remark) {
        WorkOrderLog log = new WorkOrderLog();
        log.setOrderId(orderId);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setAction(action);
        log.setRemark(remark);
        return this.save(log);
    }
}
