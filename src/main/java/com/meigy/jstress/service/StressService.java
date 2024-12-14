package com.meigy.jstress.service;

import com.meigy.jstress.config.DataSourceConfig;
import com.meigy.jstress.core.JdbcTemplateManager;
import com.meigy.jstress.core.MetricsCollector;
import com.meigy.jstress.core.StressContext;
import com.meigy.jstress.core.StressExecutor;
import com.meigy.jstress.model.MetricsResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Meigy
 * @Description 对象还没有描述
 * @Date 2024-12-14 16:07
 **/
@Service
public class StressService {
    private final StressExecutor stressExecutor;
    private final MetricsCollector metricsCollector;
    private final DataSourceConfig dataSourceConfig;
    private final JdbcTemplateManager jdbcTemplateManager;

    public StressService(StressExecutor stressExecutor, MetricsCollector metricsCollector, DataSourceConfig dataSourceConfig, JdbcTemplateManager jdbcTemplateManager) {
        this.stressExecutor = stressExecutor;
        this.metricsCollector = metricsCollector;
        this.dataSourceConfig = dataSourceConfig;
        this.jdbcTemplateManager = jdbcTemplateManager;
    }

    public void start() throws Exception {
        metricsCollector.reset();
        stressExecutor.start();
    }

    public void stop() {
        stressExecutor.stop(StressContext.StopReason.MANUAL);
    }

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

    public Map<String, Object> getDataSources() {
        Map<String, Object> result = new HashMap<>();
        result.put("active", dataSourceConfig.getActiveDataSourceName());
        result.put("available", dataSourceConfig.getAvailableDataSources());
        return result;
    }

    public StressExecutor getExecutor() {
        return stressExecutor;
    }

    public Map<String, Object> executeSql(@RequestBody Map<String, String> request) {
        String dataSource = request.get("dataSource");
        String sql = request.get("sql");

        Map<String, Object> result = new HashMap<>();
        try {
            if (sql.trim().toLowerCase().startsWith("select")) {
                // 执行查询
                List<Map<String, Object>> queryResult = jdbcTemplateManager.getJdbcTemplate(dataSource).queryForList(sql);
                result.put("isQuery", true);
                result.put("data", queryResult);
            } else {
                // 执行更新
                int affected = jdbcTemplateManager.getJdbcTemplate(dataSource).update(sql);
                result.put("isQuery", false);
                result.put("affectedRows", affected);
            }
        } catch (Exception e) {
            throw new RuntimeException("SQL执行失败: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", stressExecutor.isRunning());
        return status;
    }

}
