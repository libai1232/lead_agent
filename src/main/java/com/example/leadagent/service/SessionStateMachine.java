package com.example.leadagent.service;

import com.example.leadagent.model.Action;
import com.example.leadagent.model.CustomerSession;
import com.example.leadagent.model.Intent;
import com.example.leadagent.model.LlmDecision;
import com.example.leadagent.model.SessionStatus;
import org.springframework.stereotype.Component;

/**
 * 负责执行确定性的客户会话状态规则。
 *
 * <p>该类拥有最终动作裁决权；LLM 的 suggestedAction 只能作为建议，不能绕过这里的规则。</p>
 */
@Component
public class SessionStateMachine {

    /**
     * 根据 LLM 分析结果更新异常计数并计算最终有效动作。
     *
     * <p>同一 Session 上的计数更新和状态转换在同一个同步块中完成，避免并发丢失更新。</p>
     *
     * @param session 当前客户 Session
     * @param decision LLM 对本条消息的结构化分析
     * @return Java 规则裁决后的最终动作
     */
    public Action applyDecision(CustomerSession session, LlmDecision decision) {
        synchronized (session) {
            // 一旦转人工，任何后续模型建议都不能恢复自动处理。
            if (session.getStatus() == SessionStatus.HUMAN_ESCALATED) {
                return Action.ESCALATE_TO_HUMAN;
            }

            // OFF_TOPIC 和明显不满是正交信号，但共用同一个连续异常计数器。
            boolean abnormal = decision.intent() == Intent.OFF_TOPIC || decision.dissatisfied();
            if (abnormal) {
                session.setAbnormalCount(session.getAbnormalCount() + 1);
            } else {
                session.setAbnormalCount(0);
            }

            // 第二次连续异常由 Java 强制转人工，覆盖 LLM 的 suggestedAction。
            if (session.getAbnormalCount() >= 2) {
                session.escalateToHuman();
                return Action.ESCALATE_TO_HUMAN;
            }

            // 未触发强制规则时才允许采用模型建议。
            return decision.suggestedAction();
        }
    }
}
