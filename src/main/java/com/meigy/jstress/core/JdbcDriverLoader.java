package com.meigy.jstress.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JdbcDriverLoader {

    @Value("${jdbc.driver.path}")
    private String jdbcDriverPath;

    @PostConstruct
    public void loadDrivers() {
        List<String> loadedDrivers = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(jdbcDriverPath);
            
            if (resources.length == 0) {
                log.info("未找到JDBC驱动: {}", jdbcDriverPath);
                return;
            }

            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            java.lang.reflect.Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);

            for (Resource resource : resources) {
                try {
                    method.invoke(classLoader, resource.getURL());
                    loadedDrivers.add(resource.getFilename());
                    log.info("加载JDBC驱动: {}", resource.getFilename());
                } catch (Exception e) {
                    log.error("加载JDBC驱动失败: " + resource.getFilename(), e);
                }
            }
        } catch (Exception e) {
            log.error("初始化JDBC驱动加载器失败", e);
        }

        if (!loadedDrivers.isEmpty()) {
            log.info("成功加载 {} 个JDBC驱动: {}", loadedDrivers.size(), loadedDrivers);
        }
    }
} 