package com.meigy.jstress.model;

/**
 * @Author Meigy
 * @Description 对象还没有描述
 * @Date 2024-12-14 16:09
 **/
public class MetricsResponse {
    private long totalRequests;
    private long successRequests;
    private long failedRequests;
    private double tps;
    private double avgResponseTime;
    private double recentAvgResponseTime;

    // Getters and Setters
    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getSuccessRequests() {
        return successRequests;
    }

    public void setSuccessRequests(long successRequests) {
        this.successRequests = successRequests;
    }

    public long getFailedRequests() {
        return failedRequests;
    }

    public void setFailedRequests(long failedRequests) {
        this.failedRequests = failedRequests;
    }

    public double getTps() {
        return tps;
    }

    public void setTps(double tps) {
        this.tps = tps;
    }

    public double getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(double avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public double getRecentAvgResponseTime() {
        return recentAvgResponseTime;
    }

    public void setRecentAvgResponseTime(double recentAvgResponseTime) {
        this.recentAvgResponseTime = recentAvgResponseTime;
    }
}
