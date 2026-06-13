package com.solar.ops.admin.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class QrCodeUtil {

    @Value("${qrcode.upload.path:/data/qrcode}")
    private String uploadPath;

    @Value("${qrcode.access.url:/qrcode}")
    private String accessUrl;

    public String generateQrCodeBase64(String content, int width, int height) {
        try {
            BufferedImage image = generateQrCodeImage(content, width, height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.error("生成二维码失败", e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    public String generateQrCodeFile(String content, String assetCode) {
        try {
            int width = 300;
            int height = 300;
            BufferedImage image = generateQrCodeImage(content, width, height);

            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String fileName = assetCode + "_" + UUID.randomUUID().toString().replace("-", "") + ".png";
            File outputFile = new File(uploadDir.toFile(), fileName);
            ImageIO.write(image, "PNG", outputFile);

            return accessUrl + "/" + fileName;
        } catch (Exception e) {
            log.error("生成二维码文件失败", e);
            throw new RuntimeException("生成二维码文件失败", e);
        }
    }

    private BufferedImage generateQrCodeImage(String content, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);

        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
        );

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
