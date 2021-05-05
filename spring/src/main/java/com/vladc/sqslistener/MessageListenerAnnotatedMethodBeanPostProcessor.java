package com.vladc.sqslistener;

import com.vladc.sqslistener.annotation.SqsMessageListenerHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
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

public class MessageListenerAnnotatedMethodBeanPostProcessor implements BeanPostProcessor, Ordered {

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

      Map<Method, SqsMessageListenerHandler> annotatedMethods = MethodIntrospector.selectMethods(
          targetClass, (MetadataLookup<SqsMessageListenerHandler>) method ->
              AnnotatedElementUtils.findMergedAnnotation(method, SqsMessageListenerHandler.class));

      annotatedMethods.forEach((method, handlerAnnotation) -> {

        String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
            .queueName(handlerAnnotation.queue())
            .build()).queueUrl();

        final MessageHandler handler = (Message message) -> {
          try {
            method.invoke(bean, message);
          } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
          }
        };

        SqsQueue queue = SqsQueue.builder()
            .url(queueUrl)
            .maxBatchSize(handlerAnnotation.maxBatchSize())
            .visibilityTimeoutSeconds(handlerAnnotation.visibilityTimeoutSeconds())
            .longPolling(handlerAnnotation.longPolling())
            .messageHandler(handler)
            .autoAcknowledge(handlerAnnotation.autoAcknowledge())
            .build();

        SqsMessageListener listener = new SqsMessageListener(sqsClient);
        listener.setQueue(queue);
        listener.setConcurrentConsumers(handlerAnnotation.concurrentConsumers());
        listener.setMessageProcessorPoolSize(handlerAnnotation.messageProcessorPoolSize());
        sqsMessageListenerManager.registerListener(handlerAnnotation.queue(), listener);
      });
    }
    return bean;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
