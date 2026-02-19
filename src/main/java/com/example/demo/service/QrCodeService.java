package com.example.demo.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Service
public class QrCodeService {

    private static final int DEFAULT_SIZE = 250;

    /**
     * QRコード画像を PNG バイト配列として生成
     */
    public byte[] generateQrCodeImage(String text, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.MARGIN, 1
            );
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("QRコード生成に失敗しました", e);
        }
    }

    /**
     * QRコード画像を PNG バイト配列として生成 (デフォルトサイズ)
     */
    public byte[] generateQrCodeImage(String text) {
        return generateQrCodeImage(text, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    /**
     * QRコード画像を Base64 エンコードされた data URI として返す
     * (HTML の img src に直接埋め込み可能)
     */
    public String generateQrCodeBase64(String text, int width, int height) {
        byte[] imageBytes = generateQrCodeImage(text, width, height);
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/png;base64," + base64;
    }

    /**
     * QRコード画像を Base64 data URI として返す (デフォルトサイズ)
     */
    public String generateQrCodeBase64(String text) {
        return generateQrCodeBase64(text, DEFAULT_SIZE, DEFAULT_SIZE);
    }
}
