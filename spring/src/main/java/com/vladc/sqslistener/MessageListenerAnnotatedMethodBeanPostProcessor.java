package com.vladc.sqslistener;

import com.vladc.sqslistener.annotation.SqsMessageHandler;
import com.vladc.sqslistener.annotation.SqsMessageHandler.AckMode;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
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

    String queueUrl = getQueueUrl(handlerAnnotation);
    Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
    ReflectiveMessageHandler handler = new ReflectiveMessageHandler(invocableMethod, bean);

    SqsQueue queue = SqsQueue.builder()
        .url(queueUrl)
        .maxBatchSize(handlerAnnotation.maxBatchSize())
        .visibilityTimeoutSeconds(handlerAnnotation.visibilityTimeout())
        .longPolling(handlerAnnotation.longPolling())
        .messageHandler(handler)
        .autoAcknowledge(handlerAnnotation.ackMode().equals(AckMode.AUTO))
        .build();

    SqsMessageListener listener = new SqsMessageListener(sqsClient);
    listener.setQueue(queue);
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