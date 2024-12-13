package com.meigy.jstress.core;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Component
@Data
public class MetricsCollector {
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();
    private final AtomicLong totalResponseTime = new AtomicLong();
    
    private volatile long lastSampleTime = System.currentTimeMillis();
    private volatile long lastSuccessCount = 0;
    private volatile long lastResponseTime = 0;
    private volatile double tps = 0.0;
    private volatile double avgResponseTime = 0.0;
    private volatile double recentAvgResponseTime = 0.0;

    public void recordSuccess(long responseTime) {
        totalRequests.increment();
        successRequests.increment();
        totalResponseTime.addAndGet(responseTime);
    }

    public void recordFailure() {
        totalRequests.increment();
        failedRequests.increment();
    }

    public void calculateMetrics(int sampleRate) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastSampleTime;
        if (timeDiff >= sampleRate * 1000) {
            long currentSuccessCount = successRequests.sum();
            long currentTotalResponseTime = totalResponseTime.get();
            long successDiff = currentSuccessCount - lastSuccessCount;
            long responseTimeDiff = currentTotalResponseTime - lastResponseTime;
            
            tps = (double) successDiff / sampleRate;
            avgResponseTime = successDiff > 0 ? 
                    (double) totalResponseTime.get() / successRequests.sum() : 0;
            recentAvgResponseTime = successDiff > 0 ?
                    (double) responseTimeDiff / successDiff : 0;

            lastSampleTime = currentTime;
            lastSuccessCount = currentSuccessCount;
            lastResponseTime = currentTotalResponseTime;
        }
    }

    public void reset() {
        totalRequests.reset();
        successRequests.reset();
        failedRequests.reset();
        totalResponseTime.set(0);
        lastSampleTime = System.currentTimeMillis();
        lastSuccessCount = 0;
        lastResponseTime = 0;
        tps = 0.0;
        avgResponseTime = 0.0;
        recentAvgResponseTime = 0.0;
    }
} 