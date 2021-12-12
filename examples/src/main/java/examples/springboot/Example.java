package examples.springboot;

import com.vladc.sqslistener.MessageListener;
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
