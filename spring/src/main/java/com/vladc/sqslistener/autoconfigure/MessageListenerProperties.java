package com.vladc.sqslistener.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "messaging.sqs")
public class MessageListenerProperties {

  private String queueUrl;

  /**
   * Message processor thread pool size
   */
  private int messageProcessorPoolSize = 11;

  /**
   * Maximum number of SQS messages held in application
   */
  private int maximumInflightMessages = 10;

  /**
   * Number of threads polling from the queue
   */
  private int concurrentConsumers = 1;

  /**
   * Maximum number of messages that can be retrieved in a single sqsClient.receive() call
   */
  private int maxBatchSize = 10;

  /**
   * SQS receive visibility timeout
   */
  private int visibilityTimeoutSeconds = 90;

  /**
   * Enable/disable long polling behaviour
   */
  private boolean longPolling = true;

  /**
   * Enable/disable messaging functionality
   */
  private boolean enabled = true;

  private boolean autoAcknowledge = true;


  public String getQueueUrl() {
    return queueUrl;
  }

  public void setQueueUrl(String queueUrl) {
    this.queueUrl = queueUrl;
  }

  public int getMessageProcessorPoolSize() {
    return messageProcessorPoolSize;
  }

  public void setMessageProcessorPoolSize(int messageProcessorPoolSize) {
    this.messageProcessorPoolSize = messageProcessorPoolSize;
  }

  public int getMaximumInflightMessages() {
    return maximumInflightMessages;
  }

  public void setMaximumInflightMessages(int maximumInflightMessages) {
    this.maximumInflightMessages = maximumInflightMessages;
  }

  public int getConcurrentConsumers() {
    return concurrentConsumers;
  }

  public void setConcurrentConsumers(int concurrentConsumers) {
    this.concurrentConsumers = concurrentConsumers;
  }

  public int getMaxBatchSize() {
    return maxBatchSize;
  }

  public void setMaxBatchSize(int maxBatchSize) {
    this.maxBatchSize = maxBatchSize;
  }

  public int getVisibilityTimeoutSeconds() {
    return visibilityTimeoutSeconds;
  }

  public void setVisibilityTimeoutSeconds(int visibilityTimeoutSeconds) {
    this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
  }

  public boolean isLongPolling() {
    return longPolling;
  }

  public void setLongPolling(boolean longPolling) {
    this.longPolling = longPolling;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isAutoAcknowledge() {
    return autoAcknowledge;
  }

  public void setAutoAcknowledge(boolean autoAcknowledge) {
    this.autoAcknowledge = autoAcknowledge;
  }
}
