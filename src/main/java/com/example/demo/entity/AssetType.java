package com.example.demo.entity;

public enum AssetType {
    PC("PC"),
    MOBILE("携帯");

    private final String displayName;

    AssetType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
