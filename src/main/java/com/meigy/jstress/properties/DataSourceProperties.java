package com.meigy.jstress.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "dbconfig")
public class DataSourceProperties {
    private Map<String, DatabaseConfig> datasources;

    @Data
    public static class DatabaseConfig {
        private String name;
        private String url;
        private String driverClassName;
        private String username;
        private String password;
    }
} 