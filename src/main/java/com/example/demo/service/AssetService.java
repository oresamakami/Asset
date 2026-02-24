package com.example.demo.service;

import com.example.demo.entity.Asset;
import com.example.demo.entity.Assignment;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssignmentRepository assignmentRepository;

    /**
     * ランダムなQRコードIDを自動生成 (例: AST-A3B2C1D4)
     * 重複しないよう確認してから返す
     */
    public String generateQrCodeId() {
        String id;
        do {
            id = "AST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (assetRepository.findByQrCodeId(id).isPresent());
        return id;
    }

    public List<Asset> findAll() {
        return assetRepository.findAll();
    }

    public Optional<Asset> findById(Long id) {
        return assetRepository.findById(id);
    }

    public Optional<Asset> findByQrCodeId(String qrCodeId) {
        return assetRepository.findByQrCodeId(qrCodeId);
    }

    public Asset save(Asset asset) {
        return assetRepository.save(asset);
    }

    public void deleteById(Long id) {
        assetRepository.deleteById(id);
    }

    /**
     * 同じ型名を持つ既存資産から画像パスを取得する。
     * 見つからなければ empty。
     */
    public Optional<String> findImagePathByModelName(String modelName) {
        if (modelName == null || modelName.isBlank()) return Optional.empty();
        return assetRepository.findFirstByModelNameAndImagePathIsNotNull(modelName)
                .map(Asset::getImagePath);
    }

    /**
     * 資産ID → 現在の貸出情報 のマップを取得 (一覧画面で使用者を表示するため)
     */
    public Map<Long, Assignment> getCurrentAssignmentMap() {
        List<Assignment> active = assignmentRepository.findByReturnDateIsNull();
        Map<Long, Assignment> map = new HashMap<>();
        for (Assignment a : active) {
            map.put(a.getAsset().getId(), a);
        }
        return map;
    }

    /**
     * 全履歴 (新しい順)
     */
    public List<Assignment> findAllAssignments() {
        return assignmentRepository.findAllByOrderByCheckoutDateDesc();
    }
}
