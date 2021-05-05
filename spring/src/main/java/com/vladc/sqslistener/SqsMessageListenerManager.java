package com.vladc.sqslistener;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextRefreshedEvent;

public class SqsMessageListenerManager implements DisposableBean, SmartLifecycle,
    ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

  private static final Logger log = LoggerFactory.getLogger(SqsMessageListenerManager.class);

  private ConfigurableApplicationContext applicationContext;

  private boolean contextRefreshed;

  private final Map<String, SqsMessageListener> listeners = new ConcurrentHashMap<>();

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    if (applicationContext instanceof ConfigurableApplicationContext) {
      this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }
  }

  public void registerListener(String id, SqsMessageListener listener) {
    log.debug("registering listener - {}", id);

    Objects.requireNonNull(listener, "SqsMessageListener must not be null");

    synchronized (listeners) {
      if (listeners.containsKey(id)) {
        log.warn("Listener [{}] is already registered", id);
      }
      listeners.put(id, listener);
      applicationContext.getBeanFactory().registerSingleton(id, listener);
      listener.initialize();
    }
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  @Override
  public void start() {
    if (contextRefreshed) {
      listeners.values().forEach(SqsMessageListener::start);
    }
  }

  @Override
  public void stop() {
    listeners.values().forEach(SqsMessageListener::destroy);
  }

  @Override
  public boolean isRunning() {
    for (SqsMessageListener listener : listeners.values()) {
      if (listener.isRunning()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void destroy() throws Exception {
    listeners.values().forEach(listener -> {
      if (listener instanceof DisposableBean) {
        try {
          ((DisposableBean) listener).destroy();
        } catch (Exception e) {
          log.error("Failed to destroy message listener [{}]", listener, e);
        }
      }
    });
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (event.getApplicationContext().equals(this.applicationContext)) {
      this.contextRefreshed = true;
    }
  }
}
