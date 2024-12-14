package com.meigy.jstress.core;

import com.meigy.jstress.config.StressProperties;
import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class SqlLoader {
    private final SqlExecutor sqlExecutor;
    private String sql;
    private List<String[]> params;
    private final AtomicInteger currentParamIndex = new AtomicInteger(0);
    private StressProperties properties;
    
    public SqlLoader(SqlExecutor sqlExecutor,StressProperties properties) {
        this.sqlExecutor = sqlExecutor;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        try {
            loadSql(properties.getSql().getFilePath());
            loadParams(properties.getSql().getParamsPath());
            analyzeSql();
        } catch (Exception e) {
            log.error("初始化SQL加载器失败", e);
        }
    }

    public void loadSql(String sqlPath) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(sqlPath);
        
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            sql = FileCopyUtils.copyToString(reader);
        }
        
        if (sql == null || sql.isEmpty()) {
            throw new IllegalArgumentException("SQL文件为空");
        }
        
    }

    public void loadParams(String paramsPath) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(paramsPath);
        
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {
            params = csvReader.readAll();
        }
    }

    public void analyzeSql() {
        sqlExecutor.analyzeSql(sql);
    }

    public void executeSql() {
        String[] currentParams = getNextParams();
        sqlExecutor.execute(sql, currentParams);
    }

    public void switchDataSource() {
        sqlExecutor.switchDataSource();
    }

    private String[] getNextParams() {
        if (params == null || params.isEmpty()) {
            return new String[0];
        }
        int index = currentParamIndex.getAndIncrement() % params.size();
        return params.get(index);
    }

    public String getSql() {
        return sql;
    }
} 