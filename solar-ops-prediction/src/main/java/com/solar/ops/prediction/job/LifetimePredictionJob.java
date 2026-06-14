package com.solar.ops.prediction.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.prediction.service.LifetimePredictionService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class LifetimePredictionJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(LifetimePredictionJob.class);

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private InverterMapper inverterMapper;

    @Autowired
    private LifetimePredictionService lifetimePredictionService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行每日健康度计算和寿命预测定时任务");

        try {
            LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Station::getStatus, 1);
            List<Station> stations = stationMapper.selectList(wrapper);

            if (stations == null || stations.isEmpty()) {
                log.info("没有可用的电站，跳过任务");
                return;
            }

            for (Station station : stations) {
                try {
                    log.info("开始处理电站: {} (ID: {})", station.getStationName(), station.getId());

                    LambdaQueryWrapper<Inverter> invWrapper = new LambdaQueryWrapper<>();
                    invWrapper.eq(Inverter::getStationId, station.getId())
                            .eq(Inverter::getStatus, 1);
                    List<Inverter> inverters = inverterMapper.selectList(invWrapper);

                    for (Inverter inverter : inverters) {
                        try {
                            lifetimePredictionService.calculateDailyHealth(
                                    station.getId(), inverter.getId(), LocalDate.now());

                            lifetimePredictionService.predictLifetime(
                                    station.getId(), inverter.getId(), 90);
                        } catch (Exception e) {
                            log.error("逆变器寿命预测失败: inverterId={}", inverter.getId(), e);
                        }
                    }

                } catch (Exception e) {
                    log.error("电站寿命预测失败: stationId={}", station.getId(), e);
                }
            }

            log.info("每日健康度计算和寿命预测任务执行完成");
        } catch (Exception e) {
            log.error("每日健康度计算和寿命预测任务异常", e);
            throw new JobExecutionException(e);
        }
    }
}
