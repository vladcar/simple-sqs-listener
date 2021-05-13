package com.vladc.sqslistener;

import com.vladc.sqslistener.SqsQueue.SqsQueueBuilder;
import com.vladc.sqslistener.annotation.SqsMessageHandler;
import com.vladc.sqslistener.annotation.SqsMessageHandler.AckMode;
import com.vladc.sqslistener.annotation.SqsMessageHandler.PollMode;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

public class MessageListenerAnnotatedMethodBeanPostProcessor implements BeanPostProcessor,
    BeanFactoryAware, Ordered {

  private static final Logger log = LoggerFactory
      .getLogger(MessageListenerAnnotatedMethodBeanPostProcessor.class);

  private final SqsMessageListenerManager sqsMessageListenerManager;
  private final SqsClient sqsClient;

  private final Set<Class<?>> nonAnnotatedClasses = Collections
      .newSetFromMap(new ConcurrentHashMap<>());

  private BeanExpressionContext expressionContext;
  private BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

  public MessageListenerAnnotatedMethodBeanPostProcessor(
      SqsMessageListenerManager sqsMessageListenerManager,
      SqsClient sqsClient) {
    this.sqsMessageListenerManager = sqsMessageListenerManager;
    this.sqsClient = sqsClient;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof AopInfrastructureBean) {
      // Ignore AOP infrastructure such as scoped proxies.
      return bean;
    }

    Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
    if (!nonAnnotatedClasses.contains(targetClass) &&
        AnnotationUtils.isCandidateClass(targetClass, SqsMessageHandler.class)) {

      Map<Method, SqsMessageHandler> annotatedMethods = getHandlerMethods(targetClass);
      if (annotatedMethods.isEmpty()) {
        nonAnnotatedClasses.add(targetClass);
      } else {
        annotatedMethods.forEach((method, handlerAnnotation) ->
            processListenerMethods(handlerAnnotation, method, bean));

        if (log.isDebugEnabled()) {
          log.debug("@SqsMessageHandler methods processed on bean {} : {}", beanName,
              annotatedMethods);
        }
      }
    }
    return bean;
  }

  private void processListenerMethods(SqsMessageHandler handlerAnnotation, Method method,
      Object bean) {

    Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
    Optional<SqsQueue> queueOpt = getQueue(handlerAnnotation);

    SqsQueueBuilder queueBuilder = SqsQueue.builder()
        .url(queueOpt.map(SqsQueue::getUrl)
            .orElseGet(() -> getQueueUrl(handlerAnnotation)))
        .maxBatchSize(queueOpt.map(SqsQueue::getMaxBatchSize)
            .orElseGet(handlerAnnotation::maxBatchSize))
        .visibilityTimeoutSeconds(queueOpt.map(SqsQueue::getVisibilityTimeoutSeconds)
            .orElseGet(handlerAnnotation::visibilityTimeout))
        .longPolling(queueOpt.map(SqsQueue::isLongPolling)
            .orElse(handlerAnnotation.pollMode().equals(PollMode.LONG)))
        .autoAcknowledge(queueOpt.map(SqsQueue::isAutoAcknowledge)
            .orElse(handlerAnnotation.ackMode().equals(AckMode.AUTO)))
        .messageHandler(queueOpt.map(SqsQueue::getMessageHandler)
            .orElseGet(() -> new ReflectiveMessageHandler(invocableMethod, bean)))
        .errorHandler(queueOpt.map(SqsQueue::getErrorHandler)
            .orElseGet(() -> getErrorHandler(handlerAnnotation)));

    Optional<SqsConfigurer> configurer = getConfigurer(handlerAnnotation);
    configurer.ifPresent(c -> c.configure(queueBuilder));

    SqsMessageListener listener = new SqsMessageListener(sqsClient);
    listener.setQueue(queueBuilder.build());
    listener.setConsumerCount(handlerAnnotation.concurrency());
    resolveTaskExecutor(handlerAnnotation, listener);
    sqsMessageListenerManager.registerListener(handlerAnnotation.queueName(), listener);
  }

  private Map<Method, SqsMessageHandler> getHandlerMethods(Class<?> targetClass) {
    return MethodIntrospector.selectMethods(
        targetClass, (MetadataLookup<SqsMessageHandler>) method ->
            AnnotatedElementUtils.findMergedAnnotation(method, SqsMessageHandler.class));
  }

  private String getQueueUrl(SqsMessageHandler handler) {
    if (handler.queueName().isEmpty()) {
      return "";
    }
    try {
      return sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
          .queueName(handler.queueName())
          .build()).queueUrl();
    } catch (QueueDoesNotExistException e) {
      log.error("Queue {} not found", handler.queueName(), e);
      throw new IllegalArgumentException(e);
    }
  }

  private void resolveTaskExecutor(SqsMessageHandler handler, SqsMessageListener listener) {
    if (handler.executor().isEmpty()) {
      return;
    }
    TaskExecutor taskExecutor = (TaskExecutor) resolver
        .evaluate(handler.executor(), this.expressionContext);
    if (taskExecutor == null) {
      return;
    }
    if (!(taskExecutor instanceof ThreadPoolTaskExecutor)) {
      throw new IllegalArgumentException(
          "Only instances of ThreadPoolTaskExecutor are supported by @SqsMessageHandler#executor");
    }
    listener.setTaskExecutor(((ThreadPoolTaskExecutor) taskExecutor).getThreadPoolExecutor());
  }

  private Optional<SqsConfigurer> getConfigurer(SqsMessageHandler handler) {
    if (handler.config().isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable((SqsConfigurer) resolver
          .evaluate(handler.config(), this.expressionContext));
    } catch (BeansException e) {
      throw new IllegalStateException("Failed to register SqsQueue bean", e);
    }
  }

  private Optional<SqsQueue> getQueue(SqsMessageHandler handler) {
    if (handler.queue().isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable((SqsQueue) resolver
          .evaluate(handler.queue(), this.expressionContext));
    } catch (BeansException e) {
      throw new IllegalStateException("Failed to register SqsQueue bean", e);
    }
  }

  private ErrorHandler getErrorHandler(SqsMessageHandler handler) {
    if (handler.exceptionHandler().isEmpty()) {
      return null;
    }
    try {
      return (ErrorHandler) resolver
          .evaluate(handler.exceptionHandler(), this.expressionContext);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to register error handler bean", e);
    }
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    if (beanFactory instanceof ConfigurableListableBeanFactory) {
      this.resolver = ((ConfigurableListableBeanFactory) beanFactory).getBeanExpressionResolver();
      this.expressionContext = new BeanExpressionContext(
          (ConfigurableListableBeanFactory) beanFactory, null);
    }
  }
}
