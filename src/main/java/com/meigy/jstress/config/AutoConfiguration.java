package com.meigy.jstress.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.meigy.jstress.core.*;
import com.meigy.jstress.properties.DataSourceProperties;
import com.meigy.jstress.properties.DruidProperties;
import com.meigy.jstress.properties.StressProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @Author Meigy
 * @Description 对象还没有描述
 * @Date 2025-08-28 19:34
 **/
@Configuration
@DependsOn({"appContextHolder"})
public class AutoConfiguration {
//    @Bean
//    @Order(1)
//    public AppContextHolder appContextHolder() {
//        return new AppContextHolder();
//    }

    @Bean
    @Order(2)
    public JdbcDriverLoader jdbcDriverLoader() {
        return new JdbcDriverLoader();
    }

    @Bean
    @Order(3)
    public DataSourceManager dataSourceManager(DataSourceProperties dataSourceProperties, JdbcDriverLoader jdbcDriverLoader, DruidProperties druidProperties) {
        return new DataSourceManager(dataSourceProperties, jdbcDriverLoader, druidProperties);
    }

    @Bean
    @Order(4)
    public JdbcTemplateManager jdbcTemplateManager(DataSourceManager dataSourceManager) {
        return new JdbcTemplateManager(dataSourceManager);
    }

    @Bean
    @Order(5)
    public MetricsCollector metricsCollector() {
        return new MetricsCollector();
    }

    @Bean
    @Order(6)
    public StressExecutor stressExecutor(StressProperties properties, MetricsCollector metricsCollector, ThreadPoolTaskExecutor defaultExecutor) {
        return new StressExecutor(properties, metricsCollector, defaultExecutor);
    }

    @Bean
    @Order(7)
    public UserContext userContext(StressProperties properties) {
        return new UserContext(properties);
    }
}
