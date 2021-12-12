package com.vladc.sqslistener.internal;

import com.vladc.sqslistener.ErrorHandler;
import software.amazon.awssdk.services.sqs.model.Message;

public class TestErrorHandler implements ErrorHandler {

  @Override
  public void onError(Message message, Exception exception) {}
}
