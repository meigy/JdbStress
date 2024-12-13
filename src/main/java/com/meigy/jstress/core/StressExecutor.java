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
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Future;

import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Component
public class StressExecutor {
    private final ThreadPoolTaskExecutor executor;
    private final SqlLoader sqlLoader;
    private final MetricsCollector metricsCollector;
    private final StressProperties properties;
    private final ConsoleReporter consoleReporter;
    
    private volatile StressContext currentContext;

    public StressExecutor(ThreadPoolTaskExecutor stressTestExecutor,
                         SqlLoader sqlLoader,
                         MetricsCollector metricsCollector,
                         StressProperties properties,
                         ConsoleReporter consoleReporter) {
        this.executor = stressTestExecutor;
        this.sqlLoader = sqlLoader;
        this.metricsCollector = metricsCollector;
        this.properties = properties;
        this.consoleReporter = consoleReporter;
    }

    public void start() throws Exception {
        if (currentContext != null && currentContext.isRunning()) {
            log.warn("压测已在运行中");
            return;
        }

        String taskId = String.valueOf(System.currentTimeMillis());
        
        // 加载SQL和参数
        sqlLoader.loadSql(properties.getSql().getFilePath());
        sqlLoader.loadParams(properties.getSql().getParamsPath());
        sqlLoader.switchDataSource();
        
        List<Future<?>> taskFutures = new ArrayList<>();

        // 创建自动停止线程（如果需要）
        Thread autoStopThread = null;
        if (properties.getDuration() > 0) {
            autoStopThread = createAutoStopThread(taskId);
            autoStopThread.start();
        }

        // 创建上下文
        currentContext = new StressContext(taskId, metricsCollector, properties, taskFutures, autoStopThread);
        
        // 启动工作线程
        int threadCount = properties.getThreadPool().getCoreSize();
        for (int i = 0; i < threadCount; i++) {
            Future<?> future = executor.submit(new StressTask());
            taskFutures.add(future);
        }

        // 启动控制台报告
        consoleReporter.start(properties.getSampleRate());
    }

    public void stop(StressContext.StopReason reason) {
        if (currentContext != null && currentContext.isRunning()) {
            currentContext.shutdown(reason);
            consoleReporter.stop();
            //log.info("压测已停止，原因: {}", reason.getDescription());
            currentContext = null;
        }
    }

    private class StressTask implements Runnable {
        @Override
        public void run() {
            while (currentContext != null && currentContext.isRunning()) {
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

    private Thread createAutoStopThread(String taskId) {
        Thread autoStopThread = new Thread(() -> {
            try {
                Thread.sleep(properties.getDuration() * 1000L);
                if (currentContext != null 
                        && currentContext.isRunning() 
                        && taskId.equals(currentContext.getTaskId())) {
                    stop(StressContext.StopReason.TIMEOUT);
                }
            } catch (InterruptedException e) {
                // 正常中断，不需要处理
            }
        });
        autoStopThread.setDaemon(true);
        return autoStopThread;
    }

    public boolean isRunning() {
        return currentContext != null && currentContext.isRunning();
    }

    @PostMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", isRunning());
        return status;
    }
} 