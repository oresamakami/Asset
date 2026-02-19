package com.example.demo.entity;

public enum UserRole {
    ADMIN("管理者"),
    USER("一般ユーザー");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
