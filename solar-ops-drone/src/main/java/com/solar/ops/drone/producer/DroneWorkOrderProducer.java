package com.solar.ops.drone.producer;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DroneWorkOrderProducer {

    private static final String DRONE_WORKORDER_TOPIC = "drone-defect-workorder-topic";
    private static final String APP_PUSH_TOPIC = "app-push-topic";

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    public boolean sendWorkOrderCreateMessage(DroneWorkOrderMessage message) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQTemplate未初始化，无法发送工单消息");
            return false;
        }

        try {
            String json = JSON.toJSONString(message);
            rocketMQTemplate.syncSend(DRONE_WORKORDER_TOPIC, MessageBuilder.withPayload(json).build());
            log.info("无人机缺陷工单创建消息发送成功, defectId: {}", message.getDefectId());
            return true;
        } catch (Exception e) {
            log.error("无人机缺陷工单创建消息发送失败, defectId: {}", message.getDefectId(), e);
            return false;
        }
    }

    public boolean sendWorkOrderCreatedPush(Long workOrderId, String workOrderNo, String title,
                                            String content, String receiverPhone, Map<String, String> extras) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQTemplate未初始化，无法发送推送消息");
            return false;
        }

        try {
            Map<String, Object> pushMsg = new HashMap<>();
            pushMsg.put("type", "WORKORDER_CREATED");
            pushMsg.put("workOrderId", workOrderId);
            pushMsg.put("workOrderNo", workOrderNo);
            pushMsg.put("title", title);
            pushMsg.put("content", content);
            pushMsg.put("receiverPhone", receiverPhone);
            pushMsg.put("extras", extras);
            pushMsg.put("createTime", LocalDateTime.now().toString());

            String json = JSON.toJSONString(pushMsg);
            rocketMQTemplate.syncSend(APP_PUSH_TOPIC, MessageBuilder.withPayload(json).build());
            log.info("工单创建推送消息发送成功, workOrderId: {}, workOrderNo: {}", workOrderId, workOrderNo);
            return true;
        } catch (Exception e) {
            log.error("工单创建推送消息发送失败, workOrderId: {}", workOrderId, e);
            return false;
        }
    }

    public boolean isAvailable() {
        return rocketMQTemplate != null;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DroneWorkOrderMessage implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long defectId;

        private Long stationId;

        private Long imageId;

        private Long taskId;

        private String defectType;

        private String defectTypeName;

        private Integer defectLevel;

        private String description;

        private Double bboxX1;
        private Double bboxY1;
        private Double bboxX2;
        private Double bboxY2;

        private Double centerX;
        private Double centerY;

        private Double maxTemperature;
        private Double minTemperature;
        private Double avgTemperature;

        private String source = "DRONE";

        private String createTime;
    }
}
