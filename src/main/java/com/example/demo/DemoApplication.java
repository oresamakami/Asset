package com.example.demo;

import com.example.demo.config.SshTunnelConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		// SSHトンネルを Spring 起動前に確立
		// (リモートMySQL に localhost:13306 経由でアクセスするため)
		String tunnelEnabled = System.getenv().getOrDefault("SSH_TUNNEL_DISABLED", "false");
		if (!"true".equalsIgnoreCase(tunnelEnabled)) {
			try {
				SshTunnelConfig.startTunnel(
						"192.168.11.120",  // SSHホスト
						22,                // SSHポート
						"administrator",   // SSHユーザー
						"Admin000",        // SSHパスワード
						"localhost",       // リモートMySQL ホスト
						3306,              // リモートMySQL ポート
						13306              // ローカル転送ポート
				);
			} catch (Exception e) {
				System.err.println("WARNING: SSH tunnel failed - " + e.getMessage());
				System.err.println("Continuing without tunnel (use local MySQL or set SSH_TUNNEL_DISABLED=true)");
			}
		}

		SpringApplication.run(DemoApplication.class, args);

		// シャットダウン時にトンネルを閉じる
		Runtime.getRuntime().addShutdownHook(new Thread(SshTunnelConfig::stopTunnel));
	}
}
