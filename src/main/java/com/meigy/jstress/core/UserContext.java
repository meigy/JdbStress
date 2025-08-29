package com.meigy.jstress.core;

import com.alibaba.druid.pool.DruidDataSource;
import com.meigy.jstress.properties.StressProperties;
import com.opencsv.CSVReader;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import javax.annotation.PostConstruct;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Meigy
 * @Description 对象还没有描述
 * @Date 2025-08-28 17:55
 **/
//@Component
@Getter
@Setter
public class UserContext {
    private final Logger logger = LoggerFactory.getLogger(UserContext.class);

    private String datasourceName = "mysql";

    private String stressSqlTemplate = "";

    private List<String[]> stressSqlParams = new ArrayList<>();

    private StressProperties stressProperties;

    public UserContext(StressProperties properties) {
        this.stressProperties = properties;
    }

    @PostConstruct
    public void init() {
        try {
            String sqlPath = stressProperties.getSql().getFilePath();
            PathMatchingResourcePatternResolver resolver1 = new PathMatchingResourcePatternResolver();
            Resource resource1 = resolver1.getResource(sqlPath);

            try (Reader reader = new InputStreamReader(resource1.getInputStream(), StandardCharsets.UTF_8)) {
                stressSqlTemplate = FileCopyUtils.copyToString(reader);
            }

            String paramsPath = stressProperties.getSql().getParamsPath();
            PathMatchingResourcePatternResolver resolver2 = new PathMatchingResourcePatternResolver();
            Resource resource2 = resolver2.getResource(paramsPath);

            try (Reader reader = new InputStreamReader(resource2.getInputStream(), StandardCharsets.UTF_8); CSVReader csvReader = new CSVReader(reader)) {
                stressSqlParams = csvReader.readAll();
            }
        } catch (Exception e) {
            logger.error("读取sql,params配置识别", e);
        }
    }

    public NamedParameterJdbcTemplate getActiveNamedJdbcTemplate() {
        JdbcTemplateManager jdbcTemplateManager = AppContextHolder.getBean(JdbcTemplateManager.class);
        return jdbcTemplateManager.getNamedJdbcTemplate(getDatasourceName());
    }

    public JdbcTemplate getActiveJdbcTemplate() {
        JdbcTemplateManager jdbcTemplateManager = AppContextHolder.getBean(JdbcTemplateManager.class);
        return jdbcTemplateManager.getJdbcTemplate(getDatasourceName());
    }

    private DruidDataSource getActiveDataSource() {
        DataSourceManager dataSourceManager = AppContextHolder.getBean(DataSourceManager.class);
        return dataSourceManager.getDataSource(getDatasourceName());
    }

}
