package com.solar.ops.workorder.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.solar.ops.workorder.dto.WorkOrderCreateDTO;
import com.solar.ops.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "fault-alarm-topic", consumerGroup = "workorder-consumer-group")
public class FaultMessageConsumer implements RocketMQListener<String> {

    private final WorkOrderService workOrderService;

    @Override
    public void onMessage(String message) {
        log.info("收到故障告警消息: {}", message);
        try {
            JSONObject jsonObject = JSON.parseObject(message);
            String faultCode = jsonObject.getString("faultCode");
            Long stationId = jsonObject.getLong("stationId");
            Long inverterId = jsonObject.getLong("inverterId");
            String description = jsonObject.getString("description");
            Double efficiency = jsonObject.getDouble("efficiency");
            Double threshold = jsonObject.getDouble("threshold");

            if (StringUtils.hasText(faultCode) && stationId != null) {
                WorkOrderCreateDTO dto = new WorkOrderCreateDTO();
                dto.setStationId(stationId);
                dto.setInverterId(inverterId);
                dto.setFaultCode(faultCode);
                dto.setDescription(description);
                workOrderService.createWorkOrder(dto);
                log.info("故障工单创建成功, faultCode: {}, stationId: {}", faultCode, stationId);
            } else if (efficiency != null && threshold != null && stationId != null) {
                workOrderService.createWorkOrderByEfficiency(stationId, inverterId, efficiency, threshold);
                log.info("效率类工单处理完成, stationId: {}, efficiency: {}, threshold: {}", stationId, efficiency, threshold);
            } else {
                log.warn("消息格式不正确，无法创建工单: {}", message);
            }
        } catch (Exception e) {
            log.error("处理故障消息失败: {}", message, e);
        }
    }
}
