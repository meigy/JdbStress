package com.meigy.jstress.core;

import lombok.Data;
import java.util.List;
import java.util.concurrent.Future;

import com.meigy.jstress.config.StressProperties;

@Data
public class StressContext {
    public enum StopReason {
        MANUAL("手动停止"),
        TIMEOUT("时间到期"),
        ERROR("执行错误");

        private final String description;

        StopReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final MetricsCollector metricsCollector;
    private final StressProperties properties;

    private final String taskId;
    private final long startTime;
    private final Thread monitorThread;
    private final List<Future<?>> taskFutures;
    private final Thread autoStopThread;
    private volatile boolean running;
    private volatile StopReason stopReason;

    public StressContext(String taskId, MetricsCollector metricsCollector,
                         StressProperties properties, List<Future<?>> taskFutures, Thread autoStopThread) {
        this.taskId = taskId;
        this.startTime = System.currentTimeMillis();
        this.metricsCollector = metricsCollector;        
        this.properties = properties;
        this.monitorThread = startMonitorThread();
        this.taskFutures = taskFutures;
        this.autoStopThread = autoStopThread;
        this.running = true;
    }

    public void shutdown(StopReason reason) {
        this.stopReason = reason;
        this.running = false;
        monitorThread.interrupt();
        if (!StopReason.TIMEOUT.equals(reason) && autoStopThread != null) {
            autoStopThread.interrupt();
        }
        taskFutures.forEach(future -> {
            running = false;
            try {Thread.sleep(100);} catch (InterruptedException e) {}
            future.cancel(true);
        });
    }

    private Thread startMonitorThread() {
        Thread monitorThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() 
                        && this.isRunning()) {
                    metricsCollector.calculateMetrics(properties.getSampleRate());
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                // 正常中断，不需要处理
            }
        });
        monitorThread.setName("metrics-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
        return monitorThread;
    }


} 