package com.example.demo.controller;

import com.example.demo.entity.Department;
import com.example.demo.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /** 一覧 */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("departments", departmentService.findAll());
        return "department/list";
    }

    /** 新規登録フォーム */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("department", new Department());
        return "department/form";
    }

    /** 新規保存 */
    @PostMapping
    public String save(@ModelAttribute Department department, RedirectAttributes flash) {
        departmentService.save(department);
        flash.addFlashAttribute("success", "部署「" + department.getName() + "」を登録しました");
        return "redirect:/departments";
    }

    /** 編集フォーム */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Department department = departmentService.findById(id)
                .orElseThrow(() -> new RuntimeException("部署が見つかりません"));
        model.addAttribute("department", department);
        return "department/form";
    }

    /** 更新 */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Department department, RedirectAttributes flash) {
        department.setId(id);
        departmentService.save(department);
        flash.addFlashAttribute("success", "部署情報を更新しました");
        return "redirect:/departments";
    }

    /** 削除 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            departmentService.deleteById(id);
            flash.addFlashAttribute("success", "部署を削除しました");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "この部署は使用中のため削除できません");
        }
        return "redirect:/departments";
    }
}
