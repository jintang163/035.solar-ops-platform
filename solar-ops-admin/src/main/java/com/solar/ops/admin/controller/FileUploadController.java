package com.solar.ops.admin.controller;

import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/files")
@Api(tags = "文件上传")
public class FileUploadController {

    @Value("${file.upload.path:/data/upload}")
    private String uploadPath;

    @Value("${file.access.url:/files}")
    private String accessUrl;

    @PostMapping("/upload")
    @ApiOperation(value = "上传文件")
    public Result<Map<String, String>> upload(@ApiParam(value = "文件") @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String datePath = sdf.format(new Date());
            String saveDir = uploadPath + File.separator + datePath;
            Path saveDirPath = Paths.get(saveDir);
            if (!Files.exists(saveDirPath)) {
                Files.createDirectories(saveDirPath);
            }

            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path filePath = saveDirPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            String fileUrl = accessUrl + "/" + datePath + "/" + fileName;

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("fileName", fileName);
            result.put("originalFilename", originalFilename);

            return Result.success(result);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-upload")
    @ApiOperation(value = "批量上传文件")
    public Result<List<Map<String, String>>> batchUpload(@ApiParam(value = "文件列表") @RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.error("文件不能为空");
        }

        List<Map<String, String>> resultList = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                Result<Map<String, String>> result = upload(file);
                if (result.isSuccess() && result.getData() != null) {
                    resultList.add(result.getData());
                }
            }
        }

        return Result.success(resultList);
    }
}
