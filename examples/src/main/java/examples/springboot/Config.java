package examples.springboot;

import com.vladc.sqslistener.SqsQueue;
import com.vladc.sqslistener.annotation.EnableSqs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.services.sqs.SqsClient;

@EnableSqs
@Configuration
public class Config {

  @Value("${my-sqs-queue.url}")
  private String queueUrl;

  @Bean(name = "myQueue")
  public SqsQueue queue() {
    return SqsQueue.builder()
        .url(queueUrl)
        .maxBatchSize(10)
        .visibilityTimeoutSeconds(120)
        .build();
  }

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
  public SqsClient sqsClient() {
    return SqsClient.builder()
        .build();
  }
}
