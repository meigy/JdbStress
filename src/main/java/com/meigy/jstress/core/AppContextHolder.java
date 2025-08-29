package com.meigy.jstress.core;

import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @Author Meigy
 * @Description 对象还没有描述
 * @Date 2025-08-28 17:41
 **/
@Component("appContextHolder")
//@Order(Ordered.HIGHEST_PRECEDENCE)
public class AppContextHolder implements ApplicationContextAware {
    private static org.springframework.context.ApplicationContext context;
    public AppContextHolder() {
    }

    @Override
    public void setApplicationContext(org.springframework.context.ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static org.springframework.context.ApplicationContext getContext() {
        return context;
    }

    public static <T> T getBean(Class<T> requiredType) {
        return context.getBean(requiredType);
    }
}
