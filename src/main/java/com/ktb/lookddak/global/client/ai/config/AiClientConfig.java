package com.ktb.lookddak.global.client.ai.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(AiClientProperties.class)
public class AiClientConfig {

    @Bean
    @Qualifier("aiWebClient")
    public WebClient aiWebClient(
            WebClient.Builder builder,
            AiClientProperties properties
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.toIntExact(properties.getConnectTimeout().toMillis())
                )
                .responseTimeout(properties.getResponseTimeout());

        WebClient.Builder aiWebClientBuilder = builder
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        if (StringUtils.hasText(properties.getApiKey())) {
            aiWebClientBuilder.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + properties.getApiKey()
            );
        }

        return aiWebClientBuilder.build();
    }
}
