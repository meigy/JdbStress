package com.meigy.jstress.core;

import com.alibaba.druid.pool.DruidDataSource;
import com.meigy.jstress.config.DataSourceConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Meigy
 * @Description 对象还没有描述
 * @Date 2024-12-14 16:28
 **/
@Component
public class JdbcTemplateManager {
    private final Map<String, JdbcTemplate> jdbcTemplateMap = new ConcurrentHashMap<>();

    private final DataSourceConfig dataSourceConfig;

    public JdbcTemplateManager(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    private JdbcTemplate createJdbcTemplate(String dataSourceName) {
        return jdbcTemplateMap.computeIfAbsent(dataSourceName, key -> {
            DruidDataSource dataSource = dataSourceConfig.getDataSource(key);
            return new JdbcTemplate(dataSource);
        });
    }

    public NamedParameterJdbcTemplate createNewNamedJdbcTemplate() {
        DruidDataSource dataSource = dataSourceConfig.getDataSource(dataSourceConfig.getActiveDataSourceName());
        return new NamedParameterJdbcTemplate(new JdbcTemplate(dataSource));
    }

    public JdbcTemplate getJdbcTemplate(String dataSourceName) {
        return createJdbcTemplate(dataSourceName);
    }
}
