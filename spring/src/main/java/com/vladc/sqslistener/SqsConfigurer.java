package com.vladc.sqslistener;

import com.vladc.sqslistener.SqsQueue.SqsQueueBuilder;

@Deprecated
public interface SqsConfigurer {

  void configure(SqsQueueBuilder builder);
}
