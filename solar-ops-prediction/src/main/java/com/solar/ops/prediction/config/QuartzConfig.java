package com.solar.ops.prediction.config;

import com.solar.ops.prediction.job.DeviationCheckJob;
import com.solar.ops.prediction.job.PredictionJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail predictionJobDetail() {
        return JobBuilder.newJob(PredictionJob.class)
                .withIdentity("predictionJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger predictionJobTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 0 * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(predictionJobDetail())
                .withIdentity("predictionJobTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail deviationCheckJobDetail() {
        return JobBuilder.newJob(DeviationCheckJob.class)
                .withIdentity("deviationCheckJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger deviationCheckJobTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 */15 * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(deviationCheckJobDetail())
                .withIdentity("deviationCheckJobTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }
}
