package com.solar.ops.analysis.job;

import com.solar.ops.analysis.service.RevenueCalculateService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class RevenueCalculateJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(RevenueCalculateJob.class);

    @Autowired
    private RevenueCalculateService revenueCalculateService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行电费收益计算定时任务");
        try {
            LocalDate calculateDate = LocalDate.now().minusDays(1);
            log.info("收益计算日期: {}", calculateDate);

            revenueCalculateService.calculateAllStationsDailyRevenue(calculateDate);

            if (calculateDate.equals(calculateDate.withDayOfMonth(1).minusDays(1))
                    || calculateDate.getDayOfMonth() == 1) {
                LocalDate lastMonth = calculateDate.minusMonths(1);
                log.info("月初汇总：汇总{}年{}月的月度收益", lastMonth.getYear(), lastMonth.getMonthValue());
                revenueCalculateService.calculateAllStationsMonthlyRevenue(lastMonth.getYear(), lastMonth.getMonthValue());
            }

            if (calculateDate.getMonthValue() == 1 && calculateDate.getDayOfMonth() == 1) {
                int lastYear = calculateDate.getYear() - 1;
                log.info("年初汇总：汇总{}年的年度收益", lastYear);
                revenueCalculateService.calculateAllStationsYearlyRevenue(lastYear);
            }

            log.info("电费收益计算定时任务执行完成");
        } catch (Exception e) {
            log.error("电费收益计算定时任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}
