package com.meigy.jstress.service;

import com.meigy.jstress.config.StressProperties;
import com.meigy.jstress.model.StressTestConfig;
import org.springframework.stereotype.Service;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ConfigurationService {
    private final StressProperties properties;
    private static final Logger log = LoggerFactory.getLogger(ConfigurationService.class);

    public ConfigurationService(StressProperties properties) {
        this.properties = properties;
    }

    public StressTestConfig getCurrentConfig() {
        StressTestConfig config = new StressTestConfig();
        
        // 复制线程池配置
        StressTestConfig.ThreadPoolConfig threadPool = new StressTestConfig.ThreadPoolConfig();
        threadPool.setCoreSize(properties.getThreadPool().getCoreSize());
        threadPool.setMaxSize(properties.getThreadPool().getMaxSize());
        threadPool.setQueueCapacity(properties.getThreadPool().getQueueCapacity());
        config.setThreadPool(threadPool);

        // 复制其他配置
        config.setDuration(properties.getDuration());
        config.setSampleRate(properties.getSampleRate());

        // 加载SQL配置
        StressTestConfig.SqlConfig sqlConfig = new StressTestConfig.SqlConfig();
        try {
            File sqlFile = new File(properties.getSql().getFilePath());
            if (!sqlFile.exists()) {
                log.warn("SQL文件不存在: {}", sqlFile.getAbsolutePath());
                sqlConfig.setSql("");
            } else {
                String sql = FileUtils.readFileToString(sqlFile, StandardCharsets.UTF_8);
                sqlConfig.setSql(sql);
            }

            File paramsFile = new File(properties.getSql().getParamsPath());
            if (!paramsFile.exists()) {
                log.warn("参数文件不存在: {}", paramsFile.getAbsolutePath());
                sqlConfig.setParams("");
            } else {
                String params = FileUtils.readFileToString(paramsFile, StandardCharsets.UTF_8);
                sqlConfig.setParams(params);
            }
        } catch (IOException e) {
            log.error("读取文件失败", e);
            sqlConfig.setSql("");
            sqlConfig.setParams("");
        }
        config.setSql(sqlConfig);

        return config;
    }

    public void updateConfig(StressTestConfig newConfig) throws IOException {
        // 更新线程池配置
        properties.getThreadPool().setCoreSize(newConfig.getThreadPool().getCoreSize());
        properties.getThreadPool().setMaxSize(newConfig.getThreadPool().getMaxSize());
        properties.getThreadPool().setQueueCapacity(newConfig.getThreadPool().getQueueCapacity());

        // 更新其他配置
        properties.setDuration(newConfig.getDuration());
        properties.setSampleRate(newConfig.getSampleRate());

        // 保存SQL和参数到文件
        if (newConfig.getSql() != null) {
            File sqlFile = new File(properties.getSql().getFilePath());
            FileUtils.writeStringToFile(sqlFile, newConfig.getSql().getSql(), StandardCharsets.UTF_8);
            
            File paramsFile = new File(properties.getSql().getParamsPath());
            FileUtils.writeStringToFile(paramsFile, newConfig.getSql().getParams(), StandardCharsets.UTF_8);
        }
    }
} 