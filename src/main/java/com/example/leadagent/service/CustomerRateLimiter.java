package com.example.leadagent.service;

import com.example.leadagent.model.CustomerSession;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 限制同一个客户在任意连续 60 秒窗口内最多发送一条主动消息。
 *
 * <p>限流基于 Session 的最近成功发送时间，不使用固定自然分钟窗口。</p>
 */
@Component
public class CustomerRateLimiter {

    private static final Duration SEND_WINDOW = Duration.ofSeconds(60);
    private final Clock clock;

    /** 使用系统 UTC 时钟创建生产环境限流器。 */
    public CustomerRateLimiter() {
        this(Clock.systemUTC());
    }

    /**
     * 使用指定时钟创建限流器，便于确定性测试时间边界。
     *
     * @param clock 提供当前时间的时钟
     */
    CustomerRateLimiter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 原子检查当前客户是否可以发送，并在允许时记录本次发送时间。
     *
     * <p>当前最小 Demo 将成功构造并返回 REPLY 响应视为真正发送成功，
     * 因此许可成功时立即提交 {@code lastSentAt}。</p>
     *
     * @param session 当前客户 Session
     * @return {@code true} 表示允许发送，{@code false} 表示仍处于 60 秒窗口内
     */
    public boolean tryAcquire(CustomerSession session) {
        // 对同一个 Session 加锁，保证“检查 + 更新时间”不可被并发请求拆开。
        synchronized (session) {
            Instant now = clock.instant();
            Instant lastSentAt = session.getLastSentAt();

            // 只有经过完整 60 秒后才重新允许发送，恰好 60 秒时可以发送。
            if (lastSentAt != null && now.isBefore(lastSentAt.plus(SEND_WINDOW))) {
                return false;
            }

            session.setLastSentAt(now);
            return true;
        }
    }
}
