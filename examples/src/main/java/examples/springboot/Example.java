package examples.springboot;

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
