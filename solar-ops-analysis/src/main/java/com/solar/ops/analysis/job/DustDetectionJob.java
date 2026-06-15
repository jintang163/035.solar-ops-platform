package com.solar.ops.analysis.job;

import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.analysis.entity.DustAccumulationRecord;
import com.solar.ops.analysis.service.CleaningReminderService;
import com.solar.ops.analysis.service.DustAccumulationService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;

@Component
public class DustDetectionJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(DustDetectionJob.class);

    @Autowired
    private DustAccumulationService dustAccumulationService;

    @Autowired
    private CleaningReminderService cleaningReminderService;

    @Autowired
    private StationMapper stationMapper;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行积灰检测与清洗建议生成定时任务");
        try {
            LocalDate detectDate = LocalDate.now().minusDays(1);
            log.info("积灰检测日期: {}", detectDate);

            List<Station> stations = stationMapper.selectList(null);
            if (CollectionUtils.isEmpty(stations)) {
                log.warn("未查询到电站信息，跳过积灰检测任务");
                return;
            }

            int totalDustRecords = 0;
            int totalReminders = 0;

            for (Station station : stations) {
                try {
                    log.info("开始检测电站[{}]积灰情况", station.getName());
                    List<DustAccumulationRecord> dustRecords =
                            dustAccumulationService.detectDustAccumulation(station.getId(), detectDate);
                    totalDustRecords += dustRecords.size();
                    log.info("电站[{}]积灰检测完成，生成记录{}条", station.getName(), dustRecords.size());
                } catch (Exception e) {
                    log.error("电站[{}]积灰检测失败", station.getName(), e);
                }
            }

            int reminderCount = cleaningReminderService.generateCleaningReminders(detectDate).size();
            totalReminders += reminderCount;
            log.info("清洗建议生成完成，生成建议{}条", reminderCount);

            log.info("积灰检测定时任务执行完成，累计生成积灰记录{}条，清洗建议{}条",
                    totalDustRecords, totalReminders);

        } catch (Exception e) {
            log.error("积灰检测定时任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}
