package com.meigy.jstress.controller;

import com.meigy.jstress.core.UserContext;
import com.meigy.jstress.model.StressTestConfig;
import com.meigy.jstress.service.ConfigurationService;
import com.meigy.jstress.service.StressService;
import com.meigy.jstress.model.MetricsResponse;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/stress")
public class StressTestController {

    @Resource
    private StressService stressService;
    @Resource
    private  ConfigurationService configurationService;
    @Resource
    private UserContext userContext;

    @PostMapping("/start")
    public void start() throws Exception {
        stressService.start();
    }

    @PostMapping("/stop")
    public void stop() {
        stressService.stop();
    }

    @GetMapping("/metrics")
    public MetricsResponse getMetrics() {
        return stressService.getMetrics();
    }

    @GetMapping("/config")
    public StressTestConfig getConfig() {
        return configurationService.getCurrentConfig();
    }

    @PutMapping("/config")
    public void updateConfig(@RequestBody StressTestConfig config) throws IOException {
        if (stressService.getExecutor().isRunning()) {
            throw new IllegalStateException("Cannot update configuration while test is running");
        }
        configurationService.updateConfig(config);
    }

    @GetMapping("/datasources")
    public Map<String, Object> getDataSources() {
        return stressService.getDataSources();
    }

    @PostMapping("/datasource/switch")
    public void switchDataSource(@RequestParam String name) {
        if (stressService.getExecutor().isRunning()) {
            throw new IllegalStateException("压测运行中不能切换数据源");
        }
        userContext.setDatasourceName(name);
    }

    @PostMapping("/execute-sql")
    public Map<String, Object> executeSql(@RequestBody Map<String, String> request) {
        return stressService.executeSql(request);
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return stressService.getStatus();
    }
} 