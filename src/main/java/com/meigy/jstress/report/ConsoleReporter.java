package com.meigy.jstress.report;

import com.meigy.jstress.core.MetricsCollector;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ConsoleReporter {
    private final MetricsCollector metricsCollector;
    private final SimpleDateFormat dateFormat;
    private ScheduledExecutorService scheduler;

    public ConsoleReporter(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public void start(int sampleRate) {
        if (scheduler != null && !scheduler.isShutdown()) {
            stop();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::report, sampleRate, sampleRate, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void report() {
        StringBuilder report = new StringBuilder("\n");
        report.append("=========================== 压测报告 ===========================\n");
        report.append("时间: ").append(dateFormat.format(new Date())).append("\n");
        report.append("-----------------------------------------------------------\n");
        report.append(String.format("总请求数: %d\n", metricsCollector.getTotalRequests().sum()));
        report.append(String.format("成功请求数: %d\n", metricsCollector.getSuccessRequests().sum()));
        report.append(String.format("失败请求数: %d\n", metricsCollector.getFailedRequests().sum()));
        report.append(String.format("每秒成功数(TPS): %.2f\n", metricsCollector.getTps()));
        report.append(String.format("总平均响应时间: %.2f ms\n", metricsCollector.getAvgResponseTime()));
        report.append(String.format("最近平均响应时间: %.2f ms\n", metricsCollector.getRecentAvgResponseTime()));
        report.append("===========================================================\n");
        
        System.out.println(report);
    }
} 