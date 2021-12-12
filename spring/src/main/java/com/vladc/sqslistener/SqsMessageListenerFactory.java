package com.vladc.sqslistener;

import com.vladc.sqslistener.internal.SqsQueueAttributes;

public interface SqsMessageListenerFactory {

  MessageListener createListener(SqsQueueAttributes attributes);
}
