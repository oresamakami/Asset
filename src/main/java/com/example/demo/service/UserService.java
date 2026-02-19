package com.example.demo.service;

import com.example.demo.entity.Department;
import com.example.demo.entity.Section;
import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final PasswordEncoder passwordEncoder;

    /** 全ユーザー一覧 */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /** IDで検索 */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /** ユーザー名で検索 */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /** ユーザー名の重複チェック */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /** 新規ユーザー作成 */
    @Transactional
    public User create(String username, String rawPassword, String displayName,
                       UserRole role, Long departmentId, Long sectionId) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("ユーザー名「" + username + "」は既に使用されています");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setDisplayName(displayName);
        user.setRole(role);
        setDepartmentAndSection(user, departmentId, sectionId);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    /** 新規ユーザー作成 (部署・課なし - 初期admin用) */
    @Transactional
    public User create(String username, String rawPassword, String displayName, UserRole role) {
        return create(username, rawPassword, displayName, role, null, null);
    }

    /** ユーザー情報更新 (パスワード変更なし) */
    @Transactional
    public User update(Long id, String displayName, UserRole role, boolean enabled,
                       Long departmentId, Long sectionId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: ID=" + id));
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setEnabled(enabled);
        setDepartmentAndSection(user, departmentId, sectionId);
        return userRepository.save(user);
    }

    /** パスワード変更 */
    @Transactional
    public void changePassword(Long id, String rawPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: ID=" + id));
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }

    /** 初期管理者ユーザーがなければ作成。既にあればパスワードを修復する */
    @Transactional
    public void ensureAdminExists() {
        var existing = userRepository.findByUsername("admin");
        if (existing.isPresent()) {
            User admin = existing.get();
            if (!admin.getPassword().startsWith("$2")) {
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEnabled(true);
                userRepository.save(admin);
            }
        } else {
            create("admin", "admin123", "管理者", UserRole.ADMIN);
        }
    }

    /** 部署・課をセットするヘルパー */
    private void setDepartmentAndSection(User user, Long departmentId, Long sectionId) {
        if (departmentId != null) {
            Department dept = departmentRepository.findById(departmentId).orElse(null);
            user.setDepartment(dept);
        } else {
            user.setDepartment(null);
        }
        if (sectionId != null) {
            Section sec = sectionRepository.findById(sectionId).orElse(null);
            user.setSection(sec);
        } else {
            user.setSection(null);
        }
    }
}
