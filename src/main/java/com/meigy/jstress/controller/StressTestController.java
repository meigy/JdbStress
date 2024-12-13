package com.meigy.jstress.controller;

import com.meigy.jstress.core.MetricsCollector;
import com.meigy.jstress.core.StressExecutor;
import com.meigy.jstress.model.StressTestConfig;
import com.meigy.jstress.service.ConfigurationService;
import com.meigy.jstress.config.DataSourceConfig;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stress")
public class StressTestController {
    private final StressExecutor stressExecutor;
    private final MetricsCollector metricsCollector;
    private final ConfigurationService configurationService;
    private final DataSourceConfig dataSourceConfig;

    public StressTestController(StressExecutor stressExecutor, MetricsCollector metricsCollector, ConfigurationService configurationService, DataSourceConfig dataSourceConfig) {
        this.stressExecutor = stressExecutor;
        this.metricsCollector = metricsCollector;
        this.configurationService = configurationService;
        this.dataSourceConfig = dataSourceConfig;
    }

    @PostMapping("/start")
    public void start() throws Exception {
        stressExecutor.start();
    }

    @PostMapping("/stop")
    public void stop() {
        stressExecutor.stop();
    }

    @GetMapping("/metrics")
    public MetricsResponse getMetrics() {
        MetricsResponse response = new MetricsResponse();
        response.setTotalRequests(metricsCollector.getTotalRequests().sum());
        response.setSuccessRequests(metricsCollector.getSuccessRequests().sum());
        response.setFailedRequests(metricsCollector.getFailedRequests().sum());
        response.setTps(metricsCollector.getTps());
        response.setAvgResponseTime(metricsCollector.getAvgResponseTime());
        response.setRecentAvgResponseTime(metricsCollector.getRecentAvgResponseTime());
        return response;
    }

    @GetMapping("/config")
    public StressTestConfig getConfig() {
        return configurationService.getCurrentConfig();
    }

    @PutMapping("/config")
    public void updateConfig(@RequestBody StressTestConfig config) throws IOException {
        if (stressExecutor.isRunning()) {
            throw new IllegalStateException("Cannot update configuration while test is running");
        }
        configurationService.updateConfig(config);
    }

    @GetMapping("/datasources")
    public Map<String, Object> getDataSources() {
        Map<String, Object> result = new HashMap<>();
        result.put("active", dataSourceConfig.getActiveDataSource());
        result.put("available", dataSourceConfig.getAvailableDataSources());
        return result;
    }

    @PostMapping("/datasource/switch")
    public void switchDataSource(@RequestParam String name) {
        if (stressExecutor.isRunning()) {
            throw new IllegalStateException("压测运行中不能切换数据源");
        }
        dataSourceConfig.switchDataSource(name);
    }

    @PostMapping("/execute-sql")
    public Map<String, Object> executeSql(@RequestBody Map<String, String> request) {
        String dataSource = request.get("dataSource");
        String sql = request.get("sql");
        
        // 切换数据源
        if (!dataSourceConfig.getActiveDataSource().equals(dataSource)) {
            dataSourceConfig.switchDataSource(dataSource);
        }

        Map<String, Object> result = new HashMap<>();
        try {
            if (sql.trim().toLowerCase().startsWith("select")) {
                // 执行查询
                List<Map<String, Object>> queryResult = dataSourceConfig.getJdbcTemplate().queryForList(sql);
                result.put("isQuery", true);
                result.put("data", queryResult);
            } else {
                // 执行更新
                int affected = dataSourceConfig.getJdbcTemplate().update(sql);
                result.put("isQuery", false);
                result.put("affectedRows", affected);
            }
        } catch (Exception e) {
            throw new RuntimeException("SQL执行失败: " + e.getMessage());
        }
        return result;
    }

    public static class MetricsResponse {
        private long totalRequests;
        private long successRequests;
        private long failedRequests;
        private double tps;
        private double avgResponseTime;
        private double recentAvgResponseTime;

        // Getters and Setters
        public long getTotalRequests() {
            return totalRequests;
        }

        public void setTotalRequests(long totalRequests) {
            this.totalRequests = totalRequests;
        }

        public long getSuccessRequests() {
            return successRequests;
        }

        public void setSuccessRequests(long successRequests) {
            this.successRequests = successRequests;
        }

        public long getFailedRequests() {
            return failedRequests;
        }

        public void setFailedRequests(long failedRequests) {
            this.failedRequests = failedRequests;
        }

        public double getTps() {
            return tps;
        }

        public void setTps(double tps) {
            this.tps = tps;
        }

        public double getAvgResponseTime() {
            return avgResponseTime;
        }

        public void setAvgResponseTime(double avgResponseTime) {
            this.avgResponseTime = avgResponseTime;
        }

        public double getRecentAvgResponseTime() {
            return recentAvgResponseTime;
        }

        public void setRecentAvgResponseTime(double recentAvgResponseTime) {
            this.recentAvgResponseTime = recentAvgResponseTime;
        }
    }
} 