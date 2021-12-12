package examples.springboot;

import com.vladc.sqslistener.HandlerInterceptor;
import com.vladc.sqslistener.annotation.EnableSqs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
@EnableSqs
@Configuration
public class Config {

  @Bean(name = "sqsListenerExec")
  public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(21);
    executor.setMaxPoolSize(21);
    executor.setAllowCoreThreadTimeOut(true);
    executor.setKeepAliveSeconds(120);
    executor.setThreadNamePrefix("sqsListener-");
    return executor;
  }

  @Bean
  public HandlerInterceptor loggingFilter() {
    return message -> {
      log.info("processing message: {}", message.messageId());
      log.info("filter invoked");
    };
  }

  @Bean
  public SqsClient sqsClient() {
    return SqsClient.builder()
        .build();
  }
}
