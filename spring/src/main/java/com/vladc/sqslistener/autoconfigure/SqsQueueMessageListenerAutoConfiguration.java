package com.vladc.sqslistener.autoconfigure;

import com.vladc.sqslistener.SqsQueue;
import com.vladc.sqslistener.SqsMessageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@EnableConfigurationProperties(MessageListenerProperties.class)
public class SqsQueueMessageListenerAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(prefix = "messaging.sqs", name = "enabled", havingValue = "true")
  public SqsMessageListener messageListener(MessageListenerProperties properties,
      SqsClient sqsClient) {

    SqsQueue queue = SqsQueue.builder()
        .url(properties.getQueueUrl())
        .maxBatchSize(properties.getMaxBatchSize())
        .visibilityTimeoutSeconds(properties.getVisibilityTimeoutSeconds())
        .longPolling(properties.isLongPolling())
        .autoAcknowledge(properties.isAutoAcknowledge())
        .build();

    SqsMessageListener listener = new SqsMessageListener(sqsClient);
    listener.setQueue(queue);
    listener.setConcurrentConsumers(properties.getConcurrentConsumers());
    listener.setMessageProcessorPoolSize(properties.getMessageProcessorPoolSize());
    return listener;
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(prefix = "messaging.sqs", name = "enabled", havingValue = "true")
  public SqsClient sqsClient() {
    return SqsClient.create();
  }


}
