package com.vladc.sqslistener;

import software.amazon.awssdk.services.sqs.model.Message;

public interface MessageHandler {

  void handle(Message message);
}
