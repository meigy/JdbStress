package com.meigy.jstress.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "stress")
public class StressProperties {
    private ThreadPool threadPool;
    private Integer duration;
    private Integer sampleRate;
    private SqlConfig sql;

    @Data
    public static class ThreadPool {
        private Integer coreSize;
        private Integer maxSize;
        private Integer queueCapacity;
    }

    @Data
    public static class SqlConfig {
        private String filePath;
        private String paramsPath;
    }
} 