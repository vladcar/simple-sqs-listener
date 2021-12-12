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
