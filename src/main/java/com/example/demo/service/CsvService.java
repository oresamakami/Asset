package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CsvService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final AssetRepository assetRepository;
    private final AssetService assetService;

    // ==================== 共通 ====================

    /**
     * UTF-8 BOM の有無で文字コードを判定する。
     * BOM があれば UTF-8、なければ Shift-JIS (Windows-31J) として扱う。
     * Excel で CSV を保存すると Shift-JIS になるケースに対応。
     */
    private BufferedReader createReader(MultipartFile file) throws IOException {
        byte[] raw = file.getBytes();
        Charset charset;
        int skip = 0;

        if (raw.length >= 3
                && (raw[0] & 0xFF) == 0xEF
                && (raw[1] & 0xFF) == 0xBB
                && (raw[2] & 0xFF) == 0xBF) {
            charset = StandardCharsets.UTF_8;
            skip = 3;
        } else {
            charset = Charset.forName("Windows-31J");
        }

        return new BufferedReader(
                new InputStreamReader(
                        new ByteArrayInputStream(raw, skip, raw.length - skip), charset));
    }

    // ==================== 社員CSV ====================

    public String getEmployeeCsvTemplate() {
        return "社員番号,氏名,部署ID,課ID\nEMP001,山田太郎,1,1\n";
    }

    /** @deprecated Use {@link #getEmployeeCsvTemplate()} instead */
    @Deprecated
    public String getCsvTemplate() {
        return getEmployeeCsvTemplate();
    }

    @Transactional
    public CsvImportResult importEmployeeCsv(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = createReader(file)) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.replace("\uFEFF", "");

                if (line.trim().isEmpty()) continue;

                if (isHeader) {
                    isHeader = false;
                    if (line.contains("社員番号") || line.toLowerCase().contains("employeecode")) {
                        continue;
                    }
                }

                try {
                    String[] cols = line.split(",", -1);
                    if (cols.length < 2) {
                        errors.add(lineNumber + "行目: 列数が不足しています");
                        continue;
                    }

                    String employeeCode = cols[0].trim();
                    String name = cols[1].trim();
                    String deptIdStr = cols.length > 2 ? cols[2].trim() : "";
                    String secIdStr = cols.length > 3 ? cols[3].trim() : "";

                    if (employeeCode.isEmpty() || name.isEmpty()) {
                        errors.add(lineNumber + "行目: 社員番号と氏名は必須です");
                        continue;
                    }

                    if (employeeRepository.findByEmployeeCode(employeeCode).isPresent()) {
                        errors.add(lineNumber + "行目: 社員番号 " + employeeCode + " は既に登録済みです");
                        continue;
                    }

                    Department department = null;
                    if (!deptIdStr.isEmpty()) {
                        try {
                            Long deptId = Long.parseLong(deptIdStr);
                            Optional<Department> deptOpt = departmentRepository.findById(deptId);
                            if (deptOpt.isEmpty()) {
                                errors.add(lineNumber + "行目: 部署ID「" + deptIdStr + "」がマスタに存在しません");
                                continue;
                            }
                            department = deptOpt.get();
                        } catch (NumberFormatException e) {
                            errors.add(lineNumber + "行目: 部署ID「" + deptIdStr + "」は数値で指定してください");
                            continue;
                        }
                    }

                    Section section = null;
                    if (!secIdStr.isEmpty()) {
                        if (department == null) {
                            errors.add(lineNumber + "行目: 課IDを指定する場合は部署IDも必須です");
                            continue;
                        }
                        try {
                            Long secId = Long.parseLong(secIdStr);
                            Optional<Section> secOpt = sectionRepository.findById(secId);
                            if (secOpt.isEmpty()) {
                                errors.add(lineNumber + "行目: 課ID「" + secIdStr + "」がマスタに存在しません");
                                continue;
                            }
                            section = secOpt.get();
                            if (!section.getDepartment().getId().equals(department.getId())) {
                                errors.add(lineNumber + "行目: 課ID「" + secIdStr + "」は部署「"
                                        + department.getName() + "」に属していません");
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            errors.add(lineNumber + "行目: 課ID「" + secIdStr + "」は数値で指定してください");
                            continue;
                        }
                    }

                    Employee emp = new Employee();
                    emp.setEmployeeCode(employeeCode);
                    emp.setName(name);
                    emp.setDepartment(department);
                    emp.setSection(section);
                    employeeRepository.save(emp);
                    successCount++;

                } catch (Exception e) {
                    errors.add(lineNumber + "行目: 処理エラー - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("ファイル読み込みエラー: " + e.getMessage());
        }

        return new CsvImportResult(successCount, errors);
    }

    // ==================== 資産CSV ====================

    public String getAssetCsvTemplate() {
        return "旧管理No,資産種別,品名,型名,製造番号,OS,CPU,メモリ,ストレージ,スペック,購入日\n"
                + "PC-0001,PC,ThinkPad X1 Carbon,20U9CTO1WW,PF-123456,Windows 11 Pro,Core i7-1365U,16GB,512GB SSD,Wi-Fi 6E,2025-04-01\n"
                + "MB-0015,MOBILE,iPhone 15,MTJN3J/A,F2LXX1234,,,,128GB,,2025-06-15\n";
    }

    @Transactional
    public CsvImportResult importAssetCsv(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = createReader(file)) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.replace("\uFEFF", "");

                if (line.trim().isEmpty()) continue;

                if (isHeader) {
                    isHeader = false;
                    if (line.contains("旧管理No") || line.contains("資産種別") || line.toLowerCase().contains("assettype")) {
                        continue;
                    }
                }

                try {
                    String[] cols = line.split(",", -1);
                    if (cols.length < 3) {
                        errors.add(lineNumber + "行目: 列数が不足しています（旧管理No, 資産種別, 品名 は必須）");
                        continue;
                    }

                    String oldCode = cols[0].trim();
                    String typeStr = cols[1].trim();
                    String productName = cols[2].trim();
                    String modelName = cols.length > 3 ? cols[3].trim() : "";
                    String serialNumber = cols.length > 4 ? cols[4].trim() : "";
                    String os = cols.length > 5 ? cols[5].trim() : "";
                    String cpu = cols.length > 6 ? cols[6].trim() : "";
                    String memory = cols.length > 7 ? cols[7].trim() : "";
                    String storage = cols.length > 8 ? cols[8].trim() : "";
                    String spec = cols.length > 9 ? cols[9].trim() : "";
                    String purchaseDateStr = cols.length > 10 ? cols[10].trim() : "";

                    if (typeStr.isEmpty() || productName.isEmpty()) {
                        errors.add(lineNumber + "行目: 資産種別と品名は必須です");
                        continue;
                    }

                    if (!oldCode.isEmpty()
                            && assetRepository.findByOldManagementCode(oldCode).isPresent()) {
                        errors.add(lineNumber + "行目: 旧管理No「" + oldCode + "」は既に登録済みです");
                        continue;
                    }

                    AssetType assetType;
                    try {
                        assetType = parseAssetType(typeStr);
                    } catch (IllegalArgumentException e) {
                        errors.add(lineNumber + "行目: 資産種別「" + typeStr + "」が不正です（PC / MOBILE / 携帯 のいずれかを指定）");
                        continue;
                    }

                    if (!serialNumber.isEmpty()
                            && assetRepository.findAll().stream()
                            .anyMatch(a -> serialNumber.equals(a.getSerialNumber()))) {
                        errors.add(lineNumber + "行目: 製造番号「" + serialNumber + "」は既に登録済みです");
                        continue;
                    }

                    LocalDate purchaseDate = null;
                    if (!purchaseDateStr.isEmpty()) {
                        try {
                            purchaseDate = LocalDate.parse(purchaseDateStr);
                        } catch (DateTimeParseException e) {
                            errors.add(lineNumber + "行目: 購入日「" + purchaseDateStr + "」の形式が不正です（yyyy-MM-dd で指定）");
                            continue;
                        }
                    }

                    Asset asset = new Asset();
                    if (!oldCode.isEmpty()) asset.setOldManagementCode(oldCode);
                    asset.setAssetType(assetType);
                    asset.setQrCodeId(assetService.generateQrCodeId());
                    asset.setProductName(productName);
                    asset.setStatus(AssetStatus.STOCK);
                    if (!modelName.isEmpty()) asset.setModelName(modelName);
                    if (!serialNumber.isEmpty()) asset.setSerialNumber(serialNumber);
                    if (!os.isEmpty()) asset.setOs(os);
                    if (!cpu.isEmpty()) asset.setCpu(cpu);
                    if (!memory.isEmpty()) asset.setMemory(memory);
                    if (!storage.isEmpty()) asset.setStorage(storage);
                    if (!spec.isEmpty()) asset.setSpec(spec);
                    asset.setPurchaseDate(purchaseDate);

                    if (!modelName.isEmpty()) {
                        assetService.findImagePathByModelName(modelName)
                                .ifPresent(asset::setImagePath);
                    }

                    assetRepository.save(asset);
                    successCount++;

                } catch (Exception e) {
                    errors.add(lineNumber + "行目: 処理エラー - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("ファイル読み込みエラー: " + e.getMessage());
        }

        return new CsvImportResult(successCount, errors);
    }

    private AssetType parseAssetType(String value) {
        return switch (value.toUpperCase()) {
            case "PC" -> AssetType.PC;
            case "MOBILE", "携帯" -> AssetType.MOBILE;
            default -> throw new IllegalArgumentException("Unknown asset type: " + value);
        };
    }

    // ==================== 共通レコード ====================

    public record CsvImportResult(int successCount, List<String> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
