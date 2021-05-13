package examples.springboot;

import com.vladc.sqslistener.annotation.SqsMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

@Component
public class TestListener {

  private static final Logger log = LoggerFactory.getLogger(TestListener.class);

  @SqsMessageHandler(queue = "#{@myQueue}", executor = "#{@sqsListenerExec}", exceptionHandler = "#{@myErrorHandler}", concurrency = 2)
  public void handleMessage(Message message) {
    log.info("got message: {}", message);

    if (message.body().contains("something bad")) {
      throw new RuntimeException("oops");
    }
  }
}
