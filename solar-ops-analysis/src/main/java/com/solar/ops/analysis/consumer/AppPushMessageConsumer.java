package com.solar.ops.analysis.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.solar.ops.analysis.service.AppPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "app-push-topic", consumerGroup = "app-push-consumer-group")
public class AppPushMessageConsumer implements RocketMQListener<String> {

    private final AppPushService appPushService;

    @Override
    @SuppressWarnings("unchecked")
    public void onMessage(String message) {
        log.info("收到APP推送消息: {}", message);
        try {
            JSONObject jsonObject = JSON.parseObject(message);
            String type = jsonObject.getString("type");

            if ("WORKORDER_CREATED".equals(type)) {
                handleWorkOrderCreatedPush(jsonObject);
            } else {
                log.warn("未知的推送类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理APP推送消息失败: {}", message, e);
        }
    }

    private void handleWorkOrderCreatedPush(JSONObject jsonObject) {
        String title = jsonObject.getString("title");
        String content = jsonObject.getString("content");
        String receiverPhone = jsonObject.getString("receiverPhone");

        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            log.warn("推送标题或内容为空，跳过推送");
            return;
        }

        Map<String, String> extras = new HashMap<>();
        Object extrasObj = jsonObject.get("extras");
        if (extrasObj instanceof Map) {
            ((Map<String, Object>) extrasObj).forEach((k, v) -> {
                if (v != null) {
                    extras.put(k, String.valueOf(v));
                }
            });
        }

        Long workOrderId = jsonObject.getLong("workOrderId");
        String workOrderNo = jsonObject.getString("workOrderNo");
        extras.put("workOrderId", workOrderId != null ? String.valueOf(workOrderId) : "");
        extras.put("workOrderNo", StringUtils.hasText(workOrderNo) ? workOrderNo : "");
        extras.put("pushType", "WORKORDER_CREATED");

        if (StringUtils.hasText(receiverPhone)) {
            boolean result = appPushService.pushToPhone(receiverPhone, title, content, extras);
            log.info("工单创建推送执行完成, phone: {}, workOrderNo: {}, result: {}",
                    receiverPhone, workOrderNo, result);
        } else {
            log.warn("接收手机号为空，无法推送, workOrderNo: {}", workOrderNo);
        }
    }
}
