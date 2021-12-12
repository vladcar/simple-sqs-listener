package com.vladc.sqslistener.internal;

import com.vladc.sqslistener.MessageListener;
import com.vladc.sqslistener.SqsListenerRegistry;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;

public class DefaultSqsListenerRegistry
    implements SqsListenerRegistry, SmartLifecycle, ApplicationContextAware {

  private ConfigurableApplicationContext applicationContext;
  private final Map<String, MessageListener> listeners = new ConcurrentHashMap<>();
  private final AtomicInteger listenerCounter = new AtomicInteger(1);

  private volatile boolean running = false;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    if (applicationContext instanceof ConfigurableApplicationContext) {
      this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }
  }

  @Override
  public void start() {
    listeners.values().forEach(MessageListener::subscribe);
    this.running = true;
  }

  @Override
  public void stop() {
    this.running = false;
    listeners.values().forEach(MessageListener::destroy);

    for (MessageListener messageListener : listeners.values()) {
      try {
        messageListener.awaitTermination(20, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public boolean isRunning() {
    return this.running;
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  @Override
  public void registerListener(MessageListener listener) {
    Objects.requireNonNull(listener, "SqsQueueMessageListener must not be null");
    String id = "sqsListener%s".formatted(listenerCounter.getAndIncrement());
    if (listeners.putIfAbsent(id, listener) == null) {
      applicationContext.getBeanFactory().registerSingleton(id, listener);
    }
  }
}
