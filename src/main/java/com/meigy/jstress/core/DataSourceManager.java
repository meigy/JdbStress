package com.meigy.jstress.core;

import com.alibaba.druid.pool.DruidDataSource;
import com.meigy.jstress.core.JdbcDriverLoader;
import com.meigy.jstress.properties.DataSourceProperties;
import com.meigy.jstress.properties.DruidProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
//@Configuration
public class DataSourceManager {
    
    private final DataSourceProperties dataSourceProperties;
    private final Map<String, DruidDataSource> dataSourceMap = new ConcurrentHashMap<>();
    //private final Map<String, JdbcTemplate> jdbcTemplateMap = new ConcurrentHashMap<>();
    private final JdbcDriverLoader jdbcDriverLoader;
    private final DruidProperties druidProperties;

    public DataSourceManager(DataSourceProperties dataSourceProperties, JdbcDriverLoader jdbcDriverLoader, DruidProperties druidProperties) {
        this.dataSourceProperties = dataSourceProperties;
        this.jdbcDriverLoader = jdbcDriverLoader;
        this.druidProperties = druidProperties;
    }

    @PostConstruct
    public void init() {
        // 确保在创建数据源之前加载所有驱动
        //jdbcDriverLoader.loadDrivers();
    }

    // @Bean
    // public JdbcTemplate jdbcTemplate() {
    //     return createJdbcTemplate(activeDataSource);
    // }

    // @Bean
    // public NamedParameterJdbcTemplate namedJdbcTemplate() {
    //     return createNamedJdbcTemplate();
    // }


    private DruidDataSource createDataSource(String dataSourceName) {
        return dataSourceMap.computeIfAbsent(dataSourceName, key -> {
            DataSourceProperties.DatabaseConfig config = dataSourceProperties.getDatasources().get(key);
            DruidDataSource druidDataSource = new DruidDataSource();
            druidDataSource.setUrl(config.getUrl());
            druidDataSource.setDriverClassName(config.getDriverClassName());
            druidDataSource.setUsername(config.getUsername());
            druidDataSource.setPassword(config.getPassword());
            // 设置连接池配置
            druidDataSource.setInitialSize(druidProperties.getInitialSize());
            druidDataSource.setMinIdle(druidProperties.getMinIdle());
            druidDataSource.setMaxActive(druidProperties.getMaxActive());
            druidDataSource.setMaxWait(druidProperties.getMaxWait());
            druidDataSource.setTimeBetweenEvictionRunsMillis(druidProperties.getTimeBetweenEvictionRunsMillis());
            druidDataSource.setMinEvictableIdleTimeMillis(druidProperties.getMinEvictableIdleTimeMillis());
            druidDataSource.setValidationQuery(druidProperties.getValidationQuery());
            druidDataSource.setTestWhileIdle(druidProperties.getTestWhileIdle());
            druidDataSource.setTestOnBorrow(druidProperties.getTestOnBorrow());
            druidDataSource.setTestOnReturn(druidProperties.getTestOnReturn());
            druidDataSource.setPoolPreparedStatements(druidProperties.getPoolPreparedStatements());
            druidDataSource.setMaxPoolPreparedStatementPerConnectionSize(druidProperties.getMaxPoolPreparedStatementPerConnectionSize());
            try {
                druidDataSource.init();
            } catch (Exception e) {
                log.error("Failed to initialize DruidDataSource for {}", key, e);
                throw new RuntimeException(e);
            }
            return druidDataSource;
        });
    }

    public Map<String, DataSourceProperties.DatabaseConfig> getAvailableDataSources() {
        return dataSourceProperties.getDatasources();
    }

    public DruidDataSource getDataSource(String dataSourceName) {
        return createDataSource(dataSourceName);
    }
} 