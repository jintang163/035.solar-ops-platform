package com.solar.ops.analysis.job;

import com.solar.ops.analysis.service.CleaningPlanService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CleaningImprovementCalcJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(CleaningImprovementCalcJob.class);

    @Autowired
    private CleaningPlanService cleaningPlanService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行清洗提升发电量计算定时任务");
        try {
            int calcCount = cleaningPlanService.calculatePendingImprovements();
            log.info("清洗提升发电量计算完成，成功计算{}个计划", calcCount);
        } catch (Exception e) {
            log.error("清洗提升发电量计算定时任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}
