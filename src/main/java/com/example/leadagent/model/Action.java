package com.example.leadagent.model;

/**
 * Agent 被允许选择的有限动作集合。
 */
public enum Action {
    /** 生成并发送一条客户回复。 */
    REPLY,
    /** 本轮不回复，标记稍后跟进。 */
    SCHEDULE_FOLLOWUP,
    /** 将会话转交给人工处理。 */
    ESCALATE_TO_HUMAN,
    /** 标记客户不感兴趣并结束会话。 */
    MARK_NOT_INTERESTED
}
