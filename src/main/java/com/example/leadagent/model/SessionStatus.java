package com.example.leadagent.model;

/**
 * 客户会话的生命周期状态。
 */
public enum SessionStatus {
    /** Agent 可以继续自动处理消息。 */
    ACTIVE,
    /** 会话已转人工，Agent 必须保持静默。 */
    HUMAN_ESCALATED,
    /** 会话已结束，Agent 必须保持静默。 */
    CLOSED
}
