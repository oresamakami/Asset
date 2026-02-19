package com.example.demo.entity;

public enum AssetStatus {
    STOCK("在庫"),
    IN_USE("使用中"),
    BROKEN("故障"),
    DISPOSED("廃棄");

    private final String displayName;

    AssetStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
