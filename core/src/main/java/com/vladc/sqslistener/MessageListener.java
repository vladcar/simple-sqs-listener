package com.vladc.sqslistener;

import java.util.concurrent.TimeUnit;

public interface MessageListener {

  void subscribe();

  void destroy();

  void awaitTermination(long timeout, TimeUnit timeUnit) throws InterruptedException;
}
