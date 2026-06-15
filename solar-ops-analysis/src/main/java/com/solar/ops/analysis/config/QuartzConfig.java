package com.solar.ops.analysis.config;

import com.solar.ops.analysis.job.CleaningImprovementCalcJob;
import com.solar.ops.analysis.job.DailyStatisticsJob;
import com.solar.ops.analysis.job.DustDetectionJob;
import com.solar.ops.analysis.job.EfficiencyCalculateJob;
import com.solar.ops.analysis.job.RevenueCalculateJob;
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

    @Bean
    public JobDetail dustDetectionJobDetail() {
        return JobBuilder.newJob(DustDetectionJob.class)
                .withIdentity("dustDetectionJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger dustDetectionTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 30 2 * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(dustDetectionJobDetail())
                .withIdentity("dustDetectionTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail cleaningImprovementCalcJobDetail() {
        return JobBuilder.newJob(CleaningImprovementCalcJob.class)
                .withIdentity("cleaningImprovementCalcJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger cleaningImprovementCalcTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 0 3 * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(cleaningImprovementCalcJobDetail())
                .withIdentity("cleaningImprovementCalcTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail revenueCalculateJobDetail() {
        return JobBuilder.newJob(RevenueCalculateJob.class)
                .withIdentity("revenueCalculateJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger revenueCalculateTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 30 1 * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(revenueCalculateJobDetail())
                .withIdentity("revenueCalculateTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }
}
