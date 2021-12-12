package com.vladc.sqslistener;

public interface SqsListenerRegistry {

  void registerListener(MessageListener listener);
}
