package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ログインID */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** パスワード (BCrypt ハッシュ) */
    @Column(nullable = false)
    private String password;

    /** 表示名 */
    @Column(nullable = false, length = 100)
    private String displayName;

    /** 権限 (ADMIN / USER) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    /** 部署 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** 課 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    /** 有効/無効 */
    @Column(nullable = false)
    private boolean enabled = true;
}
