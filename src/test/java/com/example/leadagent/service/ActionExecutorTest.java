package com.example.leadagent.service;

import com.example.leadagent.dto.ChatResponse;
import com.example.leadagent.model.Action;
import com.example.leadagent.model.CustomerSession;
import com.example.leadagent.model.SessionStatus;
import com.example.leadagent.service.guard.OutputGuardService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证四种受允许动作的响应和 Session 状态变化。 */
class ActionExecutorTest {

    private final ActionExecutor actionExecutor = new ActionExecutor(
            new CustomerRateLimiter(),
            replyDraft -> true
    );

    /** REPLY 返回回复草稿，且不改变 Session 状态。 */
    @Test
    void replyReturnsDraft() {
        CustomerSession session = new CustomerSession("customer-1");

        ChatResponse response = actionExecutor.execute(Action.REPLY, session, "回复草稿");

        assertThat(response.message()).isEqualTo("回复草稿");
        assertThat(response.silent()).isFalse();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getLastSentAt()).isNotNull();
    }

    /** 第二个 REPLY 在距离首次发送不足 60 秒时被最终发送层强制静默。 */
    @Test
    void secondReplyWithinWindowIsSilent() {
        Instant now = Instant.parse("2026-09-02T04:00:00Z");
        CustomerRateLimiter rateLimiter = new CustomerRateLimiter(Clock.fixed(now, ZoneOffset.UTC));
        ActionExecutor executor = new ActionExecutor(rateLimiter, replyDraft -> true);
        CustomerSession session = new CustomerSession("customer-1");

        ChatResponse firstResponse = executor.execute(Action.REPLY, session, "第一条回复");
        ChatResponse secondResponse = executor.execute(Action.REPLY, session, "第二条回复");

        assertThat(firstResponse.silent()).isFalse();
        assertThat(secondResponse.message()).isNull();
        assertThat(secondResponse.silent()).isTrue();
        assertThat(session.getLastSentAt()).isEqualTo(now);
    }

    /** unsafe 草稿必须替换为固定安全回复，并将该发送计入限流。 */
    @Test
    void replacesUnsafeReplyWithFixedSafeMessage() {
        Instant now = Instant.parse("2026-09-02T04:00:00Z");
        CustomerRateLimiter rateLimiter = new CustomerRateLimiter(Clock.fixed(now, ZoneOffset.UTC));
        ActionExecutor executor = new ActionExecutor(rateLimiter, replyDraft -> false);
        CustomerSession session = new CustomerSession("customer-1");

        ChatResponse response = executor.execute(Action.REPLY, session, "不应发送的原始草稿");

        assertThat(response.message()).isEqualTo(
                "抱歉，我无法提供内部系统信息。如果你想了解产品能力，我可以继续介绍。"
        );
        assertThat(response.silent()).isFalse();
        assertThat(session.getLastSentAt()).isEqualTo(now);
    }

    /** Output Guard 抛出异常时也必须使用固定安全回复。 */
    @Test
    void replacesReplyWhenOutputGuardFails() {
        OutputGuardService outputGuardService = replyDraft -> {
            throw new IllegalStateException("guard unavailable");
        };
        ActionExecutor executor = new ActionExecutor(new CustomerRateLimiter(), outputGuardService);
        CustomerSession session = new CustomerSession("customer-1");

        ChatResponse response = executor.execute(Action.REPLY, session, "未经审核的原始草稿");

        assertThat(response.message()).isEqualTo(
                "抱歉，我无法提供内部系统信息。如果你想了解产品能力，我可以继续介绍。"
        );
        assertThat(response.silent()).isFalse();
    }

    /** Output Guard 必须先于最终 RateLimiter 执行。 */
    @Test
    void reviewsOutputBeforeFinalRateLimitCheck() {
        OutputGuardService outputGuardService = mock(OutputGuardService.class);
        CustomerRateLimiter rateLimiter = mock(CustomerRateLimiter.class);
        CustomerSession session = new CustomerSession("customer-1");
        when(outputGuardService.isSafe("回复草稿")).thenReturn(true);
        when(rateLimiter.tryAcquire(session)).thenReturn(true);
        ActionExecutor executor = new ActionExecutor(rateLimiter, outputGuardService);

        executor.execute(Action.REPLY, session, "回复草稿");

        var orderedCalls = inOrder(outputGuardService, rateLimiter);
        orderedCalls.verify(outputGuardService).isSafe("回复草稿");
        orderedCalls.verify(rateLimiter).tryAcquire(session);
    }

    /** SCHEDULE_FOLLOWUP 本轮保持静默，且不改变 Session 状态。 */
    @Test
    void scheduleFollowupRemainsSilent() {
        CustomerSession session = new CustomerSession("customer-1");

        ChatResponse response = actionExecutor.execute(Action.SCHEDULE_FOLLOWUP, session, "不会发送");

        assertThat(response.message()).isNull();
        assertThat(response.silent()).isTrue();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getLastSentAt()).isNull();
    }

    /** ESCALATE_TO_HUMAN 转人工并在本轮保持静默。 */
    @Test
    void escalateToHumanUpdatesStatusAndRemainsSilent() {
        CustomerSession session = new CustomerSession("customer-1");

        ChatResponse response = actionExecutor.execute(Action.ESCALATE_TO_HUMAN, session, "不会发送");

        assertThat(response.message()).isNull();
        assertThat(response.silent()).isTrue();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.HUMAN_ESCALATED);
        assertThat(session.getLastSentAt()).isNull();
    }

    /** MARK_NOT_INTERESTED 关闭会话并在本轮保持静默。 */
    @Test
    void markNotInterestedClosesSessionAndRemainsSilent() {
        CustomerSession session = new CustomerSession("customer-1");

        ChatResponse response = actionExecutor.execute(Action.MARK_NOT_INTERESTED, session, "不会发送");

        assertThat(response.message()).isNull();
        assertThat(response.silent()).isTrue();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(session.getLastSentAt()).isNull();
    }
}
