package com.vladc.sqslistener.autoconfigure;

import com.vladc.sqslistener.SqsQueue;
import com.vladc.sqslistener.annotation.SqsMessageHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "messaging.sqs")
public class MessageListenerProperties {

  /**
   * Enable/disable messaging functionality
   */
  private boolean enabled = true;

  /**
   * Default number of concurrent consumers per {@linkplain SqsQueue}. Can be overridden by
   * specifying {@linkplain SqsMessageHandler#concurrency()}
   */
  private int consumerCount = 0;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setConsumerCount(int consumerCount) {
    this.consumerCount = consumerCount;
  }

  public int getConsumerCount() {
    return consumerCount;
  }
}
