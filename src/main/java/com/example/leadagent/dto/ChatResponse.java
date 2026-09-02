package com.example.leadagent.dto;

import com.example.leadagent.model.Action;
import com.example.leadagent.model.Intent;
import com.example.leadagent.model.LeadProfile;
import com.example.leadagent.model.SessionStatus;

import java.time.Instant;

/**
 * 聊天接口的响应数据。
 *
 * @param message 可发送给客户的回复；静默时为 {@code null}
 * @param silent 是否必须保持静默
 * @param intent LLM 判断的客户意图；未调用或解析失败时为 {@code null}
 * @param dissatisfied LLM 判断的明显不满信号；未调用或解析失败时为 {@code null}
 * @param action Java 状态机裁决后的最终动作；尚未裁决时为 {@code null}
 * @param sessionStatus 动作执行后的 Session 状态
 * @param abnormalCount 动作执行后的连续异常计数
 * @param lastSentAt 最近一次真正发送回复的时间
 * @param leadProfile 当前累计的客户初筛字段
 * @param handoffSummary 转人工后供人工查看的摘要；未转人工时为 {@code null}
 * @param conversationHistorySize 当前 Session 保存的有限历史消息数
 */
public record ChatResponse(
        String message,
        boolean silent,
        Intent intent,
        Boolean dissatisfied,
        Action action,
        SessionStatus sessionStatus,
        int abnormalCount,
        Instant lastSentAt,
        LeadProfile leadProfile,
        String handoffSummary,
        int conversationHistorySize
) {

    /**
     * 创建尚未附加 Demo 调试字段的动作响应。
     *
     * <p>最终由 AgentService 统一补充只读诊断信息。</p>
     */
    public ChatResponse(String message, boolean silent) {
        this(message, silent, null, null, null, null, 0, null, LeadProfile.empty(), null, 0);
    }
}
