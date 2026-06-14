package com.solar.ops.drone.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.solar.ops.drone.dto.DroneDefectHandleDTO;
import com.solar.ops.drone.dto.DroneDefectQueryDTO;
import com.solar.ops.drone.entity.DroneDefect;
import com.solar.ops.drone.vo.DroneDefectVO;

import java.util.List;

public interface DroneDefectService extends IService<DroneDefect> {

    IPage<DroneDefectVO> page(DroneDefectQueryDTO dto);

    DroneDefectVO getDetail(Long id);

    boolean confirmDefect(DroneDefectHandleDTO dto);

    Long generateWorkOrder(DroneDefectHandleDTO dto);

    List<DroneDefectVO> getByImageId(Long imageId);

    List<DroneDefectVO> getByTaskId(Long taskId);

    int countUnconfirmed(Long stationId);

    int countSerious(Long stationId);
}
