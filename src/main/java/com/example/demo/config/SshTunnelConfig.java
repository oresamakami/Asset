package com.example.demo.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * SSHトンネルを管理するユーティリティ。
 * DemoApplication.main() から Spring 起動前に呼び出す。
 */
public class SshTunnelConfig {

    private static final Logger log = LoggerFactory.getLogger(SshTunnelConfig.class);
    private static Session session;

    /**
     * SSHトンネルを開始する
     */
    public static void startTunnel(String sshHost, int sshPort,
                                   String sshUser, String sshPassword,
                                   String remoteHost, int remotePort,
                                   int localPort) {
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(sshUser, sshHost, sshPort);
            session.setPassword(sshPassword);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setServerAliveInterval(30000);
            session.connect(10000);

            int assignedPort = session.setPortForwardingL(localPort, remoteHost, remotePort);
            log.info("SSH tunnel established: localhost:{} -> {}:{} (via {}@{}:{})",
                    assignedPort, remoteHost, remotePort, sshUser, sshHost, sshPort);

        } catch (Exception e) {
            log.error("SSH tunnel failed: {}", e.getMessage());
            throw new RuntimeException("SSHトンネルの接続に失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * SSHトンネルを終了する
     */
    public static void stopTunnel() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            log.info("SSH tunnel disconnected");
        }
    }
}
