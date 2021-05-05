package com.vladc.sqslistener;

import com.vladc.sqslistener.annotation.SqsMessageListenerHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodIntrospector.MetadataLookup;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

public class MessageListenerAnnotatedMethodBeanPostProcessor implements BeanPostProcessor, Ordered {

  private static final Logger log = LoggerFactory.getLogger(MessageListenerAnnotatedMethodBeanPostProcessor.class);

  private final SqsMessageListenerManager sqsMessageListenerManager;
  private final SqsClient sqsClient;

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
    if (AnnotationUtils.isCandidateClass(targetClass, SqsMessageListenerHandler.class)) {

      Map<Method, SqsMessageListenerHandler> annotatedMethods = getHandlerMethods(targetClass);

      annotatedMethods.forEach((method, handlerAnnotation) -> {
        String queueUrl = getQueueUrl(handlerAnnotation);
        ReflectiveMessageHandler handler = new ReflectiveMessageHandler(method, targetClass);

        SqsQueue queue = SqsQueue.builder()
            .url(queueUrl)
            .maxBatchSize(handlerAnnotation.maxBatchSize())
            .visibilityTimeoutSeconds(handlerAnnotation.visibilityTimeout())
            .longPolling(handlerAnnotation.longPolling())
            .messageHandler(handler)
            .autoAcknowledge(handlerAnnotation.autoAcknowledge())
            .build();

        SqsMessageListener listener = new SqsMessageListener(sqsClient);
        listener.setQueue(queue);
        listener.setConcurrentConsumers(handlerAnnotation.concurrentConsumers());
        listener.setMessageProcessorPoolSize(handlerAnnotation.messageProcessorPoolSize());
        sqsMessageListenerManager.registerListener(handlerAnnotation.queueName(), listener);
      });
    }
    return bean;
  }

  private Map<Method, SqsMessageListenerHandler> getHandlerMethods(Class<?> targetClass) {
    return MethodIntrospector.selectMethods(
        targetClass, (MetadataLookup<SqsMessageListenerHandler>) method ->
            AnnotatedElementUtils.findMergedAnnotation(method, SqsMessageListenerHandler.class));
  }

  private String getQueueUrl(SqsMessageListenerHandler handler) {
    try {
      return sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
          .queueName(handler.queueName())
          .build()).queueUrl();
    } catch (QueueDoesNotExistException e) {
      log.error("Queue {} not found", handler.queueName(), e);
      throw new IllegalArgumentException(e);
    }
  }

  private static class ReflectiveMessageHandler implements MessageHandler {

    private final Method method;
    private final Object delegate;

    private ReflectiveMessageHandler(Method method, Object delegate) {
      this.method = method;
      this.delegate = delegate;
    }

    @Override
    public void handle(Message message) {
      try {
        method.invoke(delegate, message);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
