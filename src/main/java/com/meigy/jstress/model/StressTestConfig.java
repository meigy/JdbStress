package com.meigy.jstress.model;

public class StressTestConfig {
    private ThreadPoolConfig threadPool;
    private Integer duration;
    private Integer sampleRate;
    private SqlConfig sql;

    public static class ThreadPoolConfig {
        private Integer coreSize;
        private Integer maxSize;
        private Integer queueCapacity;

        public Integer getCoreSize() {
            return coreSize;
        }

        public void setCoreSize(Integer coreSize) {
            this.coreSize = coreSize;
        }

        public Integer getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(Integer maxSize) {
            this.maxSize = maxSize;
        }

        public Integer getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(Integer queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class SqlConfig {
        private String sql;
        private String params;

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public String getParams() {
            return params;
        }

        public void setParams(String params) {
            this.params = params;
        }
    }

    // Getters and Setters
    public ThreadPoolConfig getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ThreadPoolConfig threadPool) {
        this.threadPool = threadPool;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    public SqlConfig getSql() {
        return sql;
    }

    public void setSql(SqlConfig sql) {
        this.sql = sql;
    }
} 