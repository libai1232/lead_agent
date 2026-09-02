package com.example.leadagent.model;

/**
 * LLM 对单条客户消息的唯一允许输出结构。
 *
 * intent 和 dissatisfied 是两个独立维度，例如 NEED_MORE_INFO 也可能同时 dissatisfied=true。
 * 该结构只包含业务分析结果，不包含 Session 状态、客户标识、限流数据或工具信息。
 *
 * @param intent 客户消息的业务意图
 * @param dissatisfied 是否存在明显不满情绪
 * @param suggestedAction LLM 建议的动作，最终仍需经过 Java 状态机裁决
 * @param replyDraft LLM 生成的回复草稿
 */
public record LlmDecision(
        Intent intent,
        boolean dissatisfied,
        Action suggestedAction,
        String replyDraft
) {
}
