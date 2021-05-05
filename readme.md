# Simple AWS SQS listener

It is very easy to get started with AWS SQS. However, official documentation does not offer production-ready examples of efficient polling mechanisms.
Unlike other messaging tools, SQS requires you to write your own polling code using official SDK. Making it efficient, scalable and multithreaded is not trivial and requires a lot of boilerplate code.

This library uses official AWS SDK (v2) and abstracts away polling from the queue with fluent configuration interface.

### Features

- One SQS queue per listener model
- Multiple concurrent consumers per listener
- AutoAcknowledge mode - auto delete message if `com.vladc.sqslistener.MessageHandler.handle` returns successfully
- Long/Short polling
- Concurrent message processing. You can provide your own thread pool or use default (see `com.vladc.sqslistener.SqsMessageListener.defaultMessageProcessorExecutor`)


### Example usage

#### Plain Java

```java
 SqsClient sqsClient = SqsClient.create();

 SqsQueue queue = SqsQueue.builder()
    .url("https://my-queue-url.com")
    .maxBatchSize(10)
    .visibilityTimeoutSeconds(90)
    .longPolling(true)
    .autoAcknowledge(true)
    .messageHandler((meessage) -> {
      // handle message
    })
    .errorHandler((message, exception) -> {
      // handle error
    })
    .build();

 SqsQueueMessageListener listener = new SqsQueueMessageListener(sqsClient);
 listener.setQueue(queue);
 listener.setConcurrentConsumers(3);
 listener.setMessageProcessorPoolSize(31);
 
 listener.initialize();
```

#### Spring Boot configuration

##### Prerequisites

- `simple-sqs-listener-spring-boot` must be present on classpath
- Spring Boot 2.4.0
- AWS SDK `SqsClient` must be configured as bean

```java
package com.example;

import com.vladc.sqslistener.annotation.EnableSqs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@EnableSqs
@Configuration
public class Config {

  @Bean
  public SqsClient sqsClient() {
    return SqsClient.builder()
        .region(Region.EU_CENTRAL_1)
        .credentialsProvider(ProfileCredentialsProvider.create("my-profile"))
        .build();
  }
}
```
```java
package com.example;

import com.vladc.sqslistener.annotation.SqsMessageListenerHandler;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

@Component
public class TestHandler {

  @SqsMessageListenerHandler(queue = "tst-queue", messageProcessorPoolSize = 21, concurrentConsumers = 2)
  public void handleMessage(Message message) {
    System.out.println(message);
  }
}
```

