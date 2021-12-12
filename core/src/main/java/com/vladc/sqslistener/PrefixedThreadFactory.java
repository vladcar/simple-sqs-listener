package com.vladc.sqslistener;

import static java.lang.String.format;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class PrefixedThreadFactory implements ThreadFactory {

  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private final String namePrefix;

  PrefixedThreadFactory(String prefix) {
    this.namePrefix = prefix;
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread thread = new Thread(r);
    thread.setDaemon(false);
    thread.setName(format("%s-%d", namePrefix, threadNumber.getAndIncrement()));
    return thread;
  }
}
