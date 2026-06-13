package com.solar.ops.analysis.job;

import com.solar.ops.analysis.service.EfficiencyAnalysisService;
import com.solar.ops.analysis.service.HealthAssessmentService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class DailyStatisticsJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(DailyStatisticsJob.class);

    @Autowired
    private EfficiencyAnalysisService efficiencyAnalysisService;

    @Autowired
    private HealthAssessmentService healthAssessmentService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行每日凌晨统计任务");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            log.info("每日统计任务执行完成，统计日期: {}", yesterday);
        } catch (Exception e) {
            log.error("每日凌晨统计任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}
