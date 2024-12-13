package com.meigy.jstress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties
public class JstressApplication {

    public static void main(String[] args) {
        SpringApplication.run(JstressApplication.class, args);
    }

    @EventListener(WebServerInitializedEvent.class)
    public void onWebServerReady(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        log.info("\n----------------------------------------------------------\n" +
                 "\t数据库压测工具已启动! 访问: http://localhost:{}\n" +
                 "----------------------------------------------------------", port);
    }
}
