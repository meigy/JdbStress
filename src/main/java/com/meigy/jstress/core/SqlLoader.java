package com.meigy.jstress.core;

import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class SqlLoader {
    private final SqlExecutor sqlExecutor;
    private String sql;
    private List<String[]> params;
    private final AtomicInteger currentParamIndex = new AtomicInteger(0);

    public SqlLoader(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    public void loadSql(String sqlPath) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(sqlPath));
        this.sql = new String(bytes, StandardCharsets.UTF_8);
        if (sql == null || sql.isEmpty()) {
            throw new IllegalArgumentException("SQL文件为空");
        }
        // 加载时就分析SQL类型
        sqlExecutor.analyzeSql(sql);
    }

    public void loadParams(String paramsPath) throws Exception {
        try (CSVReader reader = new CSVReader(new FileReader(paramsPath))) {
            params = reader.readAll();
        }
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
} 