package com.meigy.jstress.core;

import com.meigy.jstress.config.StressProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import com.meigy.jstress.report.ConsoleReporter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Component
public class StressExecutor {
    private final JdbcTemplate jdbcTemplate;
    private final ThreadPoolTaskExecutor executor;
    private final SqlLoader sqlLoader;
    private final MetricsCollector metricsCollector;
    private final StressProperties properties;
    private final ConsoleReporter consoleReporter;
    
    private final AtomicBoolean running = new AtomicBoolean(false);

    public StressExecutor(JdbcTemplate jdbcTemplate,
                         ThreadPoolTaskExecutor stressTestExecutor,
                         SqlLoader sqlLoader,
                         MetricsCollector metricsCollector,
                         StressProperties properties,
                         ConsoleReporter consoleReporter) {
        this.jdbcTemplate = jdbcTemplate;
        this.executor = stressTestExecutor;
        this.sqlLoader = sqlLoader;
        this.metricsCollector = metricsCollector;
        this.properties = properties;
        this.consoleReporter = consoleReporter;
    }

    public void start() throws Exception {
        if (!running.compareAndSet(false, true)) {
            log.warn("压测已在运行中");
            return;
        }

        // 加载SQL和参数
        sqlLoader.loadSql(properties.getSql().getFilePath());
        sqlLoader.loadParams(properties.getSql().getParamsPath());

        // 启动监控线程
        startMonitorThread();

        // 启动工作线程
        int threadCount = properties.getThreadPool().getCoreSize();
        for (int i = 0; i < threadCount; i++) {
            executor.execute(new StressTask());
        }

        // 启动控制台报告
        consoleReporter.start(properties.getSampleRate());

        // 运行指定时间后停止
        if (properties.getDuration() > 0) {
            Thread.sleep(properties.getDuration() * 1000L);
            stop();
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            executor.shutdown();
            consoleReporter.stop();
        }
    }

    private void startMonitorThread() {
        Thread monitorThread = new Thread(() -> {
            try {
                while (running.get()) {
                    metricsCollector.calculateMetrics(properties.getSampleRate());
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        monitorThread.setName("metrics-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private class StressTask implements Runnable {
        @Override
        public void run() {
            while (running.get()) {
                long startTime = System.currentTimeMillis();
                try {
                    sqlLoader.executeSql();
                    long responseTime = System.currentTimeMillis() - startTime;
                    metricsCollector.recordSuccess(responseTime);
                } catch (Exception e) {
                    log.error("执行SQL失败", e);
                    metricsCollector.recordFailure();
                }
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    @PostMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", isRunning());
        return status;
    }
} 