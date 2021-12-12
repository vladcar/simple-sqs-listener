package com.vladc.sqslistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SqsMessageListenerTest {

  private SqsMessageListener underTest;

  @Mock private SqsClient mockSqsClient;
  @Mock private MessageHandler mockHandler;
  @Captor private ArgumentCaptor<ReceiveMessageRequest> receiveRequestCaptor;

  @BeforeEach
  void setUp() {
    List<Message> testMessages =
        List.of(
            Message.builder().messageId("testId").build(),
            Message.builder().messageId("testId2").build());

    doAnswer(
            new AnswersWithDelay(
                10, i -> ReceiveMessageResponse.builder().messages(testMessages).build()))
        .when(mockSqsClient)
        .receiveMessage(any(ReceiveMessageRequest.class));
  }

  @AfterEach
  void tearDown() {
    underTest.destroy();
  }

  @Test
  void testCanPoll() {
    underTest = createListener();
    underTest.subscribe();

    verify(mockSqsClient, after(TimeUnit.MILLISECONDS.toMillis(500)).atLeastOnce())
        .receiveMessage(receiveRequestCaptor.capture());
    verify(mockHandler, atLeast(2)).handle(any());
    verify(mockSqsClient, atLeast(2)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));

    ReceiveMessageRequest receiveMessageRequest = receiveRequestCaptor.getValue();
    assertThat(receiveMessageRequest.queueUrl()).isEqualTo(underTest.getQueue().getUrl());
    assertThat(receiveMessageRequest.maxNumberOfMessages())
        .isEqualTo(underTest.getQueue().getMaxBatchSize());
    assertThat(receiveMessageRequest.waitTimeSeconds())
        .isEqualTo(underTest.getQueue().getLongPolling() ? 20 : 1);
    assertThat(receiveMessageRequest.visibilityTimeout())
        .isEqualTo(underTest.getQueue().getVisibilityTimeoutSeconds());
    assertThat(receiveMessageRequest.messageAttributeNames()).containsExactly("All");
  }

  @Test
  void testReceiveFailed() {
    underTest = createListener();
    when(mockSqsClient.receiveMessage(receiveRequestCaptor.capture()))
        .thenThrow(SdkClientException.create("testFailure"));

    underTest.subscribe();

    verify(mockHandler, after(TimeUnit.SECONDS.toMillis(1)).never()).handle(any());
    verify(mockSqsClient, after(TimeUnit.SECONDS.toMillis(2)).never())
        .deleteMessageBatch(any(DeleteMessageBatchRequest.class));
  }

  @Test
  void testProcessMessageFailed() {
    underTest = createListener();
    doThrow(new RuntimeException("processingFailure")).when(mockHandler).handle(any(Message.class));

    underTest.subscribe();

    verify(mockSqsClient, after(TimeUnit.MILLISECONDS.toMillis(500)).atLeastOnce())
        .receiveMessage(receiveRequestCaptor.capture());
    verify(mockSqsClient, after(TimeUnit.MILLISECONDS.toMillis(500)).never())
        .deleteMessageBatch(any(DeleteMessageBatchRequest.class));
  }

  private SqsMessageListener createListener() {
    return SqsMessageListener.builder()
        .client(mockSqsClient)
        .url("testQueue")
        .maxBatchSize(10)
        .visibilityTimeoutSeconds(90)
        .handler(mockHandler)
        .errorHandler(null)
        .longPolling(true)
        .autoAcknowledge(true)
        .concurrency(1)
        .build();
  }
}
