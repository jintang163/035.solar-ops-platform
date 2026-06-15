package com.solar.ops.analysis.job;

import com.solar.ops.admin.entity.SparePartInventory;
import com.solar.ops.admin.entity.SparePartPurchaseSuggestion;
import com.solar.ops.admin.entity.SysUser;
import com.solar.ops.admin.enums.SparePartWarnStatusEnum;
import com.solar.ops.admin.enums.UrgencyEnum;
import com.solar.ops.admin.mapper.SparePartInventoryMapper;
import com.solar.ops.admin.mapper.SparePartPurchaseSuggestionMapper;
import com.solar.ops.admin.mapper.SysUserMapper;
import com.solar.ops.analysis.service.AppPushService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SparePartStockCheckJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(SparePartStockCheckJob.class);

    @Autowired
    private SparePartInventoryMapper inventoryMapper;

    @Autowired
    private SparePartPurchaseSuggestionMapper purchaseSuggestionMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private AppPushService appPushService;

    @Value("${spring.mail.username:noreply@solar.com}")
    private String mailFrom;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行安全库存定时检查任务");
        try {
            List<SparePartInventory> inventories = inventoryMapper.selectList(
                    new LambdaQueryWrapper<SparePartInventory>()
                            .eq(SparePartInventory::getStatus, 1)
                            .eq(SparePartInventory::getDeleted, 0)
            );

            int updatedCount = 0;
            int lowWarnCount = 0;
            int insufficientCount = 0;

            for (SparePartInventory inventory : inventories) {
                Integer oldWarnStatus = inventory.getWarnStatus();
                Integer newWarnStatus = calculateWarnStatus(inventory);

                if (!newWarnStatus.equals(oldWarnStatus)) {
                    inventory.setWarnStatus(newWarnStatus);
                    inventoryMapper.updateById(inventory);
                    updatedCount++;

                    if (SparePartWarnStatusEnum.LOW_WARN.getCode().equals(newWarnStatus)) {
                        lowWarnCount++;
                    } else if (SparePartWarnStatusEnum.INSUFFICIENT.getCode().equals(newWarnStatus)) {
                        insufficientCount++;
                    }
                }
            }

            log.info("安全库存检查完成，更新了{}个备件的预警状态，低库存{}个，库存不足{}个",
                    updatedCount, lowWarnCount, insufficientCount);

            generatePurchaseSuggestions();

            if (lowWarnCount > 0 || insufficientCount > 0) {
                sendLowStockAlert(lowWarnCount, insufficientCount);
            }

        } catch (Exception e) {
            log.error("安全库存定时检查任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }

    private Integer calculateWarnStatus(SparePartInventory inventory) {
        int quantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int safeQuantity = inventory.getSafeQuantity() != null ? inventory.getSafeQuantity() : 10;

        if (quantity <= 0) {
            return SparePartWarnStatusEnum.INSUFFICIENT.getCode();
        } else if (quantity < safeQuantity) {
            if (quantity < safeQuantity / 2) {
                return SparePartWarnStatusEnum.INSUFFICIENT.getCode();
            } else {
                return SparePartWarnStatusEnum.LOW_WARN.getCode();
            }
        } else {
            return SparePartWarnStatusEnum.NORMAL.getCode();
        }
    }

    private void generatePurchaseSuggestions() {
        List<SparePartInventory> lowStockParts = inventoryMapper.selectList(
                new LambdaQueryWrapper<SparePartInventory>()
                        .eq(SparePartInventory::getStatus, 1)
                        .eq(SparePartInventory::getDeleted, 0)
                        .in(SparePartInventory::getWarnStatus,
                                SparePartWarnStatusEnum.LOW_WARN.getCode(),
                                SparePartWarnStatusEnum.INSUFFICIENT.getCode())
        );

        int generatedCount = 0;
        for (SparePartInventory inventory : lowStockParts) {
            LambdaQueryWrapper<SparePartPurchaseSuggestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SparePartPurchaseSuggestion::getPartId, inventory.getId());
            wrapper.eq(SparePartPurchaseSuggestion::getStatus, 0);
            wrapper.eq(SparePartPurchaseSuggestion::getDeleted, 0);
            SparePartPurchaseSuggestion existSuggestion = purchaseSuggestionMapper.selectOne(wrapper);

            if (existSuggestion != null) {
                continue;
            }

            int safeQuantity = inventory.getSafeQuantity() != null ? inventory.getSafeQuantity() : 10;
            int currentQuantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            int minPurchaseQuantity = inventory.getMinPurchaseQuantity() != null ? inventory.getMinPurchaseQuantity() : 10;

            int suggestQuantity = safeQuantity * 2 - currentQuantity;
            if (suggestQuantity < minPurchaseQuantity) {
                suggestQuantity = minPurchaseQuantity;
            }

            int urgency = UrgencyEnum.NORMAL.getCode();
            if (SparePartWarnStatusEnum.INSUFFICIENT.getCode().equals(inventory.getWarnStatus())) {
                if (currentQuantity <= 0) {
                    urgency = UrgencyEnum.VERY_URGENT.getCode();
                } else {
                    urgency = UrgencyEnum.URGENT.getCode();
                }
            }

            SparePartPurchaseSuggestion suggestion = new SparePartPurchaseSuggestion();
            suggestion.setSuggestionNo(generateSuggestionNo());
            suggestion.setPartId(inventory.getId());
            suggestion.setPartCode(inventory.getPartCode());
            suggestion.setPartName(inventory.getPartName());
            suggestion.setPartModel(inventory.getPartModel());
            suggestion.setPartType(inventory.getPartType());
            suggestion.setCurrentQuantity(currentQuantity);
            suggestion.setSafeQuantity(safeQuantity);
            suggestion.setSuggestQuantity(suggestQuantity);
            suggestion.setMinPurchaseQuantity(minPurchaseQuantity);
            suggestion.setUnitPrice(inventory.getUnitPrice());
            if (inventory.getUnitPrice() != null) {
                suggestion.setEstimatedAmount(inventory.getUnitPrice().multiply(BigDecimal.valueOf(suggestQuantity)));
            }
            suggestion.setSupplier(inventory.getSupplier());
            suggestion.setUrgency(urgency);
            suggestion.setStatus(0);
            suggestion.setGenerateTime(LocalDateTime.now());
            suggestion.setRemark("系统自动生成的采购建议");
            purchaseSuggestionMapper.insert(suggestion);

            generatedCount++;
        }

        log.info("生成了{}条采购建议", generatedCount);
    }

    private void sendLowStockAlert(int lowWarnCount, int insufficientCount) {
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .eq(SysUser::getDeleted, 0)
                .in(SysUser::getRole, "admin", "ops"));

        if (users.isEmpty()) {
            return;
        }

        boolean mailSent = sendLowStockEmail(lowWarnCount, insufficientCount, users);
        boolean appPushSent = sendLowStockAppPush(lowWarnCount, insufficientCount, users);

        log.info("低库存预警通知发送完成，邮件：{}，APP推送：{}", mailSent, appPushSent);
    }

    private boolean sendLowStockEmail(int lowWarnCount, int insufficientCount, List<SysUser> users) {
        if (mailSender == null) {
            log.warn("邮件发送器未配置，跳过邮件发送");
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(users.stream()
                    .map(SysUser::getEmail)
                    .filter(email -> email != null && !email.isEmpty())
                    .toArray(String[]::new));
            message.setSubject("【库存预警】备件低库存预警通知");
            message.setText(String.format(
                    "尊敬的用户：\n\n" +
                            "系统检测到备件库存预警，请及时处理。\n\n" +
                            "低库存预警备件数量：%d个\n" +
                            "库存不足备件数量：%d个\n\n" +
                            "请登录管理后台查看详细信息，并及时安排采购。\n\n" +
                            "此邮件由系统自动发送，请勿直接回复。\n\n" +
                            "光伏电站运维平台",
                    lowWarnCount, insufficientCount
            ));

            mailSender.send(message);
            log.info("低库存预警邮件已发送");
            return true;
        } catch (Exception e) {
            log.error("发送低库存预警邮件失败", e);
            return false;
        }
    }

    private boolean sendLowStockAppPush(int lowWarnCount, int insufficientCount, List<SysUser> users) {
        if (appPushService == null) {
            log.warn("APP推送服务未配置，跳过APP推送");
            return false;
        }

        try {
            String title = "【库存预警】备件库存预警";
            String content = String.format("低库存%d个，库存不足%d个，请及时处理", lowWarnCount, insufficientCount);

            Map<String, String> extras = new HashMap<>();
            extras.put("type", "spare_part_stock_warn");
            extras.put("lowWarnCount", String.valueOf(lowWarnCount));
            extras.put("insufficientCount", String.valueOf(insufficientCount));

            boolean pushSuccess = appPushService.pushToUsers(users, title, content, extras);
            if (pushSuccess) {
                log.info("低库存预警APP推送已发送");
            }
            return pushSuccess;
        } catch (Exception e) {
            log.error("发送低库存预警APP推送失败", e);
            return false;
        }
    }

    private String generateSuggestionNo() {
        String prefix = "CG";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String codePrefix = prefix + dateStr;

        LambdaQueryWrapper<SparePartPurchaseSuggestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SparePartPurchaseSuggestion::getSuggestionNo, codePrefix);
        wrapper.orderByDesc(SparePartPurchaseSuggestion::getSuggestionNo);
        wrapper.last("LIMIT 1");
        SparePartPurchaseSuggestion lastSuggestion = purchaseSuggestionMapper.selectOne(wrapper);

        int sequence = 1;
        if (lastSuggestion != null && lastSuggestion.getSuggestionNo() != null) {
            String lastNo = lastSuggestion.getSuggestionNo();
            String seqStr = lastNo.substring(codePrefix.length());
            try {
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }

        return codePrefix + String.format("%04d", sequence);
    }
}
