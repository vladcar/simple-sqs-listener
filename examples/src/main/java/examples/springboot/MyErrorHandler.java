package examples.springboot;

import com.vladc.sqslistener.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;

@Component
public class MyErrorHandler implements ErrorHandler {

  private static final Logger log = LoggerFactory.getLogger(MyErrorHandler.class);

  private final String queueUrl;
  private final SqsClient sqsClient;

  public MyErrorHandler(@Value("${my-sqs-queue.url}") String queueUrl,
      SqsClient sqsClient) {
    this.queueUrl = queueUrl;
    this.sqsClient = sqsClient;
  }

  @Override
  public void onError(Message message, Exception exception) {
    log.error("oops", exception);

    // handle exception

    // acknowledge
    sqsClient.deleteMessage(DeleteMessageRequest.builder()
        .queueUrl(queueUrl)
        .receiptHandle(message.receiptHandle())
        .build());
  }
}
