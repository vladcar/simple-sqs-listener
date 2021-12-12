package com.vladc.sqslistener.internal;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

@TestConfiguration
@PropertySource("classpath:application.properties")
public class MessagingTestConfig {

  @Bean
  public TestSqsListener testListener() {
    return new TestSqsListener();
  }

  @Bean("testErrorHandler")
  public TestErrorHandler testErrorHandler() {
    return new TestErrorHandler();
  }
}
