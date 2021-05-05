package com.vladc.sqslistener.autoconfigure;

import com.vladc.sqslistener.MessageListenerAnnotatedMethodBeanPostProcessor;
import com.vladc.sqslistener.SqsMessageListenerManager;
import com.vladc.sqslistener.annotation.EnableSqs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@EnableConfigurationProperties(MessageListenerProperties.class)
@ConditionalOnClass(EnableSqs.class)
public class AnnotationSqsMessageListenerAutoConfiguration {

  @Bean
  public MessageListenerAnnotatedMethodBeanPostProcessor messageListenerAnnotatedMethodBeanPostProcessor(
      SqsMessageListenerManager sqsMessageListenerManager, SqsClient sqsClient) {
    return new MessageListenerAnnotatedMethodBeanPostProcessor(
        sqsMessageListenerManager, sqsClient);
  }

  @Bean
  public SqsMessageListenerManager sqsMessageListenerManager() {
    return new SqsMessageListenerManager();
  }

  @Bean
  @ConditionalOnMissingBean
  public SqsClient sqsClient() {
    return SqsClient.create();
  }
}
