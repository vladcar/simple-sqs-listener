package com.vladc.sqslistener;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "messaging.sqs")
public class MessageListenerProperties {

  private boolean enabled = true;
  private int poolSize = 10;
  private int poolKeepAlive = 120;
  private String poolThreadPrefix = "sqsListener-";
}
