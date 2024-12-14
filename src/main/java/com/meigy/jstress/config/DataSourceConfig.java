package com.meigy.jstress.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.meigy.jstress.core.JdbcDriverLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class DataSourceConfig {
    
    private final DataSourceProperties dataSourceProperties;
    private final Map<String, DruidDataSource> dataSourceMap = new ConcurrentHashMap<>();
    //private final Map<String, JdbcTemplate> jdbcTemplateMap = new ConcurrentHashMap<>();
    private final JdbcDriverLoader jdbcDriverLoader;
    
    @Value("${spring.datasource.active}")
    private String activeDataSource;

    public DataSourceConfig(DataSourceProperties dataSourceProperties, JdbcDriverLoader jdbcDriverLoader) {
        this.dataSourceProperties = dataSourceProperties;
        this.jdbcDriverLoader = jdbcDriverLoader;
    }

     @PostConstruct
     public void init() {
         // 确保在创建数据源之前加载所有驱动
         jdbcDriverLoader.loadDrivers();
     }

    // @Bean
    // public JdbcTemplate jdbcTemplate() {
    //     return createJdbcTemplate(activeDataSource);
    // }

    // @Bean
    // public NamedParameterJdbcTemplate namedJdbcTemplate() {
    //     return createNamedJdbcTemplate();
    // }


    public void switchDataSource(String dataSourceName) {
        if (!dataSourceProperties.getDatasources().containsKey(dataSourceName)) {
            throw new IllegalArgumentException("数据源不存在: " + dataSourceName);
        }
        activeDataSource = dataSourceName;
        //return createJdbcTemplate(dataSourceName);
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

    public String getActiveDataSourceName() {
        return activeDataSource;
    }

    public DruidDataSource getActiveDataSource() {
        return createDataSource(activeDataSource);
    }

    public DruidDataSource getDataSource(String dataSourceName) {
        return createDataSource(dataSourceName);
    }
} 