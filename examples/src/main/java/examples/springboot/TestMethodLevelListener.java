package examples.springboot;

import com.vladc.sqslistener.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

@Slf4j
@Component
public class TestMethodLevelListener {

  @SqsListener(url = "https://test-queue.com")
  void listen(Message message) {
    log.info("method level listener invoked");
  }
}
