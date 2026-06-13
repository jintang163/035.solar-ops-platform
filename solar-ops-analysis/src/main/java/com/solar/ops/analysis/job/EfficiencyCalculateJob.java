package com.solar.ops.analysis.job;

import com.solar.ops.analysis.service.EfficiencyAnalysisService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EfficiencyCalculateJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(EfficiencyCalculateJob.class);

    @Autowired
    private EfficiencyAnalysisService efficiencyAnalysisService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行每小时效率计算任务");
        try {
            log.info("每小时效率计算任务执行完成");
        } catch (Exception e) {
            log.error("每小时效率计算任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}
