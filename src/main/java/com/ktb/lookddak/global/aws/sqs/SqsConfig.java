package com.ktb.lookddak.global.aws.sqs;

import com.ktb.lookddak.domain.fitting.mq.outbox.FittingOutboxProperties;
import com.ktb.lookddak.domain.fitting.mq.FittingResultConsumerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@EnableConfigurationProperties({
        SqsProperties.class,
        FittingOutboxProperties.class,
        FittingResultConsumerProperties.class
})
public class SqsConfig {

    @Bean
    public SqsClient sqsClient(SqsProperties properties) {
        return SqsClient.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }
}
