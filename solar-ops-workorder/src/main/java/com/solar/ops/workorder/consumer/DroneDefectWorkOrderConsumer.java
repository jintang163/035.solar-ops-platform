package com.solar.ops.workorder.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.solar.ops.workorder.dto.WorkOrderCreateDTO;
import com.solar.ops.workorder.entity.WorkOrder;
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
@RocketMQMessageListener(topic = "drone-defect-workorder-topic", consumerGroup = "drone-workorder-consumer-group")
public class DroneDefectWorkOrderConsumer implements RocketMQListener<String> {

    private final WorkOrderService workOrderService;

    @Override
    public void onMessage(String message) {
        log.info("收到无人机缺陷工单消息: {}", message);
        try {
            JSONObject jsonObject = JSON.parseObject(message);
            Long defectId = jsonObject.getLong("defectId");
            Long stationId = jsonObject.getLong("stationId");
            Long imageId = jsonObject.getLong("imageId");
            String defectType = jsonObject.getString("defectType");
            String defectTypeName = jsonObject.getString("defectTypeName");
            Integer defectLevel = jsonObject.getInteger("defectLevel");
            String description = jsonObject.getString("description");

            if (stationId == null) {
                log.warn("电站ID为空，无法创建工单, message: {}", message);
                return;
            }

            WorkOrder workOrder = new WorkOrder();
            workOrder.setStationId(stationId);
            workOrder.setOrderType(3);
            workOrder.setTitle("无人机巡检-" + (StringUtils.hasText(defectTypeName) ? defectTypeName : defectType) + "缺陷处理");

            StringBuilder descBuilder = new StringBuilder();
            if (StringUtils.hasText(description)) {
                descBuilder.append(description);
            } else {
                descBuilder.append(StringUtils.hasText(defectTypeName) ? defectTypeName : defectType)
                        .append("缺陷");
            }

            if (jsonObject.getDouble("centerX") != null && jsonObject.getDouble("centerY") != null) {
                descBuilder.append("，中心位置: (").append(jsonObject.getDouble("centerX"))
                        .append(", ").append(jsonObject.getDouble("centerY")).append(")");
            }
            if (jsonObject.getDouble("maxTemperature") != null) {
                descBuilder.append("，最高温度: ").append(jsonObject.getDouble("maxTemperature")).append("℃");
            }
            workOrder.setDescription(descBuilder.toString());

            workOrder.setFaultLevel(defectLevel != null ? defectLevel : 2);
            workOrder.setSource("DRONE");
            workOrder.setSourceId(defectId);
            workOrder.setDeviceId(imageId);
            workOrder.setDeviceType("DRONE_IMAGE");

            WorkOrder saved = workOrderService.save(workOrder);
            log.info("无人机缺陷工单创建成功, workOrderId: {}, workOrderNo: {}, defectId: {}",
                    saved.getId(), saved.getOrderNo(), defectId);

        } catch (Exception e) {
            log.error("处理无人机缺陷工单消息失败: {}", message, e);
        }
    }
}
