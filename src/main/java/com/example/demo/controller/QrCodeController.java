package com.example.demo.controller;

import com.example.demo.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qrcode")
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeService qrCodeService;

    /**
     * 任意のテキストからQRコード画像 (PNG) を返す
     * 例: /api/qrcode?text=EMP001&size=300
     */
    @GetMapping
    public ResponseEntity<byte[]> generate(
            @RequestParam String text,
            @RequestParam(defaultValue = "250") int size) {

        byte[] image = qrCodeService.generateQrCodeImage(text, size, size);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"qrcode.png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }
}
