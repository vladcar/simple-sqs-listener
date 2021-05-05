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
    .build();

 SqsQueueMessageListener listener = new SqsQueueMessageListener(sqsClient);
 listener.setQueue(queue);
 listener.setConcurrentConsumers(3);
 listener.setMessageProcessorPoolSize(31);
 
 listener.initialize();
```

#### Spring Boot configuration

work in progress