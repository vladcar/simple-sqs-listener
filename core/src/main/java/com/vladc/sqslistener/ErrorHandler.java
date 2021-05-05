package com.vladc.sqslistener;

import software.amazon.awssdk.services.sqs.model.Message;

public interface ErrorHandler {

  void onError(Message message, Exception exception);
}
