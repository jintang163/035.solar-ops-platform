package com.solar.ops.drone.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.solar.ops.drone.dto.DroneImageQueryDTO;
import com.solar.ops.drone.entity.DroneInspectionImage;
import com.solar.ops.drone.vo.DroneImageVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DroneInspectionImageService extends IService<DroneInspectionImage> {

    IPage<DroneImageVO> page(DroneImageQueryDTO dto);

    DroneImageVO getDetail(Long id);

    String uploadImage(MultipartFile file, Long taskId, Long stationId);

    boolean batchUpload(List<MultipartFile> files, Long taskId, Long stationId);

    boolean triggerDetect(Long imageId);

    boolean triggerBatchDetect(Long taskId);

    boolean updateDetectStatus(Long id, Integer status, String result);

    List<DroneImageVO> getByTaskId(Long taskId);
}
