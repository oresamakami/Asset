package com.example.demo.controller;

import com.example.demo.entity.Assignment;
import com.example.demo.entity.Department;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Section;
import com.example.demo.repository.AssignmentRepository;
import com.example.demo.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final SectionService sectionService;
    private final QrCodeService qrCodeService;
    private final CsvService csvService;
    private final AssignmentRepository assignmentRepository;

    /** 一覧 */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("employees", employeeService.findAll());
        return "employee/list";
    }

    /** 詳細画面 (QRコード表示 + 貸出中資産一覧) */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Employee employee = employeeService.findById(id)
                .orElseThrow(() -> new RuntimeException("社員が見つかりません"));
        String qrBase64 = qrCodeService.generateQrCodeBase64(employee.getEmployeeCode());
        List<Assignment> activeAssignments = assignmentRepository.findByEmployeeAndReturnDateIsNull(employee);
        model.addAttribute("employee", employee);
        model.addAttribute("qrBase64", qrBase64);
        model.addAttribute("activeAssignments", activeAssignments);
        return "employee/detail";
    }

    /** 新規登録フォーム */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("employee", new Employee());
        model.addAttribute("departments", departmentService.findAll());
        return "employee/form";
    }

    /** 新規保存 */
    @PostMapping
    public String save(@RequestParam String employeeCode,
                       @RequestParam String name,
                       @RequestParam(required = false) Long departmentId,
                       @RequestParam(required = false) Long sectionId) {
        Employee employee = new Employee();
        employee.setEmployeeCode(employeeCode);
        employee.setName(name);
        setDepartmentAndSection(employee, departmentId, sectionId);
        employeeService.save(employee);
        return "redirect:/employees";
    }

    /** 編集フォーム */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Employee employee = employeeService.findById(id)
                .orElseThrow(() -> new RuntimeException("社員が見つかりません"));
        model.addAttribute("employee", employee);
        model.addAttribute("departments", departmentService.findAll());
        if (employee.getDepartment() != null) {
            model.addAttribute("sections",
                    sectionService.findByDepartmentId(employee.getDepartment().getId()));
        }
        return "employee/form";
    }

    /** 更新 */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String employeeCode,
                         @RequestParam String name,
                         @RequestParam(required = false) Long departmentId,
                         @RequestParam(required = false) Long sectionId) {
        Employee employee = employeeService.findById(id)
                .orElseThrow(() -> new RuntimeException("社員が見つかりません"));
        employee.setEmployeeCode(employeeCode);
        employee.setName(name);
        setDepartmentAndSection(employee, departmentId, sectionId);
        employeeService.save(employee);
        return "redirect:/employees";
    }

    /** 削除 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        employeeService.deleteById(id);
        return "redirect:/employees";
    }

    /** 部署・課をセットするヘルパー */
    private void setDepartmentAndSection(Employee employee, Long departmentId, Long sectionId) {
        if (departmentId != null) {
            Department dept = departmentService.findById(departmentId).orElse(null);
            employee.setDepartment(dept);
        } else {
            employee.setDepartment(null);
        }
        if (sectionId != null) {
            Section sec = sectionService.findById(sectionId).orElse(null);
            employee.setSection(sec);
        } else {
            employee.setSection(null);
        }
    }

    // ==================== CSV 関連 ====================

    /** CSV一括登録画面 */
    @GetMapping("/csv")
    public String csvPage() {
        return "employee/csv";
    }

    /** CSVテンプレートダウンロード */
    @GetMapping("/csv/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csv = csvService.getEmployeeCsvTemplate().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] combined = new byte[bom.length + csv.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(csv, 0, combined, bom.length, csv.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"employee_template.csv\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(combined);
    }

    /** CSVアップロード処理 */
    @PostMapping("/csv/upload")
    public String uploadCsv(@RequestParam("file") MultipartFile file,
                            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ファイルが選択されていません");
            return "redirect:/employees/csv";
        }

        CsvService.CsvImportResult result = csvService.importEmployeeCsv(file);

        redirectAttributes.addFlashAttribute("successCount", result.successCount());
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", result.errors());
        }
        return "redirect:/employees/csv";
    }
}
