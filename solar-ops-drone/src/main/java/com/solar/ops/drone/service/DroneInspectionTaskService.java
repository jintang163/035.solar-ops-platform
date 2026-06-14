package com.solar.ops.drone.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.solar.ops.drone.dto.DroneTaskCreateDTO;
import com.solar.ops.drone.dto.DroneTaskQueryDTO;
import com.solar.ops.drone.entity.DroneInspectionTask;
import com.solar.ops.drone.vo.DroneStatisticsVO;
import com.solar.ops.drone.vo.DroneTaskVO;

public interface DroneInspectionTaskService extends IService<DroneInspectionTask> {

    IPage<DroneTaskVO> page(DroneTaskQueryDTO dto);

    DroneTaskVO getDetail(Long id);

    Long create(DroneTaskCreateDTO dto);

    boolean updateStatus(Long id, Integer status);

    boolean delete(Long id);

    DroneStatisticsVO getStatistics(Long stationId);
}
