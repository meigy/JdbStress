package com.meigy.jstress.core;

import com.alibaba.druid.pool.DruidDataSource;
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
//@Component
public class JdbcTemplateManager {
    private final Map<String, JdbcTemplate> jdbcTemplateMap = new ConcurrentHashMap<>();
    private final Map<String, NamedParameterJdbcTemplate> namedJdbcTemplateMap = new ConcurrentHashMap<>();

    private final DataSourceManager dataSourceManager;
    //private final UserContext userContext;

    public JdbcTemplateManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    //public NamedParameterJdbcTemplate getActiveNamedJdbcTemplate() {
    //    return getNamedJdbcTemplate(dataSourceManager.getActiveDataSourceName());
    //}

    public NamedParameterJdbcTemplate getNamedJdbcTemplate(String dataSourceName) {
        return namedJdbcTemplateMap.computeIfAbsent(dataSourceName, key -> {
            return new NamedParameterJdbcTemplate(getJdbcTemplate(dataSourceName));
        });
    }

    public JdbcTemplate getJdbcTemplate(String dataSourceName) {
        return jdbcTemplateMap.computeIfAbsent(dataSourceName, key -> {
            DruidDataSource dataSource = dataSourceManager.getDataSource(key);
            return new JdbcTemplate(dataSource);
        });
    }
}
