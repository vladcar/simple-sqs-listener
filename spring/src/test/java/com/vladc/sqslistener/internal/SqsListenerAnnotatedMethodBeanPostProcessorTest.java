package com.vladc.sqslistener.internal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

import com.vladc.sqslistener.AnnotationSqsMessageListenerAutoConfiguration;
import com.vladc.sqslistener.SqsListenerRegistry;
import com.vladc.sqslistener.SqsMessageListener;
import com.vladc.sqslistener.SqsMessageListenerFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.services.sqs.SqsClient;

@SpringBootTest(
    classes = {MessagingTestConfig.class, AnnotationSqsMessageListenerAutoConfiguration.class})
class SqsListenerAnnotatedMethodBeanPostProcessorTest {

  @Autowired private Environment env;

  @SpyBean @Autowired private SqsMessageListenerFactory messageListenerFactory;
  @MockBean private SqsListenerRegistry mockListenerRegistry;
  @MockBean private SqsClient sqsClient;

  @Captor private ArgumentCaptor<SqsMessageListener> listenerArgumentCaptor;
  @Captor private ArgumentCaptor<SqsQueueAttributes> queueCaptor;

  @Test
  void testListenerRegistered() {
    verify(messageListenerFactory).createListener(queueCaptor.capture());
    verify(mockListenerRegistry).registerListener(listenerArgumentCaptor.capture());

    SqsMessageListener listener = listenerArgumentCaptor.getValue();
    assertThat(listener).isNotNull();

    SqsQueueAttributes queue = queueCaptor.getValue();
    assertThat(queue).isNotNull();
    assertThat(queue.getUrl()).isEqualTo(env.getProperty("messaging.sqs.queueUrl"));
    assertThat(queue.getConcurrency())
        .isEqualTo(env.getProperty("messaging.sqs.concurrency", Integer.class));
    assertThat(queue.getMaxBatchSize())
        .isEqualTo(env.getProperty("messaging.sqs.maxBatchSize", Integer.class));
    assertThat(queue.getVisibilityTimeoutSeconds())
        .isEqualTo(env.getProperty("messaging.sqs.visibilityTimeoutSeconds", Integer.class));
    assertThat(queue.getHandler()).isExactlyInstanceOf(MessageAttributeMappingMessageHandler.class);
    assertThat(queue.getErrorHandler()).isExactlyInstanceOf(TestErrorHandler.class);
  }
}
