package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final AssetRepository assetRepository;
    private final AssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * チェックアウト (貸出)
     */
    @Transactional
    public Assignment checkout(String employeeCode, String qrCodeId) {
        Employee employee = employeeRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new RuntimeException("社員が見つかりません: " + employeeCode));

        Asset asset = assetRepository.findByQrCodeId(qrCodeId)
                .orElseThrow(() -> new RuntimeException("資産が見つかりません: " + qrCodeId));

        if (asset.getStatus() != AssetStatus.STOCK) {
            throw new RuntimeException(
                    "この資産は貸出できません（現在のステータス: " + asset.getStatus().getDisplayName() + "）");
        }

        // 資産ステータスを「使用中」に更新
        asset.setStatus(AssetStatus.IN_USE);
        assetRepository.save(asset);

        // 貸出履歴を作成
        Assignment assignment = new Assignment();
        assignment.setEmployee(employee);
        assignment.setAsset(asset);
        assignment.setCheckoutDate(LocalDate.now());
        return assignmentRepository.save(assignment);
    }

    /**
     * チェックイン (返却)
     */
    @Transactional
    public Assignment checkin(String qrCodeId) {
        Asset asset = assetRepository.findByQrCodeId(qrCodeId)
                .orElseThrow(() -> new RuntimeException("資産が見つかりません: " + qrCodeId));

        if (asset.getStatus() != AssetStatus.IN_USE) {
            throw new RuntimeException(
                    "この資産は返却できません（現在のステータス: " + asset.getStatus().getDisplayName() + "）");
        }

        // 貸出履歴を更新 (返却日を設定)
        Assignment assignment = assignmentRepository.findByAssetAndReturnDateIsNull(asset)
                .orElseThrow(() -> new RuntimeException("貸出履歴が見つかりません"));

        assignment.setReturnDate(LocalDate.now());
        assignmentRepository.save(assignment);

        // 資産ステータスを「在庫」に戻す
        asset.setStatus(AssetStatus.STOCK);
        assetRepository.save(asset);

        return assignment;
    }
}
