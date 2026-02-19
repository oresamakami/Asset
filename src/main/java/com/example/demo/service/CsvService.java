package com.example.demo.service;

import com.example.demo.entity.Department;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Section;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CsvService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;

    /**
     * CSVテンプレートのヘッダー行を返す（部署ID・課IDで指定）
     */
    public String getCsvTemplate() {
        return "社員番号,氏名,部署ID,課ID\nEMP001,山田太郎,1,1\n";
    }

    /**
     * CSVファイルを読み込み、社員を一括登録する。
     * 部署・課は ID で指定する。
     */
    @Transactional
    public CsvImportResult importEmployeeCsv(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // BOM除去
                if (lineNumber == 1) {
                    line = line.replace("\uFEFF", "");
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                // ヘッダー行スキップ
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

                    // 部署IDで照合
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

                    // 課IDで照合
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

    /**
     * CSV一括登録の結果
     */
    public record CsvImportResult(int successCount, List<String> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
