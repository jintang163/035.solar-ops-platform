package com.solar.ops.analysis.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.admin.entity.Asset;
import com.solar.ops.admin.entity.SysUser;
import com.solar.ops.admin.entity.WarrantyReminder;
import com.solar.ops.admin.enums.AssetStatusEnum;
import com.solar.ops.admin.mapper.AssetMapper;
import com.solar.ops.admin.mapper.SysUserMapper;
import com.solar.ops.admin.mapper.WarrantyReminderMapper;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WarrantyReminderJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(WarrantyReminderJob.class);

    @Autowired
    private AssetMapper assetMapper;

    @Autowired
    private WarrantyReminderMapper warrantyReminderMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@solar.com}")
    private String mailFrom;

    @Value("${warranty.reminder.days:30}")
    private Integer reminderDays;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行质保到期提醒任务");
        try {
            LocalDate today = LocalDate.now();
            LocalDate reminderDate = today.plusDays(reminderDays);

            LambdaQueryWrapper<Asset> assetWrapper = new LambdaQueryWrapper<>();
            assetWrapper.le(Asset::getWarrantyEndDate, reminderDate);
            assetWrapper.ge(Asset::getWarrantyEndDate, today);
            assetWrapper.eq(Asset::getAssetStatus, AssetStatusEnum.NORMAL.getCode());
            assetWrapper.eq(Asset::getDeleted, 0);

            List<Asset> assets = assetMapper.selectList(assetWrapper);
            log.info("查询到{}个即将到期的资产", assets.size());

            for (Asset asset : assets) {
                long daysLeft = ChronoUnit.DAYS.between(today, asset.getWarrantyEndDate());

                LambdaQueryWrapper<WarrantyReminder> reminderWrapper = new LambdaQueryWrapper<>();
                reminderWrapper.eq(WarrantyReminder::getAssetId, asset.getId());
                reminderWrapper.eq(WarrantyReminder::getDaysLeft, daysLeft);
                reminderWrapper.eq(WarrantyReminder::getDeleted, 0);
                WarrantyReminder existReminder = warrantyReminderMapper.selectOne(reminderWrapper);

                if (existReminder == null) {
                    WarrantyReminder reminder = new WarrantyReminder();
                    reminder.setAssetId(asset.getId());
                    reminder.setAssetCode(asset.getAssetCode());
                    reminder.setAssetName(asset.getAssetName());
                    reminder.setWarrantyEndDate(asset.getWarrantyEndDate());
                    reminder.setDaysLeft((int) daysLeft);
                    reminder.setReminderType(3);

                    List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getStatus, 1)
                            .eq(SysUser::getDeleted, 0)
                            .in(SysUser::getRole, "admin", "ops"));

                    String receivers = users.stream()
                            .map(SysUser::getEmail)
                            .filter(email -> email != null && !email.isEmpty())
                            .collect(Collectors.joining(","));
                    reminder.setReceivers(receivers);

                    boolean mailSent = sendEmail(reminder, users);
                    if (mailSent) {
                        reminder.setReminderStatus(1);
                        reminder.setReminderTime(LocalDateTime.now());
                    } else {
                        reminder.setReminderStatus(0);
                    }

                    warrantyReminderMapper.insert(reminder);
                    log.info("已创建质保提醒: 资产={}, 剩余{}天", asset.getAssetName(), daysLeft);
                }
            }

            log.info("质保到期提醒任务执行完成");
        } catch (Exception e) {
            log.error("质保到期提醒任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }

    private boolean sendEmail(WarrantyReminder reminder, List<SysUser> users) {
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
            message.setSubject("【质保提醒】" + reminder.getAssetName() + " 质保即将到期");
            message.setText(String.format(
                    "尊敬的用户：\n\n" +
                    "您的设备 [%s] (资产编号: %s) 质保期即将到期，请及时处理。\n\n" +
                    "质保到期日期: %s\n" +
                    "剩余天数: %d天\n\n" +
                    "请尽快联系供应商进行续保或更换设备。\n\n" +
                    "此邮件由系统自动发送，请勿直接回复。\n\n" +
                    "光伏电站运维平台",
                    reminder.getAssetName(),
                    reminder.getAssetCode(),
                    reminder.getWarrantyEndDate(),
                    reminder.getDaysLeft()
            ));

            mailSender.send(message);
            log.info("质保提醒邮件已发送: {}", reminder.getAssetName());
            return true;
        } catch (Exception e) {
            log.error("发送质保提醒邮件失败", e);
            return false;
        }
    }
}
