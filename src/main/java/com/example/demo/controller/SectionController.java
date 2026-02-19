package com.example.demo.controller;

import com.example.demo.entity.Department;
import com.example.demo.entity.Section;
import com.example.demo.service.DepartmentService;
import com.example.demo.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;
    private final DepartmentService departmentService;

    /** 一覧 */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("sections", sectionService.findAll());
        return "section/list";
    }

    /** 新規登録フォーム */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("section", new Section());
        model.addAttribute("departments", departmentService.findAll());
        return "section/form";
    }

    /** 新規保存 */
    @PostMapping
    public String save(@RequestParam String name,
                       @RequestParam Long departmentId,
                       RedirectAttributes flash) {
        Department dept = departmentService.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部署が見つかりません"));
        Section section = new Section();
        section.setName(name);
        section.setDepartment(dept);
        sectionService.save(section);
        flash.addFlashAttribute("success", "課「" + name + "」を登録しました");
        return "redirect:/sections";
    }

    /** 編集フォーム */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Section section = sectionService.findById(id)
                .orElseThrow(() -> new RuntimeException("課が見つかりません"));
        model.addAttribute("section", section);
        model.addAttribute("departments", departmentService.findAll());
        return "section/form";
    }

    /** 更新 */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam Long departmentId,
                         RedirectAttributes flash) {
        Section section = sectionService.findById(id)
                .orElseThrow(() -> new RuntimeException("課が見つかりません"));
        Department dept = departmentService.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部署が見つかりません"));
        section.setName(name);
        section.setDepartment(dept);
        sectionService.save(section);
        flash.addFlashAttribute("success", "課情報を更新しました");
        return "redirect:/sections";
    }

    /** 削除 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            sectionService.deleteById(id);
            flash.addFlashAttribute("success", "課を削除しました");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "この課は使用中のため削除できません");
        }
        return "redirect:/sections";
    }

    /** AJAX: 部署IDに紐づく課一覧をJSON返却 */
    @GetMapping("/api/by-department/{departmentId}")
    @ResponseBody
    public List<SectionDto> getByDepartment(@PathVariable Long departmentId) {
        return sectionService.findByDepartmentId(departmentId).stream()
                .map(s -> new SectionDto(s.getId(), s.getName()))
                .toList();
    }

    /** AJAX用DTO */
    public record SectionDto(Long id, String name) {}
}
