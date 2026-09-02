package com.example.leadagent.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证 Session 对话历史具有固定上限且保留最新消息。 */
class CustomerSessionHistoryTest {

    /** 超过十条历史时必须移除最早消息，避免上下文和内存无限增长。 */
    @Test
    void keepsOnlyTenMostRecentMessages() {
        CustomerSession session = new CustomerSession("history-customer");

        for (int index = 0; index < 12; index++) {
            session.addConversationMessage(new ConversationMessage(
                    ConversationRole.CUSTOMER,
                    "message-" + index
            ));
        }

        var history = session.getConversationHistorySnapshot();
        assertThat(history).hasSize(10);
        assertThat(history.getFirst().content()).isEqualTo("message-2");
        assertThat(history.getLast().content()).isEqualTo("message-11");
    }
}
