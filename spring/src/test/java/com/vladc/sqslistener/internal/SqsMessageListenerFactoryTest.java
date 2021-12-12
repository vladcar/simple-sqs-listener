package com.vladc.sqslistener.internal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import com.vladc.sqslistener.MessageListener;
import com.vladc.sqslistener.SqsMessageListenerFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.services.sqs.SqsClient;

@ExtendWith(MockitoExtension.class)
class SqsMessageListenerFactoryTest {

  @Mock(stubOnly = true)
  private SqsClient mockSqsClient;

  @Mock(stubOnly = true)
  private SqsQueueAttributes queue;

  @Test
  void createListener(@Mock ThreadPoolTaskExecutor mockExecutor) {

    SqsMessageListenerFactory underTest =
        new DefaultSqsMessageListenerFactory(mockSqsClient, mockExecutor, null);

    when(mockExecutor.getThreadPoolExecutor())
        .thenReturn((ThreadPoolExecutor) Executors.newCachedThreadPool());

    MessageListener listener = underTest.createListener(queue);
    assertThat(listener).isNotNull();
  }

  @Test
  void createListenerNullDependencies() {
    SqsMessageListenerFactory underTest =
        new DefaultSqsMessageListenerFactory(mockSqsClient, null, null);

    MessageListener listener = underTest.createListener(queue);
    assertThat(listener).isNotNull();
  }
}
