package com.example.demo.controller;

import com.example.demo.entity.Assignment;
import com.example.demo.service.AssetService;
import com.example.demo.service.EmployeeService;
import com.example.demo.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;
    private final EmployeeService employeeService;
    private final AssetService assetService;

    /** QRスキャン画面 */
    @GetMapping("/operation")
    public String scanPage() {
        return "operation/scan";
    }

    /** API: 社員コードで社員を検索 */
    @GetMapping("/api/employees/by-code/{code}")
    @ResponseBody
    public ResponseEntity<?> findEmployee(@PathVariable String code) {
        return employeeService.findByEmployeeCode(code)
                .map(e -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", e.getId());
                    map.put("employeeCode", e.getEmployeeCode());
                    map.put("name", e.getName());
                    map.put("department", e.getDepartment() != null ? e.getDepartment().getName() : "");
                    map.put("section", e.getSection() != null ? e.getSection().getName() : "");
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** API: QRコードIDで資産を検索 */
    @GetMapping("/api/assets/by-qr/{qrCodeId}")
    @ResponseBody
    public ResponseEntity<?> findAsset(@PathVariable String qrCodeId) {
        return assetService.findByQrCodeId(qrCodeId)
                .map(a -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", a.getId());
                    map.put("qrCodeId", a.getQrCodeId());
                    map.put("productName", a.getProductName());
                    map.put("modelName", a.getModelName() != null ? a.getModelName() : "");
                    map.put("serialNumber", a.getSerialNumber() != null ? a.getSerialNumber() : "");
                    map.put("status", a.getStatus().name());
                    map.put("statusDisplay", a.getStatus().getDisplayName());
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** API: チェックアウト (貸出) */
    @PostMapping("/api/operation/checkout")
    @ResponseBody
    public ResponseEntity<?> checkout(@RequestBody Map<String, String> body) {
        try {
            String employeeCode = body.get("employeeCode");
            String qrCodeId = body.get("qrCodeId");
            Assignment a = operationService.checkout(employeeCode, qrCodeId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message",
                    a.getEmployee().getName() + " さんに " + a.getAsset().getProductName() + " を貸出しました");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /** API: チェックイン (返却) */
    @PostMapping("/api/operation/checkin")
    @ResponseBody
    public ResponseEntity<?> checkin(@RequestBody Map<String, String> body) {
        try {
            String qrCodeId = body.get("qrCodeId");
            Assignment a = operationService.checkin(qrCodeId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message",
                    a.getAsset().getProductName() + " が返却されました（" + a.getEmployee().getName() + " さん）");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
