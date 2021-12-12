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
import software.amazon.awssdk.services.sqs.SqsClient;

public class Example {

  public static void main(String[] args) {
    SqsClient sqsClient = SqsClient.create();

    MessageListener listener =
        SqsMessageListener.builder()
            .client(sqsClient)
            .url("https://my-queue-url.com")
            .maxBatchSize(10)
            .visibilityTimeoutSeconds(90)
            .longPolling(true)
            .autoAcknowledge(true)
            .handler((message) -> {
              // handle message
            })
            .errorHandler((message, exception) -> {
              // handle error
            })
            .build();

    listener.subscribe();
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
package examples.springboot;

import com.vladc.sqslistener.annotation.SqsHandler;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

@Component
public class TestHandler {

  @SqsListener(
      queueUrl = "${my-sqs-queue.url}",
      executor = "#{@sqsListenerExec}",
      concurrency = "${my-sqs-queue.concurrency}")
  public void handleMessage(Message message) {
    System.out.println(message);
  }
}
```

##### MessageAttribute-based method mapping
Opinionated approach designed for use-cases where it is more convenient to map messages coming from the queue to a set of methods rather than method per queue.

In order for this to work, producers must include special `messageType` attribute when sending message. This allows to map `@SqsHandler` methods to a specific type of messages within single SQS queue.

- Place `@SqsListener` on class level
- Place `@SqsHandler` annotation on method level

```java
package examples.springboot;

import com.vladc.sqslistener.annotation.SqsHandler;
import com.vladc.sqslistener.annotation.SqsListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

@SqsListener(url = "${my-sqs-queue.url}")
@Component
public class TestListener {

  @SqsHandler(messageType = "someType")
  public void handleSomeType(Message message) {
    
  }
  
  @SqsHandler(messageType = "someType2")
  public void handleSomeType2(Message message) {
    
  }
}
```

Complete spring-boot configuration in [examples](./examples/src/main/java/examples/springboot)
