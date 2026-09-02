package com.example.leadagent.service;

import com.example.leadagent.dto.ChatRequest;
import com.example.leadagent.dto.ChatResponse;
import com.example.leadagent.exception.LlmDecisionException;
import com.example.leadagent.model.Action;
import com.example.leadagent.model.CustomerSession;
import com.example.leadagent.model.Intent;
import com.example.leadagent.model.LeadQualificationUpdate;
import com.example.leadagent.model.LlmDecision;
import com.example.leadagent.model.SessionStatus;
import com.example.leadagent.service.llm.LlmService;
import com.example.leadagent.service.qualification.LeadQualificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证 Agent 编排层的静默短路和状态机覆盖行为。
 */
@ExtendWith(OutputCaptureExtension.class)
class AgentServiceTest {

    /** 单元测试默认不提取初筛资料，使每个测试只聚焦原有状态约束。 */
    private static final LeadQualificationService NO_QUALIFICATION = (message, history) -> null;

    /** 验证 ACTIVE Session 会调用 LLM、执行 REPLY 并保持 ACTIVE。 */
    @Test
    void activeSessionProcessesMessageNormally() {
        LlmService llmService = mock(LlmService.class);
        SessionService sessionService = new SessionService();
        AgentService agentService = new AgentService(
                llmService,
                sessionService,
                new SessionStateMachine(),
                new ActionExecutor(new CustomerRateLimiter(), replyDraft -> true),
                NO_QUALIFICATION,
                new HumanHandoffSummaryService()
        );
        when(llmService.analyze(eq("normal-message"), anyList())).thenReturn(new LlmDecision(
                Intent.NEED_MORE_INFO,
                false,
                Action.REPLY,
                "正常回复"
        ));

        ChatResponse response = agentService.handleMessage(
                new ChatRequest("active-customer", "normal-message")
        );
        CustomerSession session = sessionService.getOrCreate("active-customer");

        assertThat(response.message()).isEqualTo("正常回复");
        assertThat(response.silent()).isFalse();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getAbnormalCount()).isZero();
        assertThat(session.getLastSentAt()).isNotNull();
        verify(llmService).analyze(eq("normal-message"), anyList());
    }

    /** 验证非 ACTIVE Session 在调用 LLM 之前被强制静默。 */
    @ParameterizedTest
    @EnumSource(value = SessionStatus.class, names = {"HUMAN_ESCALATED", "CLOSED"})
    void remainsSilentAndDoesNotCallLlmWhenSessionIsNotActive(SessionStatus status) {
        LlmService llmService = mock(LlmService.class);
        SessionService sessionService = new SessionService();
        CustomerSession session = sessionService.getOrCreate("customer-1");
        if (status == SessionStatus.HUMAN_ESCALATED) {
            session.escalateToHuman();
        } else {
            session.close();
        }
        AgentService agentService = new AgentService(
                llmService,
                sessionService,
                new SessionStateMachine(),
                new ActionExecutor(new CustomerRateLimiter(), replyDraft -> true),
                NO_QUALIFICATION,
                new HumanHandoffSummaryService()
        );

        ChatResponse response = agentService.handleMessage(
                new ChatRequest("customer-1", "请回复我")
        );

        assertThat(response.silent()).isTrue();
        assertThat(response.message()).isNull();
        assertThat(session.getStatus()).isEqualTo(status);
        verifyNoInteractions(llmService);
    }

    /** 验证第二次连续异常会丢弃 LLM 回复并将 Session 转为人工处理。 */
    @Test
    void secondConsecutiveAbnormalMessageForcesEscalation() {
        LlmService llmService = mock(LlmService.class);
        SessionService sessionService = new SessionService();
        AgentService agentService = new AgentService(
                llmService,
                sessionService,
                new SessionStateMachine(),
                new ActionExecutor(new CustomerRateLimiter(), replyDraft -> true),
                NO_QUALIFICATION,
                new HumanHandoffSummaryService()
        );
        when(llmService.analyze(eq("first"), anyList())).thenReturn(new LlmDecision(
                Intent.OFF_TOPIC,
                false,
                Action.REPLY,
                "first reply"
        ));
        when(llmService.analyze(eq("second"), anyList())).thenReturn(new LlmDecision(
                Intent.NEED_MORE_INFO,
                true,
                Action.REPLY,
                "second reply"
        ));

        ChatResponse firstResponse = agentService.handleMessage(new ChatRequest("customer-1", "first"));
        ChatResponse secondResponse = agentService.handleMessage(new ChatRequest("customer-1", "second"));
        CustomerSession session = sessionService.getOrCreate("customer-1");

        assertThat(firstResponse.silent()).isFalse();
        assertThat(secondResponse.silent()).isTrue();
        assertThat(secondResponse.message()).isNull();
        assertThat(session.getAbnormalCount()).isEqualTo(2);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.HUMAN_ESCALATED);
        // 转人工摘要必须随最终响应返回，并且不作为聊天回复发送给客户。
        assertThat(secondResponse.handoffSummary())
                .contains("当前意图：NEED_MORE_INFO")
                .contains("明显不满：true");
    }

    /** 验证结构化输出失败时不会进入状态机、动作执行器或修改 Session。 */
    @Test
    void remainsSilentAndExecutesNoActionWhenLlmDecisionParsingFails(CapturedOutput output) {
        LlmService llmService = mock(LlmService.class);
        SessionService sessionService = new SessionService();
        SessionStateMachine stateMachine = mock(SessionStateMachine.class);
        ActionExecutor actionExecutor = mock(ActionExecutor.class);
        AgentService agentService = new AgentService(
                llmService,
                sessionService,
                stateMachine,
                actionExecutor,
                NO_QUALIFICATION,
                new HumanHandoffSummaryService()
        );
        when(llmService.analyze(eq("malformed-output"), anyList()))
                .thenThrow(new LlmDecisionException("invalid structured output"));

        ChatResponse response = agentService.handleMessage(
                new ChatRequest("safe-failure-customer", "malformed-output")
        );
        CustomerSession session = sessionService.getOrCreate("safe-failure-customer");

        assertThat(response.message()).isNull();
        assertThat(response.silent()).isTrue();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getAbnormalCount()).isZero();
        assertThat(session.getLastSentAt()).isNull();
        verifyNoInteractions(stateMachine, actionExecutor);
        assertThat(output.getOut())
                .contains("LLM decision failed; returning silent response")
                .contains("exceptionType=LlmDecisionException")
                .doesNotContain("safe-failure-customer")
                .doesNotContain("malformed-output")
                .doesNotContain("invalid structured output");
    }

    /** 验证转人工后只能先由管理服务恢复，之后客户消息才会重新进入 LLM 流程。 */
    @Test
    void processesMessagesAgainOnlyAfterHumanReactivation() {
        LlmService llmService = mock(LlmService.class);
        SessionService sessionService = new SessionService();
        CustomerSession session = sessionService.getOrCreate("reactivated-customer");
        session.setAbnormalCount(2);
        session.escalateToHuman();
        AgentService agentService = new AgentService(
                llmService,
                sessionService,
                new SessionStateMachine(),
                new ActionExecutor(new CustomerRateLimiter(), replyDraft -> true),
                NO_QUALIFICATION,
                new HumanHandoffSummaryService()
        );

        ChatResponse beforeReactivation = agentService.handleMessage(
                new ChatRequest("reactivated-customer", "before-reactivation")
        );
        verifyNoInteractions(llmService);

        boolean reactivated = sessionService.reactivate("reactivated-customer");
        when(llmService.analyze(eq("after-reactivation"), anyList())).thenReturn(new LlmDecision(
                Intent.INTERESTED,
                false,
                Action.REPLY,
                "已恢复自动处理"
        ));
        ChatResponse afterReactivation = agentService.handleMessage(
                new ChatRequest("reactivated-customer", "after-reactivation")
        );

        assertThat(beforeReactivation.message()).isNull();
        assertThat(beforeReactivation.silent()).isTrue();
        assertThat(reactivated).isTrue();
        assertThat(afterReactivation.message()).isEqualTo("已恢复自动处理");
        assertThat(afterReactivation.silent()).isFalse();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getAbnormalCount()).isZero();
        verify(llmService).analyze(eq("after-reactivation"), anyList());
    }

    /** 验证有限历史只辅助后一轮处理，初筛字段以增量方式累计。 */
    @Test
    void passesBoundedHistoryAndMergesLeadQualificationFields() {
        LlmService llmService = mock(LlmService.class);
        LeadQualificationService qualificationService = mock(LeadQualificationService.class);
        SessionService sessionService = new SessionService();
        AgentService agentService = new AgentService(
                llmService,
                sessionService,
                new SessionStateMachine(),
                new ActionExecutor(new CustomerRateLimiter(), replyDraft -> true),
                qualificationService,
                new HumanHandoffSummaryService()
        );
        when(llmService.analyze(eq("first"), anyList())).thenReturn(new LlmDecision(
                Intent.INTERESTED, false, Action.SCHEDULE_FOLLOWUP, ""
        ));
        when(llmService.analyze(eq("second"), anyList())).thenReturn(new LlmDecision(
                Intent.NEED_MORE_INFO, false, Action.SCHEDULE_FOLLOWUP, ""
        ));
        when(qualificationService.extract(eq("first"), anyList())).thenReturn(
                new LeadQualificationUpdate("企业版", "", "", "", "", "")
        );
        when(qualificationService.extract(eq("second"), anyList())).thenReturn(
                new LeadQualificationUpdate("", "自动筛选线索", "", "本季度", "", "")
        );

        agentService.handleMessage(new ChatRequest("profile-customer", "first"));
        ChatResponse secondResponse = agentService.handleMessage(
                new ChatRequest("profile-customer", "second")
        );

        // 第一轮 SCHEDULE_FOLLOWUP 只保存客户消息，因此第二轮模型看到一条先前历史。
        verify(llmService).analyze(eq("second"), argThat(history ->
                history.size() == 1 && history.getFirst().content().equals("first")
        ));
        assertThat(secondResponse.leadProfile().interestedProduct()).isEqualTo("企业版");
        assertThat(secondResponse.leadProfile().customerNeed()).isEqualTo("自动筛选线索");
        assertThat(secondResponse.leadProfile().purchaseTimeline()).isEqualTo("本季度");
        assertThat(secondResponse.conversationHistorySize()).isEqualTo(2);
    }
}
