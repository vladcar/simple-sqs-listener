package com.vladc.sqslistener;

import com.vladc.sqslistener.annotation.EnableSqs;
import com.vladc.sqslistener.internal.DefaultSqsListenerRegistry;
import com.vladc.sqslistener.internal.DefaultSqsMessageListenerFactory;
import com.vladc.sqslistener.internal.MessageListenerAnnotatedMethodBeanPostProcessor;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableSqs.class)
@EnableConfigurationProperties(MessageListenerProperties.class)
@ConditionalOnProperty(
    prefix = "messaging.sqs",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AnnotationSqsMessageListenerAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public MessageListenerAnnotatedMethodBeanPostProcessor
      messageListenerAnnotatedMethodBeanPostProcessor(
          SqsMessageListenerFactory factory, SqsListenerRegistry registry) {
    return new MessageListenerAnnotatedMethodBeanPostProcessor(factory, registry);
  }

  @Bean
  @ConditionalOnMissingBean
  public SqsMessageListenerFactory sqsMessageListenerFactory(
      SqsClient sqsClient,
      ThreadPoolTaskExecutor messageProcessorExecutor,
      List<HandlerInterceptor> filters) {
    return new DefaultSqsMessageListenerFactory(sqsClient, messageProcessorExecutor, filters);
  }

  @Bean
  @ConditionalOnMissingBean
  public SqsListenerRegistry sqsListenerRegistry() {
    return new DefaultSqsListenerRegistry();
  }

  @Bean(name = "messageProcessorExecutor")
  @ConditionalOnMissingBean
  public ThreadPoolTaskExecutor messageProcessorExecutor(MessageListenerProperties properties) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(properties.getPoolSize());
    executor.setMaxPoolSize(properties.getPoolSize());
    executor.setKeepAliveSeconds(properties.getPoolKeepAlive());
    executor.setThreadNamePrefix(properties.getPoolThreadPrefix());
    executor.setAllowCoreThreadTimeOut(true);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    return executor;
  }

  @Bean
  @ConditionalOnMissingBean
  public SqsClient sqsClient() {
    return SqsClient.create();
  }
}
