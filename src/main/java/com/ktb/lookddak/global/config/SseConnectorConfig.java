package com.ktb.lookddak.global.config;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.connector.Connector;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SseConnectorProperties.class)
public class SseConnectorConfig {

    private final SseConnectorProperties properties;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> sseConnectorCustomizer() {
        return factory -> {
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(properties.getSsePort());
            factory.addAdditionalConnectors(connector);
        };
    }
}
