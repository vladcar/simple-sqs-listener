package com.vladc.sqslistener.internal;

import com.vladc.sqslistener.ErrorHandler;
import com.vladc.sqslistener.MessageHandler;
import com.vladc.sqslistener.MessageListener;
import com.vladc.sqslistener.SqsListenerRegistry;
import com.vladc.sqslistener.SqsMessageListenerFactory;
import com.vladc.sqslistener.annotation.SqsHandler;
import com.vladc.sqslistener.annotation.SqsListener;
import com.vladc.sqslistener.annotation.SqsListener.AckMode;
import com.vladc.sqslistener.annotation.SqsListener.PollMode;
import com.vladc.sqslistener.internal.MessageAttributeMappingMessageHandler.MethodMapping;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodIntrospector.MetadataLookup;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;

public class MessageListenerAnnotatedMethodBeanPostProcessor
    implements BeanPostProcessor, BeanFactoryAware, Ordered {

  private final SqsMessageListenerFactory messageListenerFactory;
  private final SqsListenerRegistry listenerRegistry;

  private BeanExpressionContext expressionContext;
  private ConfigurableListableBeanFactory beanFactory;
  private BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

  private final Set<Class<?>> nonAnnotatedClasses =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  public MessageListenerAnnotatedMethodBeanPostProcessor(
      SqsMessageListenerFactory messageListenerFactory, SqsListenerRegistry listenerRegistry) {
    this.messageListenerFactory = messageListenerFactory;
    this.listenerRegistry = listenerRegistry;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    if (beanFactory instanceof ConfigurableListableBeanFactory) {
      ConfigurableListableBeanFactory factory = (ConfigurableListableBeanFactory) beanFactory;
      this.beanFactory = factory;
      this.resolver = factory.getBeanExpressionResolver();
      this.expressionContext = new BeanExpressionContext(factory, null);
    }
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof AopInfrastructureBean) {
      // Ignore AOP infrastructure such as scoped proxies.
      return bean;
    }

    Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
    if (!nonAnnotatedClasses.contains(targetClass)
        && AnnotationUtils.isCandidateClass(targetClass, SqsListener.class)) {

      if (targetClass.isAnnotationPresent(SqsListener.class)) {
        processMultiMethodListener(targetClass, bean);
      } else {
        processMethodLevelListener(targetClass, bean);
      }
    }
    return bean;
  }

  private void processMultiMethodListener(Class<?> targetClass, Object bean) {
    SqsListener listenerAnnotation = AnnotationUtils.getAnnotation(targetClass, SqsListener.class);
    Objects.requireNonNull(listenerAnnotation);

    Map<Method, SqsHandler> handlerMethods = getHandlerMethods(targetClass);
    if (handlerMethods.isEmpty()) {
      nonAnnotatedClasses.add(targetClass);
    } else {
      MethodMapping methodMapping = buildMethodMapping(handlerMethods, targetClass);
      MessageHandler messageHandler =
          new MessageAttributeMappingMessageHandler(bean, methodMapping);
      SqsQueueAttributes queue = createQueueAttributes(listenerAnnotation, messageHandler);

      MessageListener listener = messageListenerFactory.createListener(queue);
      listenerRegistry.registerListener(listener);
    }
  }

  private void processMethodLevelListener(Class<?> targetClass, Object bean) {
    Map<Method, SqsListener> listenerMethods = getListenerMethods(targetClass);

    if (listenerMethods.isEmpty()) {
      nonAnnotatedClasses.add(targetClass);
    } else {
      for (Entry<Method, SqsListener> entry : listenerMethods.entrySet()) {
        Method key = entry.getKey();
        SqsListener value = entry.getValue();

        SqsQueueAttributes queue =
            createQueueAttributes(value, new MethodMessageHandler(key, bean));
        MessageListener listener = messageListenerFactory.createListener(queue);
        listenerRegistry.registerListener(listener);
      }
    }
  }

  private SqsQueueAttributes createQueueAttributes(
      SqsListener listenerAnnotation, MessageHandler messageHandler) {
    return SqsQueueAttributes.builder()
        .url(resolveToString(listenerAnnotation.url()))
        .errorHandler(getErrorHandler(listenerAnnotation))
        .longPolling(PollMode.LONG.equals(listenerAnnotation.pollMode()))
        .autoAcknowledge(AckMode.AUTO.equals(listenerAnnotation.ackMode()))
        .handler(messageHandler)
        .maxBatchSize(resolveToInteger(listenerAnnotation.maxBatchSize()))
        .visibilityTimeoutSeconds(resolveToInteger(listenerAnnotation.visibilityTimeout()))
        .concurrency(resolveToInteger(listenerAnnotation.concurrency()))
        .build();
  }

  private Map<Method, SqsListener> getListenerMethods(Class<?> targetClass) {
    return MethodIntrospector.selectMethods(
        targetClass,
        (MetadataLookup<SqsListener>)
            method -> AnnotatedElementUtils.findMergedAnnotation(method, SqsListener.class));
  }

  private Map<Method, SqsHandler> getHandlerMethods(Class<?> targetClass) {
    return MethodIntrospector.selectMethods(
        targetClass,
        (MetadataLookup<SqsHandler>)
            method -> AnnotatedElementUtils.findMergedAnnotation(method, SqsHandler.class));
  }

  private MethodMapping buildMethodMapping(
      Map<Method, SqsHandler> handlerMethods, Class<?> target) {
    Map<String, Method> methodMap = new HashMap<>(handlerMethods.size());
    Method defaultMethod = null;

    for (Entry<Method, SqsHandler> entry : handlerMethods.entrySet()) {
      Method method = AopUtils.selectInvocableMethod(entry.getKey(), target);
      SqsHandler handlerAnnotation = entry.getValue();

      if (handlerAnnotation.messageType().isBlank() && !handlerAnnotation.isDefault()) {
        throw new IllegalArgumentException(
            "non-default handler method messageType can not be blank");
      }
      if (handlerAnnotation.isDefault()) {
        defaultMethod = method;
      }
      if (!handlerAnnotation.messageType().isBlank()) {
        methodMap.put(handlerAnnotation.messageType(), method);
      }
    }
    return new MethodMapping(Map.copyOf(methodMap), defaultMethod);
  }

  private String resolveToString(String value) {
    Object resolved = resolveSpEl(value);
    if (resolved instanceof String) {
      return (String) resolved;
    } else {
      throw new IllegalArgumentException("Unable to evaluate %s as String".formatted(value));
    }
  }

  private Integer resolveToInteger(String value) {
    Object resolved = resolveSpEl(value);
    return Integer.valueOf((String) resolved);
  }

  private Object resolveSpEl(String value) {
    String resolved = this.beanFactory.resolveEmbeddedValue(value);
    return resolver.evaluate(resolved, this.expressionContext);
  }

  private ErrorHandler getErrorHandler(SqsListener listener) {
    if (listener.exceptionHandler().isEmpty()) {
      return null;
    }
    try {
      return (ErrorHandler) resolver.evaluate(listener.exceptionHandler(), this.expressionContext);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to register error handler bean", e);
    }
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
