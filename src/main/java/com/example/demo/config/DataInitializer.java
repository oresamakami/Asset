package com.example.demo.config;

import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * アプリケーション起動時に初期データを投入する。
 * users テーブルが空の場合、デフォルト管理者 (admin / admin123) を作成する。
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        userService.ensureAdminExists();
        log.info("初期データチェック完了（管理者ユーザー確認済み）");
    }
}
