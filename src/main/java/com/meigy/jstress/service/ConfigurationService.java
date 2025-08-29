package com.meigy.jstress.service;

import com.meigy.jstress.core.UserContext;
import com.meigy.jstress.properties.StressProperties;
import com.meigy.jstress.model.StressTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

@Service
public class ConfigurationService {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationService.class);
    @Autowired
    private StressProperties properties;
    @Autowired
    private UserContext userContext;

    //public ConfigurationService(StressProperties properties) {
    //    this.properties = properties;
    //}

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
        /*
        // 加载SQL配置
        StressTestConfig.SqlConfig sqlConfig = new StressTestConfig.SqlConfig();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            
            // 读取SQL文件
            Resource sqlResource = resolver.getResource(properties.getSql().getFilePath());
            try (Reader reader = new InputStreamReader(sqlResource.getInputStream(), StandardCharsets.UTF_8)) {
                sqlConfig.setSql(FileCopyUtils.copyToString(reader));
            }

            // 读取参数文件
            Resource paramsResource = resolver.getResource(properties.getSql().getParamsPath());
            try (Reader reader = new InputStreamReader(paramsResource.getInputStream(), StandardCharsets.UTF_8)) {
                sqlConfig.setParams(FileCopyUtils.copyToString(reader));
            }
        } catch (IOException e) {
            log.error("读取配置文件失败", e);
            sqlConfig.setSql("");
            sqlConfig.setParams("");
        }
        */
        StressTestConfig.SqlConfig sqlConfig = new StressTestConfig.SqlConfig();
        sqlConfig.setSql(userContext.getStressSqlTemplate());
        String sqlParams = "";
        for (String[] param : userContext.getStressSqlParams()) {
            sqlParams += String.join(",", param) + "\n";
        }
        sqlConfig.setParams(sqlParams);
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
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            
            // 保存SQL文件
            Resource sqlResource = resolver.getResource(properties.getSql().getFilePath());
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(sqlResource.getFile()), StandardCharsets.UTF_8)) {
                writer.write(newConfig.getSql().getSql());
            }
            userContext.setStressSqlTemplate(newConfig.getSql().getSql());
            
            // 保存参数文件
            Resource paramsResource = resolver.getResource(properties.getSql().getParamsPath());
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(paramsResource.getFile()), StandardCharsets.UTF_8)) {
                writer.write(newConfig.getSql().getParams());
            }
            List<String[]> paramsList = new java.util.ArrayList<>();
            String[] lines = newConfig.getSql().getParams().split("\\r?\\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    paramsList.add(line.split(","));
                }
            }
            userContext.setStressSqlParams(paramsList);
        }

    }
} 