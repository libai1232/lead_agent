package com.example.leadagent.service;

import com.example.leadagent.model.CustomerSession;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证客户级滚动 60 秒窗口及其并发原子性。 */
class CustomerRateLimiterTest {

    /** 验证窗口从实际发送时间开始计算，而不是按自然分钟切分。 */
    @Test
    void usesRollingSixtySecondWindow() {
        Instant firstSentAt = Instant.parse("2026-09-02T04:00:30Z");
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(
                firstSentAt,
                firstSentAt.plusSeconds(30),
                firstSentAt.plusSeconds(60)
        );
        CustomerRateLimiter rateLimiter = new CustomerRateLimiter(clock);
        CustomerSession session = new CustomerSession("customer-1");

        boolean firstAllowed = rateLimiter.tryAcquire(session);
        boolean nextNaturalMinuteBlocked = rateLimiter.tryAcquire(session);
        boolean exactBoundaryAllowed = rateLimiter.tryAcquire(session);

        assertThat(firstAllowed).isTrue();
        assertThat(nextNaturalMinuteBlocked).isFalse();
        assertThat(exactBoundaryAllowed).isTrue();
        assertThat(session.getLastSentAt()).isEqualTo(firstSentAt.plusSeconds(60));
    }

    /** 验证同一客户的并发请求只能有一个获得发送许可。 */
    @Test
    void allowsOnlyOneConcurrentSendForSameCustomer() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-02T04:00:00Z"), ZoneOffset.UTC);
        CustomerRateLimiter rateLimiter = new CustomerRateLimiter(clock);
        CustomerSession session = new CustomerSession("customer-1");

        long allowedCount = IntStream.range(0, 100)
                .parallel()
                .filter(ignored -> rateLimiter.tryAcquire(session))
                .count();

        assertThat(allowedCount).isEqualTo(1);
    }
}
