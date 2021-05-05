package com.vladc.sqslistener;

import static java.lang.String.format;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ConfigurableThreadFactory implements ThreadFactory {

  private final AtomicInteger threadNumber = new AtomicInteger(1);

  private final String namePrefix;
  private final String name;
  private final boolean daemon;

  private ConfigurableThreadFactory(String prefix, String name, boolean daemon) {
    this.namePrefix = prefix;
    this.name = name;
    this.daemon = daemon;
  }

  public static ConfigurableThreadFactory newDaemon(String prefix) {
    return new ConfigurableThreadFactory(prefix, null, true);
  }

  public static ConfigurableThreadFactory newFactory(String prefix) {
    return new ConfigurableThreadFactory(prefix, null, false);
  }

  public static ConfigurableThreadFactory newWithName(String name) {
    return new ConfigurableThreadFactory(null, name, false);
  }

  public static ConfigurableThreadFactory newDaemonWithName(String name) {
    return new ConfigurableThreadFactory(null, name, true);
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread thread = new Thread(r);
    thread.setDaemon(daemon);

    thread.setName(name != null ? name
        : format("%s-thread-%d", namePrefix, threadNumber.getAndIncrement()));

    return thread;
  }
}