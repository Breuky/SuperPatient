package com.superpatient.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 开发便利：随 Spring Boot 一起拉起 Python Agent 服务。
 *
 * 默认关闭（app.agent.auto-start），只在本地开。生产应由进程管理器（systemd / supervisor / k8s sidecar）
 * 负责，不要让业务进程去管别的进程的生命周期。
 *
 * 三个防御：
 *  1. 端口已被占用 → 认为 agent 已在跑，不重复启动
 *  2. 脚本不存在 → 打日志跳过，不影响主服务启动
 *  3. 应用关闭时连带杀掉子进程（避免残留占着 8001）
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.agent.auto-start", havingValue = "true")
public class AgentProcessLauncher implements DisposableBean {

    private final String script;
    private final int port;
    private Process process;

    public AgentProcessLauncher(@Value("${app.agent.script:../agent/server.py}") String script,
                                @Value("${app.agent.port:8001}") int port) {
        this.script = script;
        this.port = port;
    }

    @PostConstruct
    public void start() {
        if (portOpen()) {
            log.info("Agent 服务已在 127.0.0.1:{} 运行，跳过自动启动", port);
            return;
        }
        Path scriptPath = Paths.get(script).toAbsolutePath().normalize();
        if (!Files.isRegularFile(scriptPath)) {
            log.warn("找不到 Agent 脚本 {}（工作目录 {}），跳过自动启动；解读会走降级",
                    scriptPath, Paths.get("").toAbsolutePath());
            return;
        }
        ProcessBuilder builder = new ProcessBuilder(
                pythonCommand(), scriptPath.toString(), "--port", String.valueOf(port));
        builder.directory(new File(System.getProperty("user.dir")));
        builder.inheritIO();
        try {
            process = builder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(this::stopQuietly, "agent-shutdown"));
            log.info("已启动 Agent 服务: {}（端口 {}）", String.join(" ", builder.command()), port);
        } catch (Exception e) {
            log.warn("启动 Agent 服务失败（{}），解读会走降级: {}", e.getMessage(), scriptPath);
        }
    }

    @Override
    public void destroy() {
        stopQuietly();
    }

    private void stopQuietly() {
        if (process == null || !process.isAlive()) return;
        try {
            List<ProcessHandle> children = process.descendants().toList();
            children.forEach(ProcessHandle::destroy);
            process.destroy();
            log.info("已停止 Agent 服务");
        } catch (Exception e) {
            log.debug("停止 Agent 服务失败: {}", e.getMessage());
        }
    }

    private boolean portOpen() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 300);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String pythonCommand() {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "python" : "python3";
    }
}
