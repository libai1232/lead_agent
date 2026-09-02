package com.example.leadagent.service;

import com.example.leadagent.dto.ChatResponse;
import com.example.leadagent.model.Action;
import com.example.leadagent.model.CustomerSession;
import com.example.leadagent.service.guard.OutputGuardService;
import org.springframework.stereotype.Component;

/**
 * 执行状态机裁决后的有限动作，并统一生成聊天响应。
 *
 * <p>动作只能来自 {@link Action} 枚举，并通过穷尽式 switch 分派；
 * 不根据客户输入或字符串动态调用方法。</p>
 */
@Component
public class ActionExecutor {

    private static final String SAFE_FALLBACK_REPLY =
            "抱歉，我无法提供内部系统信息。如果你想了解产品能力，我可以继续介绍。";

    private final CustomerRateLimiter customerRateLimiter;
    private final OutputGuardService outputGuardService;

    /**
     * 创建有限动作执行器。
     *
     * @param customerRateLimiter 最终发送 REPLY 前使用的客户级限流器
     * @param outputGuardService 回复草稿发送前使用的独立安全审核器
     */
    public ActionExecutor(
            CustomerRateLimiter customerRateLimiter,
            OutputGuardService outputGuardService
    ) {
        this.customerRateLimiter = customerRateLimiter;
        this.outputGuardService = outputGuardService;
    }

    /**
     * 执行一个受允许的 Agent 动作。
     *
     * @param action 最终有效动作
     * @param session 当前客户 Session
     * @param replyDraft LLM 生成的回复草稿，仅 REPLY 动作会使用
     * @return 动作执行后的聊天响应
     */
    public ChatResponse execute(Action action, CustomerSession session, String replyDraft) {
        // Session 状态变更与响应生成在同一临界区内完成，避免动作只执行一半。
        synchronized (session) {
            return switch (action) {
                case REPLY -> executeReply(session, replyDraft);
                case SCHEDULE_FOLLOWUP -> new ChatResponse(null, true);
                case ESCALATE_TO_HUMAN -> {
                    session.escalateToHuman();
                    yield new ChatResponse(null, true);
                }
                case MARK_NOT_INTERESTED -> {
                    session.close();
                    yield new ChatResponse(null, true);
                }
            };
        }
    }

    /**
     * 依次执行输出审核和最终发送限流。
     *
     * @param session 当前客户 Session
     * @param replyDraft 模型生成的原始回复草稿
     * @return 可发送回复或静默结果
     */
    private ChatResponse executeReply(CustomerSession session, String replyDraft) {
        String guardedReply = guardReply(replyDraft);

        // RateLimiter 紧邻最终返回位置；固定安全回复也属于一次真正主动发送。
        return customerRateLimiter.tryAcquire(session)
                ? new ChatResponse(guardedReply, false)
                : new ChatResponse(null, true);
    }

    /** Output Guard 返回 unsafe 或发生故障时，都不允许原始草稿继续发送。 */
    private String guardReply(String replyDraft) {
        try {
            // Output Guard 位于草稿生成之后，并且只裁决自然语言内容是否可发送。
            return outputGuardService.isSafe(replyDraft)
                    ? replyDraft
                    : SAFE_FALLBACK_REPLY;
        } catch (RuntimeException exception) {
            // 执行器自身再次 fail closed，避免审核实现异常导致原始草稿被放行。
            return SAFE_FALLBACK_REPLY;
        }
    }
}
