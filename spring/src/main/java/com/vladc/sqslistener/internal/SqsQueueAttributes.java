package com.vladc.sqslistener.internal;

import com.vladc.sqslistener.ErrorHandler;
import com.vladc.sqslistener.MessageHandler;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SqsQueueAttributes {

  private final String url;
  private final Integer maxBatchSize;
  private final Integer visibilityTimeoutSeconds;
  private final MessageHandler handler;
  private final ErrorHandler errorHandler;
  private final Boolean longPolling;
  private final Boolean autoAcknowledge;
  private final Integer concurrency;
}
