package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.service.DepartmentService;
import com.example.demo.service.SectionService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final DepartmentService departmentService;
    private final SectionService sectionService;

    /** ユーザー一覧 */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "user/list";
    }

    /** 新規登録フォーム */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("departments", departmentService.findAll());
        return "user/form";
    }

    /** 新規登録処理 */
    @PostMapping("/new")
    public String create(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String displayName,
                         @RequestParam UserRole role,
                         @RequestParam(required = false) Long departmentId,
                         @RequestParam(required = false) Long sectionId,
                         RedirectAttributes flash) {
        try {
            userService.create(username, password, displayName, role, departmentId, sectionId);
            flash.addFlashAttribute("success", "ユーザー「" + displayName + "」を登録しました");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/users/new";
        }
        return "redirect:/users";
    }

    /** 編集フォーム */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));
        model.addAttribute("user", user);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("departments", departmentService.findAll());
        if (user.getDepartment() != null) {
            model.addAttribute("sections",
                    sectionService.findByDepartmentId(user.getDepartment().getId()));
        }
        return "user/edit";
    }

    /** 更新処理 */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam String displayName,
                         @RequestParam UserRole role,
                         @RequestParam(defaultValue = "false") boolean enabled,
                         @RequestParam(required = false) Long departmentId,
                         @RequestParam(required = false) Long sectionId,
                         RedirectAttributes flash) {
        userService.update(id, displayName, role, enabled, departmentId, sectionId);
        flash.addFlashAttribute("success", "ユーザー情報を更新しました");
        return "redirect:/users";
    }

    /** パスワード変更フォーム */
    @GetMapping("/{id}/password")
    public String passwordForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));
        model.addAttribute("user", user);
        return "user/password";
    }

    /** パスワード変更処理 */
    @PostMapping("/{id}/password")
    public String changePassword(@PathVariable Long id,
                                 @RequestParam String password,
                                 @RequestParam String passwordConfirm,
                                 RedirectAttributes flash) {
        if (!password.equals(passwordConfirm)) {
            flash.addFlashAttribute("error", "パスワードが一致しません");
            return "redirect:/users/" + id + "/password";
        }
        userService.changePassword(id, password);
        flash.addFlashAttribute("success", "パスワードを変更しました");
        return "redirect:/users";
    }
}
