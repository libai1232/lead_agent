package com.example.leadagent.service.llm;

import com.example.leadagent.model.LlmDecision;
import com.example.leadagent.model.ConversationMessage;

import java.util.List;

/**
 * 客户消息分析能力的统一抽象。
 *
 * <p>生产环境和测试环境可以提供不同实现，而 AgentService 无需感知底层模型。</p>
 */
public interface LlmService {

    /**
     * 将客户消息转换为结构化 LLM 决策。
     *
     * @param message 客户消息正文
     * @return 意图、情绪信号、建议动作和回复草稿
     */
    LlmDecision analyze(String message);

    /**
     * 使用有限历史辅助理解当前消息，但意图和不满信号仍只能判断当前消息。
     *
     * <p>默认实现保持测试替身和简单实现兼容；真实 LLM 实现会使用历史上下文。</p>
     *
     * @param message 当前客户消息
     * @param history 当前消息之前的有限历史快照
     * @return 当前轮结构化决策
     */
    default LlmDecision analyze(String message, List<ConversationMessage> history) {
        return analyze(message);
    }
}
