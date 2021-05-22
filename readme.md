# Simple AWS SQS listener

![example workflow](https://github.com/vladcar/simple-sqs-listener/actions/workflows/maven.yml/badge.svg)
![Maven Publish](https://github.com/vladcar/simple-sqs-listener/actions/workflows/maven-publish.yml/badge.svg)

It is very easy to get started with AWS SQS. However, official documentation does not offer production-ready examples of efficient polling mechanisms.
Unlike other messaging tools, SQS requires you to write your own polling code using official SDK. Making it efficient, scalable and multithreaded is not trivial and requires a lot of boilerplate code.

This library uses official AWS SDK (v2) and abstracts away polling from the queue with fluent configuration interface.

### Features

- One SQS queue per listener model
- Multiple concurrent consumers per listener
- AutoAcknowledge mode - auto delete message if `com.vladc.sqslistener.MessageHandler.handle` returns successfully
- Long/Short polling
- Concurrent message processing. You can provide your own thread pool or use default (see `com.vladc.sqslistener.SqsMessageListener.defaultMessageProcessorExecutor`)

### Dependency Management
#### Maven

[maven central](https://search.maven.org/artifact/io.github.vladcar/simple-sqs-listener-bom)

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.vladcar</groupId>
      <artifactId>simple-sqs-listener-bom</artifactId>
      <version>find latest version in maven central</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Import core module for plain java usage
```xml
<dependency>
  <groupId>io.github.vladcar</groupId>
  <artifactId>simple-sqs-listener-core</artifactId>
</dependency>
```

Import this if you use Spring Boot (includes core module transitively)
```xml
<dependency>
  <groupId>io.github.vladcar</groupId>
  <artifactId>simple-sqs-listener-spring-boot</artifactId>
</dependency>
```

### Example Usage

#### Plain Java

```java
package examples;

import com.vladc.sqslistener.SqsMessageListener;
import com.vladc.sqslistener.SqsQueue;
import software.amazon.awssdk.services.sqs.SqsClient;

public class Example {

  public static void main(String[] args) {
    SqsClient sqsClient = SqsClient.create();

    SqsQueue queue = SqsQueue.builder()
        .url("https://my-queue-url.com")
        .maxBatchSize(10)
        .visibilityTimeoutSeconds(90)
        .longPolling(true)
        .autoAcknowledge(true)
        .messageHandler((message) -> {
          // handle message
        })
        .errorHandler((message, exception) -> {
          // handle error
        })
        .build();

    SqsMessageListener listener = SqsMessageListener.builder()
        .client(sqsClient)
        .queue(queue)
        .consumerCount(2)
        .autoStart(true)
        .build();
  }
}

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
import software.amazon.awssdk.services.sqs.SqsClient;

@EnableSqs
@Configuration
public class Config {

  @Bean
  public SqsClient sqsClient() {
    return SqsClient.builder()
        .build();
  }
}
```
```java
package com.example;

import com.vladc.sqslistener.annotation.SqsMessageHandler;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

@Component
public class TestHandler {

  @SqsMessageHandler(queueName = "test-queue")
  public void handleMessage(Message message) {
    System.out.println(message);
  }
}
```

Complete spring-boot configuration in [examples](./examples/src/main/java/examples/springboot)
