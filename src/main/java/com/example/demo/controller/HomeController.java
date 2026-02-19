package com.example.demo.controller;

import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.AssignmentRepository;
import com.example.demo.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AssetRepository assetRepository;
    private final EmployeeRepository employeeRepository;
    private final AssignmentRepository assignmentRepository;

    /** ログイン画面 */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** ダッシュボード */
    @GetMapping("/")
    public String home(Model model) {
        long totalAssets = assetRepository.count();
        long totalEmployees = employeeRepository.count();
        long activeAssignments = assignmentRepository.findByReturnDateIsNull().size();
        model.addAttribute("totalAssets", totalAssets);
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("activeAssignments", activeAssignments);
        return "home";
    }
}
