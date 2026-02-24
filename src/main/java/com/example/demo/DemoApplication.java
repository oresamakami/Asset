package com.example.demo;

import com.example.demo.config.SshTunnelConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.InputStream;
import java.util.Properties;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		String profile = resolveActiveProfile(args);
		Properties props = loadProfileProperties(profile);

		if (Boolean.parseBoolean(props.getProperty("ssh.tunnel.enabled", "false"))) {
			try {
				SshTunnelConfig.startTunnel(
						props.getProperty("ssh.tunnel.host"),
						Integer.parseInt(props.getProperty("ssh.tunnel.port", "22")),
						props.getProperty("ssh.tunnel.username"),
						props.getProperty("ssh.tunnel.password"),
						props.getProperty("ssh.tunnel.remote-host", "localhost"),
						Integer.parseInt(props.getProperty("ssh.tunnel.remote-port", "3306")),
						Integer.parseInt(props.getProperty("ssh.tunnel.local-port", "13306"))
				);
			} catch (Exception e) {
				System.err.println("WARNING: SSH tunnel failed - " + e.getMessage());
				System.err.println("Continuing without tunnel...");
			}
		}

		SpringApplication.run(DemoApplication.class, args);

		Runtime.getRuntime().addShutdownHook(new Thread(SshTunnelConfig::stopTunnel));
	}

	/**
	 * アクティブプロファイルを解決する。
	 * 優先順位: コマンドライン引数 > システムプロパティ > 環境変数 > application.properties
	 */
	private static String resolveActiveProfile(String[] args) {
		for (String arg : args) {
			if (arg.startsWith("--spring.profiles.active=")) {
				return arg.substring("--spring.profiles.active=".length());
			}
		}
		String fromSysProp = System.getProperty("spring.profiles.active");
		if (fromSysProp != null && !fromSysProp.isBlank()) return fromSysProp;

		String fromEnv = System.getenv("SPRING_PROFILES_ACTIVE");
		if (fromEnv != null && !fromEnv.isBlank()) return fromEnv;

		Properties base = new Properties();
		try (InputStream is = DemoApplication.class.getResourceAsStream("/application.properties")) {
			if (is != null) base.load(is);
		} catch (Exception ignored) {}
		return base.getProperty("spring.profiles.active", "test");
	}

	/**
	 * 共通 + プロファイル固有のプロパティをマージして返す。
	 */
	private static Properties loadProfileProperties(String profile) {
		Properties props = new Properties();
		try (InputStream is = DemoApplication.class.getResourceAsStream("/application.properties")) {
			if (is != null) props.load(is);
		} catch (Exception ignored) {}
		try (InputStream is = DemoApplication.class.getResourceAsStream("/application-" + profile + ".properties")) {
			if (is != null) props.load(is);
		} catch (Exception ignored) {}
		return props;
	}
}
