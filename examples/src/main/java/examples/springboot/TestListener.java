package examples.springboot;

import com.vladc.sqslistener.annotation.SqsMessageHandler;
import com.vladc.sqslistener.annotation.SqsMessageHandler.AckMode;
import com.vladc.sqslistener.annotation.SqsMessageHandler.PollMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

@Component
public class TestListener {

  private static final Logger log = LoggerFactory.getLogger(TestListener.class);

  @SqsMessageHandler(
      queueUrl = "${my-sqs-queue.url}",
      executor = "#{@sqsListenerExec}",
      exceptionHandler = "#{@myErrorHandler}",
      concurrency = "${my-sqs-queue.concurrency}",
      visibilityTimeout = "${my-sqs-queue.visibilityTimeoutSeconds}",
      pollMode = PollMode.LONG,
      ackMode = AckMode.AUTO)
  public void handleMessage(Message message) {
    log.info("got message: {}", message);

    if (message.body().contains("something bad")) {
      throw new RuntimeException("oops");
    }
  }
}
