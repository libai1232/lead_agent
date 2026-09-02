package com.example.leadagent.service;

import com.example.leadagent.model.CustomerSession;
import com.example.leadagent.model.SessionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证内存 Session 的默认创建和复用行为。
 */
class SessionServiceTest {

    private final SessionService sessionService = new SessionService();

    /** 验证新 Session 的所有默认字段。 */
    @Test
    void createsSessionWithDefaults() {
        CustomerSession session = sessionService.getOrCreate("customer-1");

        assertThat(session.getCustomerId()).isEqualTo("customer-1");
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getAbnormalCount()).isZero();
        assertThat(session.getLastSentAt()).isNull();
        // 新 Session 的扩展资料也必须从空状态开始。
        assertThat(session.getConversationHistorySnapshot()).isEmpty();
        assertThat(session.getLeadProfile().interestedProduct()).isNull();
        assertThat(session.getHandoffSummary()).isNull();
    }

    /** 验证同一 customerId 始终获取同一个内存对象。 */
    @Test
    void returnsExistingSessionForSameCustomer() {
        CustomerSession first = sessionService.getOrCreate("customer-1");
        CustomerSession second = sessionService.getOrCreate("customer-1");

        assertThat(second).isSameAs(first);
    }

    /** 验证人工重新激活会恢复 ACTIVE 并清空异常计数。 */
    @Test
    void reactivatesHumanEscalatedSessionAndResetsAbnormalCount() {
        CustomerSession session = sessionService.getOrCreate("customer-1");
        session.setAbnormalCount(2);
        session.escalateToHuman();
        session.setHandoffSummary("旧的人工摘要");

        boolean reactivated = sessionService.reactivate("customer-1");

        assertThat(reactivated).isTrue();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getAbnormalCount()).isZero();
        assertThat(session.getHandoffSummary()).isNull();
    }

    /** 验证 CLOSED Session 不能通过人工重新激活接口恢复。 */
    @Test
    void doesNotReactivateClosedSession() {
        CustomerSession session = sessionService.getOrCreate("customer-1");
        session.setAbnormalCount(2);
        session.close();

        boolean reactivated = sessionService.reactivate("customer-1");

        assertThat(reactivated).isFalse();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(session.getAbnormalCount()).isEqualTo(2);
    }
}
