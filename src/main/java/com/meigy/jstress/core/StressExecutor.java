package com.meigy.jstress.core;

import com.meigy.jstress.properties.StressProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import com.meigy.jstress.report.ConsoleReporter;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Future;

import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
//@Component
public class StressExecutor {
    private ThreadPoolTaskExecutor executor;
    //private final SqlGenerator sqlGenerator;
    private final MetricsCollector metricsCollector;
    private final StressProperties properties;
    private final ConsoleReporter consoleReporter;
    private volatile StressContext stressContext;

    //private final JdbcTemplateManager jdbcTemplateManager;

    public StressExecutor(StressProperties properties, MetricsCollector metricsCollector, ThreadPoolTaskExecutor defaultExecutor) {
        //DataSourceManager dataSourceManager = AppContextHolder.getBean(DataSourceManager.class);
        this.properties = properties;
        this.metricsCollector = metricsCollector;
        this.consoleReporter = new ConsoleReporter();
        init(defaultExecutor);
    }

    public void init(ThreadPoolTaskExecutor stressTestExecutor) {
                         //SqlGenerator sqlGenerator,
                         //JdbcTemplateManager jdbcTemplateManager,
                         //MetricsCollector metricsCollector,
                         //StressProperties properties,
                         //ConsoleReporter consoleReporter) {
                         //DataSourceManager dataSourceManager) {
        this.executor = stressTestExecutor;
        //this.jdbcTemplateManager = jdbcTemplateManager;
        //this.sqlGenerator = new SqlGenerator(jdbcTemplateManager, properties);
        //this.metricsCollector = metricsCollector;
    }

    public void start() {
        if (stressContext != null && stressContext.isRunning()) {
            log.warn("压测已在运行中");
            return;
        }

        String taskId = String.valueOf(System.currentTimeMillis());
        
        // 加载SQL和参数
        //sqlGenerator.loadSql(properties.getSql().getFilePath());
        //sqlGenerator.loadParams(properties.getSql().getParamsPath());
        //sqlGenerator.reLoad();
        //sqlGenerator.switchDataSource();
        
        List<Future<?>> taskFutures = new ArrayList<>();

        // 创建自动停止线程（如果需要）
        Thread autoStopThread = null;
        if (properties.getDuration() > 0) {
            autoStopThread = createAutoStopThread(taskId);
            autoStopThread.start();
        }

        // 创建上下文
        stressContext = new StressContext(taskId, metricsCollector, properties, taskFutures, autoStopThread);
        SqlGenerator sqlGenerator = new SqlGenerator();
        UserContext userContext = AppContextHolder.getBean(UserContext.class);
        sqlGenerator.switchDataSource(userContext.getDatasourceName());
        
        // 启动工作线程
        log.warn("压测开始，线程数: {}" , properties.getThreadPool().getMaxSize());
        int threadCount = properties.getThreadPool().getMaxSize();
        for (int i = 0; i < threadCount; i++) {
            Future<?> future = executor.submit(new StressTask(sqlGenerator));
            taskFutures.add(future);
        }

        consoleReporter.setSql(sqlGenerator.getSql());
        // 启动控制台报告
        consoleReporter.start(properties.getSampleRate());
    }

    public void stop(StressContext.StopReason reason) {
        if (stressContext != null && stressContext.isRunning()) {
            stressContext.shutdown(reason);
            consoleReporter.stop();
            //log.info("压测已停止，原因: {}", reason.getDescription());
            stressContext = null;
        }
    }

    private class StressTask implements Runnable {
        private final SqlGenerator sqlGenerator;
        StressTask(SqlGenerator sqlGenerator) {
            //UserContext userContext = AppContextHolder.getBean(UserContext.class);
            this.sqlGenerator = sqlGenerator;
            //sqlGenerator.reLoad();
            //sqlGenerator.switchDataSource(userContext.getDatasourceName());
        }
        @Override
        public void run() {
            while (stressContext != null && stressContext.isRunning()) {
                long startTime = System.currentTimeMillis();
                try {
                    sqlGenerator.executeSql();
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
                if (stressContext != null
                        && stressContext.isRunning()
                        && taskId.equals(stressContext.getTaskId())) {
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
        return stressContext != null && stressContext.isRunning();
    }

    @PostMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", isRunning());
        return status;
    }
} 