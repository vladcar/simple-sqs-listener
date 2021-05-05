package com.vladc.sqslistener;

import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@ExtendWith(MockitoExtension.class)
class SqsQueueMessageListenerTest {

  @InjectMocks
  private SqsQueueMessageListener underTest;

  @Mock
  private SqsClient mockSqsClient;

  @BeforeEach
  void setup() {
    underTest = new SqsQueueMessageListener(mockSqsClient);
    underTest.setQueue(SqsQueue.builder()
        .url("testQueue")
        .maxBatchSize(1)
        .visibilityTimeoutSeconds(1)
        .longPolling(true)
        .messageHandler(message -> {})
        .build());

    List<Message> testMessages = List.of(Message.builder().messageId("testId").build(),
        Message.builder().messageId("testId2").build());

    when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
        .thenReturn(ReceiveMessageResponse.builder().messages(testMessages).build());
    when(mockSqsClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
        .thenReturn(DeleteMessageBatchResponse.builder().build());
  }

  @AfterEach
  void teardown() {
    underTest.destroy();
  }
}