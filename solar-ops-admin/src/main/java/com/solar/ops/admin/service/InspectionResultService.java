package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solar.ops.admin.dto.InspectionAudioDTO;
import com.solar.ops.admin.dto.InspectionPhotoDTO;
import com.solar.ops.admin.dto.InspectionResultItemDTO;
import com.solar.ops.admin.dto.InspectionResultSubmitDTO;
import com.solar.ops.admin.entity.*;
import com.solar.ops.admin.holder.LoginUserHolder;
import com.solar.ops.admin.mapper.*;
import com.solar.ops.admin.vo.InspectionResultDetailVO;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class InspectionResultService extends ServiceImpl<InspectionResultMapper, InspectionResult> {

    @Resource
    private InspectionResultItemMapper resultItemMapper;

    @Resource
    private InspectionPhotoMapper photoMapper;

    @Resource
    private InspectionAudioMapper audioMapper;

    @Resource
    private InspectionTaskMapper taskMapper;

    @Resource
    private InspectionItemMapper itemMapper;

    @Resource
    private InspectionReportMapper reportMapper;

    @Resource
    private InspectionTaskService taskService;

    @Resource
    private LoginUserHolder loginUserHolder;

    @Value("${file.upload.path:/data/upload}")
    private String uploadPath;

    @Value("${file.access.url:/files}")
    private String accessUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PageResult<InspectionResult> page(PageQuery pageQuery, Long stationId, Integer resultStatus) {
        Long currentUserId = loginUserHolder.getCurrentUserId();
        Page<InspectionResult> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        baseMapper.selectResultPage(page, stationId, currentUserId, resultStatus);
        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public InspectionResultDetailVO getResultDetail(Long resultId) {
        if (resultId == null) {
            throw new BusinessException("结果ID不能为空");
        }

        InspectionResult result = getById(resultId);
        if (result == null) {
            throw new BusinessException("结果不存在");
        }

        InspectionResultDetailVO detailVO = new InspectionResultDetailVO();
        detailVO.setId(result.getId());
        detailVO.setResultNo(result.getResultNo());
        detailVO.setTaskId(result.getTaskId());
        detailVO.setTaskNo(result.getTaskNo());
        detailVO.setStationId(result.getStationId());
        detailVO.setStationName(result.getStationName());
        detailVO.setInspectorId(result.getInspectorId());
        detailVO.setInspectorName(result.getInspectorName());
        detailVO.setStartTime(result.getStartTime());
        detailVO.setEndTime(result.getEndTime());
        detailVO.setTotalItems(result.getTotalItems());
        detailVO.setNormalItems(result.getNormalItems());
        detailVO.setAbnormalItems(result.getAbnormalItems());
        detailVO.setResultStatus(result.getResultStatus());
        detailVO.setOverallRemark(result.getOverallRemark());

        detailVO.setItems(resultItemMapper.selectByResultId(resultId));
        detailVO.setPhotos(photoMapper.selectByResultId(resultId));
        detailVO.setAudios(audioMapper.selectByResultId(resultId));

        return detailVO;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long submitResult(InspectionResultSubmitDTO submitDTO) {
        if (submitDTO.getTaskId() == null) {
            throw new BusinessException("任务ID不能为空");
        }

        Long currentUserId = loginUserHolder.getCurrentUserId();
        String currentUserName = loginUserHolder.getCurrentUserName();

        InspectionTask task = taskMapper.selectById(submitDTO.getTaskId());
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        InspectionResult result = new InspectionResult();
        result.setResultNo(generateResultNo());
        result.setTaskId(submitDTO.getTaskId());
        result.setTaskNo(submitDTO.getTaskNo() != null ? submitDTO.getTaskNo() : task.getTaskNo());
        result.setStationId(submitDTO.getStationId() != null ? submitDTO.getStationId() : task.getStationId());
        result.setStationName(submitDTO.getStationName() != null ? submitDTO.getStationName() : task.getStationName());
        result.setInspectorId(currentUserId);
        result.setInspectorName(currentUserName);
        result.setStartTime(submitDTO.getStartTime());
        result.setEndTime(submitDTO.getEndTime() != null ? submitDTO.getEndTime() : LocalDateTime.now());
        result.setOverallRemark(submitDTO.getOverallRemark());
        result.setLongitude(submitDTO.getLongitude());
        result.setLatitude(submitDTO.getLatitude());
        result.setIsOffline(submitDTO.getIsOffline() != null ? submitDTO.getIsOffline() : 0);
        result.setUploadTime(LocalDateTime.now());
        result.setSyncStatus(1);

        List<InspectionResultItemDTO> itemDTOs = submitDTO.getItems();
        int totalItems = itemDTOs != null ? itemDTOs.size() : 0;
        int normalItems = 0;
        int abnormalItems = 0;

        if (itemDTOs != null && !itemDTOs.isEmpty()) {
            for (InspectionResultItemDTO itemDTO : itemDTOs) {
                if (itemDTO.getIsNormal() != null && itemDTO.getIsNormal() == 1) {
                    normalItems++;
                } else {
                    abnormalItems++;
                }
            }
        }

        result.setTotalItems(totalItems);
        result.setNormalItems(normalItems);
        result.setAbnormalItems(abnormalItems);
        result.setResultStatus(abnormalItems > 0 ? 2 : 1);

        save(result);

        if (itemDTOs != null && !itemDTOs.isEmpty()) {
            List<InspectionResultItem> resultItems = new ArrayList<>();
            for (InspectionResultItemDTO itemDTO : itemDTOs) {
                InspectionResultItem resultItem = new InspectionResultItem();
                resultItem.setResultId(result.getId());
                resultItem.setTaskItemId(itemDTO.getTaskItemId());
                resultItem.setItemId(itemDTO.getItemId());
                resultItem.setItemName(itemDTO.getItemName());
                resultItem.setItemType(itemDTO.getItemType());
                resultItem.setAssetId(itemDTO.getAssetId());
                resultItem.setAssetName(itemDTO.getAssetName());
                resultItem.setAssetCode(itemDTO.getAssetCode());
                resultItem.setCheckValue(itemDTO.getCheckValue());
                resultItem.setStandardValue(itemDTO.getStandardValue());
                resultItem.setIsNormal(itemDTO.getIsNormal() != null ? itemDTO.getIsNormal() : 1);
                resultItem.setAbnormalDesc(itemDTO.getAbnormalDesc());
                resultItem.setRemark(itemDTO.getRemark());
                resultItem.setCheckTime(itemDTO.getCheckTime() != null ? itemDTO.getCheckTime() : LocalDateTime.now());
                resultItem.setLongitude(itemDTO.getLongitude());
                resultItem.setLatitude(itemDTO.getLatitude());
                resultItem.setCreateTime(LocalDateTime.now());
                resultItem.setUpdateTime(LocalDateTime.now());
                resultItems.add(resultItem);
            }
            resultItemMapper.batchInsert(resultItems);
        }

        List<InspectionPhotoDTO> photoDTOs = submitDTO.getPhotos();
        if (photoDTOs != null && !photoDTOs.isEmpty()) {
            List<InspectionPhoto> photos = new ArrayList<>();
            for (InspectionPhotoDTO photoDTO : photoDTOs) {
                InspectionPhoto photo = new InspectionPhoto();
                photo.setPhotoNo(generatePhotoNo());
                photo.setResultId(result.getId());
                photo.setResultItemId(photoDTO.getResultItemId());
                photo.setTaskId(submitDTO.getTaskId());
                photo.setAssetId(photoDTO.getAssetId());
                photo.setPhotoType(photoDTO.getPhotoType() != null ? photoDTO.getPhotoType() : 1);
                photo.setPhotoUrl(photoDTO.getPhotoUrl());
                photo.setThumbnailUrl(photoDTO.getThumbnailUrl());
                photo.setFileSize(photoDTO.getFileSize());
                photo.setWatermarkTime(photoDTO.getWatermarkTime());
                photo.setLongitude(photoDTO.getLongitude());
                photo.setLatitude(photoDTO.getLatitude());
                photo.setHasWatermark(photoDTO.getHasWatermark() != null ? photoDTO.getHasWatermark() : 0);
                photo.setRemark(photoDTO.getRemark());
                photo.setIsOffline(photoDTO.getIsOffline() != null ? photoDTO.getIsOffline() : 0);
                photo.setSyncStatus(1);
                photos.add(photo);
            }
            photoMapper.batchInsert(photos);
        }

        List<InspectionAudioDTO> audioDTOs = submitDTO.getAudios();
        if (audioDTOs != null && !audioDTOs.isEmpty()) {
            List<InspectionAudio> audios = new ArrayList<>();
            for (InspectionAudioDTO audioDTO : audioDTOs) {
                InspectionAudio audio = new InspectionAudio();
                audio.setAudioNo(generateAudioNo());
                audio.setResultId(result.getId());
                audio.setResultItemId(audioDTO.getResultItemId());
                audio.setTaskId(submitDTO.getTaskId());
                audio.setAssetId(audioDTO.getAssetId());
                audio.setAudioUrl(audioDTO.getAudioUrl());
                audio.setFileSize(audioDTO.getFileSize());
                audio.setDuration(audioDTO.getDuration());
                audio.setRecordTime(audioDTO.getRecordTime());
                audio.setLongitude(audioDTO.getLongitude());
                audio.setLatitude(audioDTO.getLatitude());
                audio.setRemark(audioDTO.getRemark());
                audio.setIsOffline(audioDTO.getIsOffline() != null ? audioDTO.getIsOffline() : 0);
                audio.setSyncStatus(1);
                audios.add(audio);
            }
            audioMapper.batchInsert(audios);
        }

        taskService.completeTask(submitDTO.getTaskId());
        generateReport(result.getId());

        return result.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long generateReport(Long resultId) {
        InspectionResult result = getById(resultId);
        if (result == null) {
            throw new BusinessException("巡检结果不存在");
        }

        InspectionReport existReport = reportMapper.selectByResultId(resultId);
        if (existReport != null) {
            return existReport.getId();
        }

        List<InspectionResultItem> items = resultItemMapper.selectByResultId(resultId);
        List<InspectionPhoto> photos = photoMapper.selectByResultId(resultId);

        int totalItems = result.getTotalItems() != null ? result.getTotalItems() : 0;
        int normalItems = result.getNormalItems() != null ? result.getNormalItems() : 0;
        int abnormalItems = result.getAbnormalItems() != null ? result.getAbnormalItems() : 0;

        BigDecimal passRate = BigDecimal.ZERO;
        if (totalItems > 0) {
            passRate = BigDecimal.valueOf(normalItems)
                    .divide(BigDecimal.valueOf(totalItems), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        BigDecimal totalScore = passRate;

        Integer healthLevel;
        if (totalScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
            healthLevel = 1;
        } else if (totalScore.compareTo(BigDecimal.valueOf(75)) >= 0) {
            healthLevel = 2;
        } else if (totalScore.compareTo(BigDecimal.valueOf(60)) >= 0) {
            healthLevel = 3;
        } else {
            healthLevel = 4;
        }

        StringBuilder problemSummary = new StringBuilder();
        StringBuilder suggestions = new StringBuilder();
        for (InspectionResultItem item : items) {
            if (item.getIsNormal() != null && item.getIsNormal() == 0) {
                problemSummary.append("- ").append(item.getItemName());
                if (StringUtils.hasText(item.getAssetName())) {
                    problemSummary.append("（").append(item.getAssetName()).append("）");
                }
                if (StringUtils.hasText(item.getAbnormalDesc())) {
                    problemSummary.append("：").append(item.getAbnormalDesc());
                }
                problemSummary.append("\n");

                suggestions.append("- 建议检查").append(item.getItemName());
                if (StringUtils.hasText(item.getAssetName())) {
                    suggestions.append("（").append(item.getAssetName()).append("）");
                }
                suggestions.append("，及时处理异常\n");
            }
        }

        Map<String, Object> reportContent = new HashMap<>();
        reportContent.put("basicInfo", buildBasicInfo(result));
        reportContent.put("statistics", buildStatistics(result, passRate, totalScore, healthLevel));
        reportContent.put("items", items);
        reportContent.put("photos", photos);
        reportContent.put("problems", problemSummary.toString());
        reportContent.put("suggestions", suggestions.toString());

        String reportContentJson;
        try {
            reportContentJson = objectMapper.writeValueAsString(reportContent);
        } catch (Exception e) {
            reportContentJson = "{}";
        }

        InspectionReport report = new InspectionReport();
        report.setReportNo(generateReportNo());
        report.setTaskId(result.getTaskId());
        report.setResultId(resultId);
        report.setStationId(result.getStationId());
        report.setStationName(result.getStationName());
        report.setReportTitle(result.getStationName() + " - 巡检报告");
        report.setReportType(result.getResultStatus() == 1 ? 1 : 2);
        report.setTotalScore(totalScore);
        report.setHealthLevel(healthLevel);
        report.setTotalItems(totalItems);
        report.setPassRate(passRate);
        report.setAbnormalCount(abnormalItems);
        report.setProblemSummary(problemSummary.toString());
        report.setSuggestions(suggestions.toString());
        report.setReportContent(reportContentJson);
        report.setGeneratedTime(LocalDateTime.now());
        report.setGeneratorId(loginUserHolder.getCurrentUserId());
        report.setGeneratorName(loginUserHolder.getCurrentUserName());

        reportMapper.insert(report);

        return report.getId();
    }

    private Map<String, Object> buildBasicInfo(InspectionResult result) {
        Map<String, Object> info = new HashMap<>();
        info.put("resultNo", result.getResultNo());
        info.put("taskNo", result.getTaskNo());
        info.put("stationName", result.getStationName());
        info.put("inspectorName", result.getInspectorName());
        info.put("startTime", result.getStartTime());
        info.put("endTime", result.getEndTime());
        info.put("overallRemark", result.getOverallRemark());
        return info;
    }

    private Map<String, Object> buildStatistics(InspectionResult result, BigDecimal passRate, 
            BigDecimal totalScore, Integer healthLevel) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalItems", result.getTotalItems());
        stats.put("normalItems", result.getNormalItems());
        stats.put("abnormalItems", result.getAbnormalItems());
        stats.put("passRate", passRate);
        stats.put("totalScore", totalScore);
        stats.put("healthLevel", healthLevel);
        return stats;
    }

    private String generateResultNo() {
        return "IR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) 
                + String.format("%03d", (int) (Math.random() * 1000));
    }

    private String generatePhotoNo() {
        return "PH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) 
                + String.format("%03d", (int) (Math.random() * 1000));
    }

    private String generateAudioNo() {
        return "AU" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) 
                + String.format("%03d", (int) (Math.random() * 1000));
    }

    private String generateReportNo() {
        return "RP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) 
                + String.format("%03d", (int) (Math.random() * 1000));
    }

    public List<InspectionResult> getPendingSyncResults() {
        LambdaQueryWrapper<InspectionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionResult::getSyncStatus, 0);
        return list(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long uploadPhoto(Long resultId, Long resultItemId, MultipartFile file,
                            Integer photoType, BigDecimal longitude, BigDecimal latitude,
                            Boolean hasWatermark, String remark) {
        if (file.isEmpty()) {
            throw new BusinessException("照片文件不能为空");
        }

        InspectionResult result = getById(resultId);
        if (result == null) {
            throw new BusinessException("巡检结果不存在");
        }

        String fileUrl = saveFile(file, "inspection/photo");
        String thumbnailUrl = fileUrl;

        InspectionPhoto photo = new InspectionPhoto();
        photo.setPhotoNo(generatePhotoNo());
        photo.setResultId(resultId);
        photo.setResultItemId(resultItemId);
        photo.setTaskId(result.getTaskId());
        photo.setPhotoType(photoType != null ? photoType : 1);
        photo.setPhotoUrl(fileUrl);
        photo.setThumbnailUrl(thumbnailUrl);
        photo.setFileSize(file.getSize());
        photo.setWatermarkTime(LocalDateTime.now());
        photo.setLongitude(longitude);
        photo.setLatitude(latitude);
        photo.setHasWatermark(hasWatermark != null && hasWatermark ? 1 : 0);
        photo.setRemark(remark);
        photo.setIsOffline(0);
        photo.setSyncStatus(1);
        photo.setCreateTime(LocalDateTime.now());
        photo.setUpdateTime(LocalDateTime.now());

        photoMapper.insert(photo);
        return photo.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long uploadAudio(Long resultId, Long resultItemId, MultipartFile file,
                            BigDecimal longitude, BigDecimal latitude, String remark) {
        if (file.isEmpty()) {
            throw new BusinessException("录音文件不能为空");
        }

        InspectionResult result = getById(resultId);
        if (result == null) {
            throw new BusinessException("巡检结果不存在");
        }

        String fileUrl = saveFile(file, "inspection/audio");

        InspectionAudio audio = new InspectionAudio();
        audio.setAudioNo(generateAudioNo());
        audio.setResultId(resultId);
        audio.setResultItemId(resultItemId);
        audio.setTaskId(result.getTaskId());
        audio.setAudioUrl(fileUrl);
        audio.setFileSize(file.getSize());
        audio.setDuration(0);
        audio.setRecordTime(LocalDateTime.now());
        audio.setLongitude(longitude);
        audio.setLatitude(latitude);
        audio.setRemark(remark);
        audio.setIsOffline(0);
        audio.setSyncStatus(1);
        audio.setCreateTime(LocalDateTime.now());
        audio.setUpdateTime(LocalDateTime.now());

        audioMapper.insert(audio);
        return audio.getId();
    }

    private String saveFile(MultipartFile file, String subDir) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String datePath = new SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
            String saveDir = uploadPath + File.separator + subDir + File.separator + datePath;
            Path saveDirPath = Paths.get(saveDir);
            if (!Files.exists(saveDirPath)) {
                Files.createDirectories(saveDirPath);
            }

            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path filePath = saveDirPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            return accessUrl + "/" + subDir + "/" + datePath + "/" + fileName;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }
    }
}
