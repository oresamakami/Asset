package com.example.demo.controller;

import com.example.demo.entity.*;
import com.example.demo.service.AssetService;
import com.example.demo.service.CsvService;
import com.example.demo.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final QrCodeService qrCodeService;
    private final CsvService csvService;

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
                asset.setImagePath(source.getImagePath());
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
        applySharedImageIfAbsent(asset);
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
        if (imageFile == null || imageFile.isEmpty()) {
            asset.setImagePath(existing.getImagePath());
        } else {
            saveImageIfPresent(asset, imageFile);
        }
        applySharedImageIfAbsent(asset);
        assetService.save(asset);
        return "redirect:/assets";
    }

    /** 削除 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        assetService.deleteById(id);
        return "redirect:/assets";
    }

    // ==================== CSV 関連 ====================

    /** CSV一括登録画面 */
    @GetMapping("/csv")
    public String csvPage() {
        return "asset/csv";
    }

    /** CSVテンプレートダウンロード */
    @GetMapping("/csv/template")
    public ResponseEntity<byte[]> downloadCsvTemplate() {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csv = csvService.getAssetCsvTemplate().getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[bom.length + csv.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(csv, 0, combined, bom.length, csv.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"asset_template.csv\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(combined);
    }

    /** CSVアップロード処理 */
    @PostMapping("/csv/upload")
    public String uploadCsv(@RequestParam("file") MultipartFile file,
                            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ファイルが選択されていません");
            return "redirect:/assets/csv";
        }

        CsvService.CsvImportResult result = csvService.importAssetCsv(file);

        redirectAttributes.addFlashAttribute("successCount", result.successCount());
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", result.errors());
        }
        return "redirect:/assets/csv";
    }

    /** 型名から共有画像を検索し、画像パスが未設定のアセットに適用する */
    @GetMapping("/api/image-by-model")
    @ResponseBody
    public Map<String, String> imageByModel(@RequestParam String modelName) {
        return assetService.findImagePathByModelName(modelName)
                .map(path -> Map.of("imagePath", path))
                .orElse(Map.of());
    }

    /** 画像が未設定のとき、同じ型名の既存画像を自動適用する */
    private void applySharedImageIfAbsent(Asset asset) {
        if (asset.getImagePath() != null) return;
        assetService.findImagePathByModelName(asset.getModelName())
                .ifPresent(asset::setImagePath);
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
