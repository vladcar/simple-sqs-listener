package com.vladc.sqslistener;

import com.vladc.sqslistener.SqsQueue.SqsQueueBuilder;

public interface SqsConfigurer {

  void configure(SqsQueueBuilder builder);
}
