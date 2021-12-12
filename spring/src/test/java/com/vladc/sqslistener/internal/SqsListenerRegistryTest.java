package com.vladc.sqslistener.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladc.sqslistener.AnnotationSqsMessageListenerAutoConfiguration;
import com.vladc.sqslistener.SqsListenerRegistry;
import com.vladc.sqslistener.SqsMessageListener;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ConfigurableApplicationContext;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@SpringBootTest(
    classes = {MessagingTestConfig.class, AnnotationSqsMessageListenerAutoConfiguration.class})
class SqsListenerRegistryTest {

  @SpyBean @Autowired private SqsListenerRegistry underTest;
  @Autowired private ConfigurableApplicationContext applicationContext;
  @MockBean private SqsClient mockSqsClient;
  @Captor private ArgumentCaptor<SqsMessageListener> listenerArgumentCaptor;

  @Test
  void registerListener() {
    when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
        .thenReturn(ReceiveMessageResponse.builder().messages(List.of()).build());
    verify(underTest).registerListener(listenerArgumentCaptor.capture());
    SqsMessageListener listener = listenerArgumentCaptor.getValue();
    assertThat(applicationContext.getBean("sqsListener1")).isEqualTo(listener);
  }
}
