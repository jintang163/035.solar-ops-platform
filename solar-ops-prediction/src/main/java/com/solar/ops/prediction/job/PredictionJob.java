package com.solar.ops.prediction.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.prediction.config.PredictionProperties;
import com.solar.ops.prediction.entity.PowerPrediction;
import com.solar.ops.prediction.entity.PredictionAlert;
import com.solar.ops.prediction.enums.AlertLevelEnum;
import com.solar.ops.prediction.enums.AlertStatusEnum;
import com.solar.ops.prediction.enums.AlertTypeEnum;
import com.solar.ops.prediction.mapper.PowerPredictionMapper;
import com.solar.ops.prediction.mapper.PredictionAlertMapper;
import com.solar.ops.prediction.service.PowerPredictionService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PredictionJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(PredictionJob.class);

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private InverterMapper inverterMapper;

    @Autowired
    private PowerPredictionService predictionService;

    @Autowired
    private PowerPredictionMapper predictionMapper;

    @Autowired
    private PredictionAlertMapper alertMapper;

    @Autowired
    private PredictionProperties predictionProperties;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行功率预测定时任务");

        try {
            LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Station::getStatus, 1);
            List<Station> stations = stationMapper.selectList(wrapper);

            for (Station station : stations) {
                try {
                    log.info("开始预测电站: {} (ID: {})", station.getStationName(), station.getId());

                    predictionService.executePrediction(station.getId(), null,
                            predictionProperties.getHoursAhead());

                    LambdaQueryWrapper<Inverter> invWrapper = new LambdaQueryWrapper<>();
                    invWrapper.eq(Inverter::getStationId, station.getId())
                            .eq(Inverter::getStatus, 1);
                    List<Inverter> inverters = inverterMapper.selectList(invWrapper);

                    for (Inverter inverter : inverters) {
                        try {
                            predictionService.executePrediction(station.getId(),
                                    inverter.getId(), predictionProperties.getHoursAhead());
                        } catch (Exception e) {
                            log.error("逆变器预测失败: inverterId={}", inverter.getId(), e);
                        }
                    }

                } catch (Exception e) {
                    log.error("电站预测失败: stationId={}", station.getId(), e);
                }
            }

            log.info("功率预测定时任务执行完成");
        } catch (Exception e) {
            log.error("功率预测定时任务异常", e);
            throw new JobExecutionException(e);
        }
    }
}
