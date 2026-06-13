package com.solar.ops.analysis.config;

import com.solar.ops.analysis.job.DailyStatisticsJob;
import com.solar.ops.analysis.job.EfficiencyCalculateJob;
import com.solar.ops.analysis.job.WarrantyReminderJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail efficiencyCalculateJobDetail() {
        return JobBuilder.newJob(EfficiencyCalculateJob.class)
                .withIdentity("efficiencyCalculateJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger efficiencyCalculateTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 0 * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(efficiencyCalculateJobDetail())
                .withIdentity("efficiencyCalculateTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail dailyStatisticsJobDetail() {
        return JobBuilder.newJob(DailyStatisticsJob.class)
                .withIdentity("dailyStatisticsJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger dailyStatisticsTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 0 2 * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(dailyStatisticsJobDetail())
                .withIdentity("dailyStatisticsTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail warrantyReminderJobDetail() {
        return JobBuilder.newJob(WarrantyReminderJob.class)
                .withIdentity("warrantyReminderJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger warrantyReminderTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 0 9 * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(warrantyReminderJobDetail())
                .withIdentity("warrantyReminderTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }
}
