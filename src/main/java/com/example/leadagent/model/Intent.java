package com.example.leadagent.model;

/**
 * LLM 对客户当前消息识别出的业务意图。
 */
public enum Intent {
    /** 客户明确表达兴趣。 */
    INTERESTED,
    /** 客户需要更多产品或服务信息。 */
    NEED_MORE_INFO,
    /** 客户明确拒绝继续沟通。 */
    REJECT,
    /** 客户消息与当前业务对话无关或答非所问。 */
    OFF_TOPIC,
    /** 无法归入其他明确类型。 */
    OTHER
}
