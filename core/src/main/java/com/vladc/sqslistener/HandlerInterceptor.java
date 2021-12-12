package com.vladc.sqslistener;

import software.amazon.awssdk.services.sqs.model.Message;

public interface HandlerInterceptor {

  void beforeHandle(Message message);
}
