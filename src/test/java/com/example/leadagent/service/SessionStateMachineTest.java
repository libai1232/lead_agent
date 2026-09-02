package com.example.leadagent.service;

import com.example.leadagent.model.Action;
import com.example.leadagent.model.CustomerSession;
import com.example.leadagent.model.Intent;
import com.example.leadagent.model.LlmDecision;
import com.example.leadagent.model.SessionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证异常计数和强制转人工规则完全由 Java 状态机执行。
 */
class SessionStateMachineTest {

    private final SessionStateMachine stateMachine = new SessionStateMachine();

    /** 验证两种异常信号共享计数器，并在第二次异常时覆盖模型建议。 */
    @Test
    void offTopicAndDissatisfactionShareCounterAndOverrideSuggestedAction() {
        CustomerSession session = new CustomerSession("customer-1");

        Action firstAction = stateMachine.applyDecision(session, new LlmDecision(
                Intent.OFF_TOPIC,
                false,
                Action.REPLY,
                "first reply"
        ));
        Action secondAction = stateMachine.applyDecision(session, new LlmDecision(
                Intent.NEED_MORE_INFO,
                true,
                Action.REPLY,
                "second reply"
        ));

        assertThat(firstAction).isEqualTo(Action.REPLY);
        assertThat(secondAction).isEqualTo(Action.ESCALATE_TO_HUMAN);
        assertThat(session.getAbnormalCount()).isEqualTo(2);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.HUMAN_ESCALATED);
    }

    /** 验证一条正常消息会中断连续异常并将计数重置为零。 */
    @Test
    void normalMessageResetsAbnormalCount() {
        CustomerSession session = new CustomerSession("customer-1");

        stateMachine.applyDecision(session, new LlmDecision(
                Intent.OFF_TOPIC,
                false,
                Action.REPLY,
                "reply"
        ));
        stateMachine.applyDecision(session, new LlmDecision(
                Intent.NEED_MORE_INFO,
                false,
                Action.REPLY,
                "reply"
        ));

        assertThat(session.getAbnormalCount()).isZero();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
    }
}
