package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.entity.AlarmMessageLog;
import com.solar.ops.admin.mapper.AlarmMessageLogMapper;
import com.solar.ops.common.page.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class AlarmMessageLogService {

    private static final Logger log = LoggerFactory.getLogger(AlarmMessageLogService.class);

    public static final int STATUS_CONSUMING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;

    @Resource
    private AlarmMessageLogMapper alarmMessageLogMapper;

    public AlarmMessageLog beginConsume(String topic, String content) {
        AlarmMessageLog messageLog = new AlarmMessageLog();
        messageLog.setTopic(topic);
        messageLog.setMessageId(generateMessageId(content));
        messageLog.setContent(content);
        messageLog.setConsumeStatus(STATUS_CONSUMING);
        messageLog.setRetryCount(0);
        messageLog.setErrorMsg(null);
        try {
            alarmMessageLogMapper.insert(messageLog);
        } catch (Exception e) {
            log.error("记录消息消费开始日志失败, topic={}, error={}", topic, e.getMessage(), e);
        }
        return messageLog;
    }

    public void markSuccess(Long id) {
        if (id == null) {
            return;
        }
        try {
            AlarmMessageLog update = new AlarmMessageLog();
            update.setId(id);
            update.setConsumeStatus(STATUS_SUCCESS);
            alarmMessageLogMapper.updateById(update);
        } catch (Exception e) {
            log.error("标记消息消费成功失败, id={}, error={}", id, e.getMessage(), e);
        }
    }

    public void markFailed(Long id, String errorMsg) {
        if (id == null) {
            return;
        }
        try {
            AlarmMessageLog existing = alarmMessageLogMapper.selectById(id);
            AlarmMessageLog update = new AlarmMessageLog();
            update.setId(id);
            update.setConsumeStatus(STATUS_FAILED);
            update.setRetryCount(existing != null && existing.getRetryCount() != null
                    ? existing.getRetryCount() + 1 : 1);
            update.setErrorMsg(truncate(errorMsg, 1000));
            alarmMessageLogMapper.updateById(update);
        } catch (Exception e) {
            log.error("标记消息消费失败失败, id={}, error={}", id, e.getMessage(), e);
        }
    }

    public PageResult<AlarmMessageLog> getLogPage(Integer pageNum, Integer pageSize,
                                                   String topic, Integer consumeStatus,
                                                   String keyword) {
        Page<AlarmMessageLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AlarmMessageLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(topic)) {
            wrapper.eq(AlarmMessageLog::getTopic, topic);
        }
        if (consumeStatus != null) {
            wrapper.eq(AlarmMessageLog::getConsumeStatus, consumeStatus);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AlarmMessageLog::getContent, keyword)
                    .or().like(AlarmMessageLog::getErrorMsg, keyword)
                    .or().like(AlarmMessageLog::getMessageId, keyword);
        }
        wrapper.orderByDesc(AlarmMessageLog::getCreateTime);

        alarmMessageLogMapper.selectPage(page, wrapper);
        return PageResult.build(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize);
    }

    private String generateMessageId(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString() + "-" + System.currentTimeMillis();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}
