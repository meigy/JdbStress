package com.meigy.jstress.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class DataSourceConfig {
    
    private final DataSourceProperties dataSourceProperties;
    private final Map<String, DruidDataSource> dataSourceMap = new ConcurrentHashMap<>();
    private final Map<String, JdbcTemplate> jdbcTemplateMap = new ConcurrentHashMap<>();
    
    @Value("${spring.datasource.active}")
    private String activeDataSource;

    public DataSourceConfig(DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return createJdbcTemplate(activeDataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedJdbcTemplate() {
        return new NamedParameterJdbcTemplate(createJdbcTemplate(activeDataSource));
    }

    public JdbcTemplate switchDataSource(String dataSourceName) {
        if (!dataSourceProperties.getDatasources().containsKey(dataSourceName)) {
            throw new IllegalArgumentException("数据源不存在: " + dataSourceName);
        }
        return createJdbcTemplate(dataSourceName);
    }

    private JdbcTemplate createJdbcTemplate(String dataSourceName) {
        return jdbcTemplateMap.computeIfAbsent(dataSourceName, key -> {
            DruidDataSource dataSource = createDataSource(key);
            return new JdbcTemplate(dataSource);
        });
    }

    private DruidDataSource createDataSource(String dataSourceName) {
        return dataSourceMap.computeIfAbsent(dataSourceName, key -> {
            DataSourceProperties.DatabaseConfig config = dataSourceProperties.getDatasources().get(key);
            DruidDataSource druidDataSource = new DruidDataSource();
            druidDataSource.setUrl(config.getUrl());
            druidDataSource.setDriverClassName(config.getDriverClassName());
            druidDataSource.setUsername(config.getUsername());
            druidDataSource.setPassword(config.getPassword());
            // 设置连接池配置
            druidDataSource.setInitialSize(5);
            druidDataSource.setMinIdle(5);
            druidDataSource.setMaxActive(20);
            druidDataSource.setMaxWait(60000);
            return druidDataSource;
        });
    }

    public Map<String, DataSourceProperties.DatabaseConfig> getAvailableDataSources() {
        return dataSourceProperties.getDatasources();
    }

    public String getActiveDataSource() {
        return activeDataSource;
    }

    public JdbcTemplate getJdbcTemplate() {
        return createJdbcTemplate(activeDataSource);
    }
} 