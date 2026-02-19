package com.example.demo.controller;

import com.example.demo.entity.*;
import com.example.demo.service.AssetService;
import com.example.demo.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final QrCodeService qrCodeService;

    @Value("${app.upload.dir:uploads/assets}")
    private String uploadDir;

    /** 一覧 (全資産の状態 + 現在の利用者) */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("assets", assetService.findAll());
        model.addAttribute("assignmentMap", assetService.getCurrentAssignmentMap());
        return "asset/list";
    }

    /** 詳細画面 (QRコード表示) */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Asset asset = assetService.findById(id)
                .orElseThrow(() -> new RuntimeException("資産が見つかりません"));
        String qrBase64 = qrCodeService.generateQrCodeBase64(asset.getQrCodeId());

        var assignmentMap = assetService.getCurrentAssignmentMap();
        Assignment currentAssignment = assignmentMap.get(asset.getId());

        model.addAttribute("asset", asset);
        model.addAttribute("qrBase64", qrBase64);
        model.addAttribute("currentAssignment", currentAssignment);
        return "asset/detail";
    }

    /** 新規登録フォーム (コピー登録対応 / QRコードID自動発行) */
    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long copyFrom, Model model) {
        Asset asset = new Asset();
        asset.setStatus(AssetStatus.STOCK);
        asset.setQrCodeId(assetService.generateQrCodeId());

        if (copyFrom != null) {
            assetService.findById(copyFrom).ifPresent(source -> {
                asset.setAssetType(source.getAssetType());
                asset.setProductName(source.getProductName());
                asset.setModelName(source.getModelName());
                asset.setOs(source.getOs());
                asset.setCpu(source.getCpu());
                asset.setMemory(source.getMemory());
                asset.setStorage(source.getStorage());
                asset.setSpec(source.getSpec());
                asset.setPurchaseDate(source.getPurchaseDate());
            });
        }

        model.addAttribute("asset", asset);
        model.addAttribute("assetTypes", AssetType.values());
        model.addAttribute("assetStatuses", AssetStatus.values());
        return "asset/form";
    }

    /** 新規保存 */
    @PostMapping
    public String save(@ModelAttribute Asset asset,
                       @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        if (asset.getStatus() == null) {
            asset.setStatus(AssetStatus.STOCK);
        }
        if (asset.getQrCodeId() == null || asset.getQrCodeId().isBlank()) {
            asset.setQrCodeId(assetService.generateQrCodeId());
        }
        saveImageIfPresent(asset, imageFile);
        assetService.save(asset);
        return "redirect:/assets";
    }

    /** 編集フォーム */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Asset asset = assetService.findById(id)
                .orElseThrow(() -> new RuntimeException("資産が見つかりません"));
        model.addAttribute("asset", asset);
        model.addAttribute("assetTypes", AssetType.values());
        model.addAttribute("assetStatuses", AssetStatus.values());
        return "asset/form";
    }

    /** 更新 */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute Asset asset,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        Asset existing = assetService.findById(id)
                .orElseThrow(() -> new RuntimeException("資産が見つかりません"));
        asset.setId(id);
        // 画像が未送信の場合は既存パスを維持
        if (imageFile == null || imageFile.isEmpty()) {
            asset.setImagePath(existing.getImagePath());
        } else {
            saveImageIfPresent(asset, imageFile);
        }
        assetService.save(asset);
        return "redirect:/assets";
    }

    /** 削除 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        assetService.deleteById(id);
        return "redirect:/assets";
    }

    /** 画像ファイルを保存し、Asset に相対パスをセットする */
    private void saveImageIfPresent(Asset asset, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return;
        }
        try {
            Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dirPath);

            String originalFilename = imageFile.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFilename = UUID.randomUUID() + extension;

            Path filePath = dirPath.resolve(storedFilename);
            imageFile.transferTo(filePath.toFile());

            asset.setImagePath(storedFilename);
        } catch (IOException e) {
            throw new RuntimeException("画像の保存に失敗しました", e);
        }
    }
}
