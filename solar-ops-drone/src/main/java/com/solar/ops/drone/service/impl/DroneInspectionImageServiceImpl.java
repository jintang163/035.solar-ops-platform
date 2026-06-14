package com.solar.ops.drone.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.drone.client.DefectDetectClient;
import com.solar.ops.drone.config.DroneProperties;
import com.solar.ops.drone.dto.DroneImageQueryDTO;
import com.solar.ops.drone.entity.DroneDefect;
import com.solar.ops.drone.entity.DroneInspectionImage;
import com.solar.ops.drone.entity.DroneInspectionTask;
import com.solar.ops.drone.enums.DetectStatusEnum;
import com.solar.ops.drone.enums.TaskStatusEnum;
import com.solar.ops.drone.mapper.DroneDefectMapper;
import com.solar.ops.drone.mapper.DroneInspectionImageMapper;
import com.solar.ops.drone.mapper.DroneInspectionTaskMapper;
import com.solar.ops.drone.service.DroneInspectionImageService;
import com.solar.ops.drone.vo.DroneImageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DroneInspectionImageServiceImpl extends ServiceImpl<DroneInspectionImageMapper, DroneInspectionImage> implements DroneInspectionImageService {

    @Autowired
    private DroneProperties droneProperties;

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private DroneInspectionTaskMapper taskMapper;

    @Autowired
    private DroneDefectMapper defectMapper;

    @Autowired
    private DefectDetectClient defectDetectClient;

    @Override
    public IPage<DroneImageVO> page(DroneImageQueryDTO dto) {
        Page<DroneInspectionImage> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<DroneInspectionImage> wrapper = new LambdaQueryWrapper<>();

        if (dto.getTaskId() != null) {
            wrapper.eq(DroneInspectionImage::getTaskId, dto.getTaskId());
        }
        if (dto.getStationId() != null) {
            wrapper.eq(DroneInspectionImage::getStationId, dto.getStationId());
        }
        if (dto.getDetectStatus() != null) {
            wrapper.eq(DroneInspectionImage::getDetectStatus, dto.getDetectStatus());
        }

        wrapper.orderByDesc(DroneInspectionImage::getCreateTime);
        Page<DroneInspectionImage> imagePage = this.page(page, wrapper);

        return imagePage.convert(image -> {
            DroneImageVO vo = new DroneImageVO();
            BeanUtils.copyProperties(image, vo);

            Station station = stationMapper.selectById(image.getStationId());
            if (station != null) {
                vo.setStationName(station.getStationName());
            }

            LambdaQueryWrapper<DroneDefect> defectWrapper = new LambdaQueryWrapper<>();
            defectWrapper.eq(DroneDefect::getImageId, image.getId());
            List<DroneDefect> defects = defectMapper.selectList(defectWrapper);
            vo.setDefectCount(defects.size());

            return vo;
        });
    }

    @Override
    public DroneImageVO getDetail(Long id) {
        DroneInspectionImage image = this.getById(id);
        if (image == null) {
            throw new BusinessException("图片不存在");
        }

        DroneImageVO vo = new DroneImageVO();
        BeanUtils.copyProperties(image, vo);

        Station station = stationMapper.selectById(image.getStationId());
        if (station != null) {
            vo.setStationName(station.getStationName());
        }

        DroneInspectionTask task = taskMapper.selectById(image.getTaskId());
        if (task != null) {
            vo.setTaskName(task.getTaskName());
        }

        LambdaQueryWrapper<DroneDefect> defectWrapper = new LambdaQueryWrapper<>();
        defectWrapper.eq(DroneDefect::getImageId, id);
        List<DroneDefect> defects = defectMapper.selectList(defectWrapper);
        vo.setDefectCount(defects.size());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadImage(MultipartFile file, Long taskId, Long stationId) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        DroneProperties.ImageStorage storage = droneProperties.getImageStorage();
        String contentType = file.getContentType();
        String[] allowedTypes = storage.getAllowedTypes();
        boolean typeAllowed = false;
        for (String type : allowedTypes) {
            if (type.equals(contentType)) {
                typeAllowed = true;
                break;
            }
        }
        if (!typeAllowed) {
            throw new BusinessException("不支持的文件类型: " + contentType);
        }

        if (file.getSize() > storage.getMaxSize()) {
            throw new BusinessException("文件大小超过限制");
        }

        try {
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path uploadPath = Paths.get(storage.getPath(), dateDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
            Path filePath = uploadPath.resolve(newFilename);
            file.transferTo(filePath.toFile());

            String relativePath = dateDir + "/" + newFilename;
            String imageUrl = storage.getBaseUrl() + "/" + relativePath;

            DroneInspectionImage image = new DroneInspectionImage();
            image.setTaskId(taskId);
            image.setStationId(stationId);
            image.setImageName(originalFilename);
            image.setImageUrl(imageUrl);
            image.setImagePath(filePath.toString());
            image.setImageType(contentType);
            image.setImageSize(file.getSize());
            image.setDetectStatus(DetectStatusEnum.PENDING.getCode());
            this.save(image);

            if (taskId != null) {
                DroneInspectionTask task = taskMapper.selectById(taskId);
                if (task != null && task.getStatus() == TaskStatusEnum.PENDING.getCode()) {
                    task.setStatus(TaskStatusEnum.PROCESSING.getCode());
                    taskMapper.updateById(task);
                }
            }

            return imageUrl;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpload(List<MultipartFile> files, Long taskId, Long stationId) {
        for (MultipartFile file : files) {
            uploadImage(file, taskId, stationId);
        }
        return true;
    }

    @Override
    public boolean triggerDetect(Long imageId) {
        DroneInspectionImage image = this.getById(imageId);
        if (image == null) {
            throw new BusinessException("图片不存在");
        }
        if (image.getDetectStatus() == DetectStatusEnum.DETECTING.getCode()) {
            throw new BusinessException("检测中，请稍后");
        }

        image.setDetectStatus(DetectStatusEnum.DETECTING.getCode());
        image.setDetectStartTime(LocalDateTime.now());
        this.updateById(image);

        asyncDetect(imageId);
        return true;
    }

    @Async
    @Transactional(rollbackFor = Exception.class)
    public void asyncDetect(Long imageId) {
        DroneInspectionImage image = this.getById(imageId);
        if (image == null) return;

        try {
            File imageFile = new File(image.getImagePath());
            if (!imageFile.exists()) {
                throw new BusinessException("图片文件不存在");
            }

            byte[] imageData = Files.readAllBytes(imageFile.toPath());
            DefectDetectClient.DetectResult detectResult = defectDetectClient.detectAndSave(
                    imageData,
                    image.getImageName(),
                    image.getImageType(),
                    image.getTaskId(),
                    imageId,
                    image.getStationId()
            );

            List<DroneDefect> defects = detectResult.getDefects();

            LambdaQueryWrapper<DroneDefect> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DroneDefect::getImageId, imageId);
            defectMapper.delete(wrapper);

            for (DroneDefect defect : defects) {
                defectMapper.insert(defect);
            }

            if (detectResult.getAnnotatedImagePath() != null) {
                image.setAnnotatedPath(detectResult.getAnnotatedImagePath());
            }
            if (detectResult.getAnnotatedImageUrl() != null) {
                image.setAnnotatedImageUrl(detectResult.getAnnotatedImageUrl());
            }
            if (detectResult.getImageWidth() != null) {
                image.setImageWidth(detectResult.getImageWidth());
            }
            if (detectResult.getImageHeight() != null) {
                image.setImageHeight(detectResult.getImageHeight());
            }

            image.setDetectStatus(DetectStatusEnum.COMPLETED.getCode());
            image.setDetectEndTime(LocalDateTime.now());
            image.setDetectTime(LocalDateTime.now());
            image.setDefectCount(defects.size());
            image.setDetectResult(defects.size() > 0 ? "检测到" + defects.size() + "处缺陷" : "未检测到缺陷");
            this.updateById(image);

            if (image.getTaskId() != null) {
                updateTaskDetectionStats(image.getTaskId());
            }

            if (defects.size() > 0) {
                pushDefectAlert(image, defects);
            }

        } catch (Exception e) {
            log.error("缺陷检测失败, imageId: {}", imageId, e);
            image.setDetectStatus(DetectStatusEnum.FAILED.getCode());
            image.setDetectResult("检测失败: " + e.getMessage());
            this.updateById(image);
        }
    }

    private void updateTaskDetectionStats(Long taskId) {
        try {
            DroneInspectionTask task = taskMapper.selectById(taskId);
            if (task == null) return;

            LambdaQueryWrapper<DroneInspectionImage> allImagesWrapper = new LambdaQueryWrapper<>();
            allImagesWrapper.eq(DroneInspectionImage::getTaskId, taskId);
            Long totalImageCount = this.count(allImagesWrapper);

            LambdaQueryWrapper<DroneInspectionImage> completedImagesWrapper = new LambdaQueryWrapper<>();
            completedImagesWrapper.eq(DroneInspectionImage::getTaskId, taskId);
            completedImagesWrapper.eq(DroneInspectionImage::getDetectStatus, DetectStatusEnum.COMPLETED.getCode());
            Long detectedImageCount = this.count(completedImagesWrapper);

            LambdaQueryWrapper<DroneDefect> allDefectsWrapper = new LambdaQueryWrapper<>();
            allDefectsWrapper.eq(DroneDefect::getTaskId, taskId);
            Long totalDefectCount = defectMapper.selectCount(allDefectsWrapper);

            LambdaQueryWrapper<DroneDefect> confirmedDefectsWrapper = new LambdaQueryWrapper<>();
            confirmedDefectsWrapper.eq(DroneDefect::getTaskId, taskId);
            confirmedDefectsWrapper.eq(DroneDefect::getConfirmed, 1);
            Long confirmedDefectCount = defectMapper.selectCount(confirmedDefectsWrapper);

            task.setImageCount(totalImageCount.intValue());
            task.setDetectedImageCount(detectedImageCount.intValue());
            task.setDefectCount(totalDefectCount.intValue());
            task.setConfirmedDefectCount(confirmedDefectCount.intValue());

            LambdaQueryWrapper<DroneInspectionImage> pendingWrapper = new LambdaQueryWrapper<>();
            pendingWrapper.eq(DroneInspectionImage::getTaskId, taskId);
            pendingWrapper.ne(DroneInspectionImage::getDetectStatus, DetectStatusEnum.COMPLETED.getCode());
            Long pendingCount = this.count(pendingWrapper);

            if (pendingCount == 0 && totalImageCount > 0) {
                task.setStatus(TaskStatusEnum.COMPLETED.getCode());
            } else if (detectedImageCount > 0) {
                task.setStatus(TaskStatusEnum.PROCESSING.getCode());
            }

            taskMapper.updateById(task);
        } catch (Exception e) {
            log.error("更新任务检测统计失败, taskId: {}", taskId, e);
        }
    }

    private void pushDefectAlert(DroneInspectionImage image, List<DroneDefect> defects) {
        try {
            int seriousCount = 0;
            for (DroneDefect defect : defects) {
                if (defect.getDefectLevel() != null && defect.getDefectLevel() >= 3) {
                    seriousCount++;
                }
            }

            if (seriousCount == 0) {
                return;
            }

            log.info("检测到严重缺陷, imageId: {}, 严重缺陷数: {}", image.getId(), seriousCount);

        } catch (Exception e) {
            log.warn("推送缺陷告警失败", e);
        }
    }

    @Override
    public boolean triggerBatchDetect(Long taskId) {
        LambdaQueryWrapper<DroneInspectionImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DroneInspectionImage::getTaskId, taskId);
        wrapper.ne(DroneInspectionImage::getDetectStatus, DetectStatusEnum.DETECTING.getCode());
        List<DroneInspectionImage> images = this.list(wrapper);

        for (DroneInspectionImage image : images) {
            triggerDetect(image.getId());
        }

        return true;
    }

    @Override
    public boolean updateDetectStatus(Long id, Integer status, String result) {
        DroneInspectionImage image = this.getById(id);
        if (image == null) {
            throw new BusinessException("图片不存在");
        }
        image.setDetectStatus(status);
        if (result != null) {
            image.setDetectResult(result);
        }
        return this.updateById(image);
    }

    @Override
    public List<DroneImageVO> getByTaskId(Long taskId) {
        LambdaQueryWrapper<DroneInspectionImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DroneInspectionImage::getTaskId, taskId);
        wrapper.orderByDesc(DroneInspectionImage::getCreateTime);
        List<DroneInspectionImage> images = this.list(wrapper);

        return images.stream().map(image -> {
            DroneImageVO vo = new DroneImageVO();
            BeanUtils.copyProperties(image, vo);

            LambdaQueryWrapper<DroneDefect> defectWrapper = new LambdaQueryWrapper<>();
            defectWrapper.eq(DroneDefect::getImageId, image.getId());
            List<DroneDefect> defects = defectMapper.selectList(defectWrapper);
            vo.setDefectCount(defects.size());

            return vo;
        }).collect(java.util.stream.Collectors.toList());
    }
}
