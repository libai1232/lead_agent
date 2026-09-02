package com.example.leadagent.service.llm;

import com.example.leadagent.model.Action;
import com.example.leadagent.model.Intent;
import com.example.leadagent.model.LlmDecision;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 测试环境使用的固定 LLM 实现，不会发起任何网络请求。
 */
@Service
@Profile("fake")
public class FakeLlmService implements LlmService {

    /**
     * 返回固定决策，用于验证 Agent 调用链和 HTTP 接口。
     *
     * @param message 客户消息；Fake 实现不分析该内容
     * @return 固定的模拟决策
     */
    @Override
    public LlmDecision analyze(String message) {
        return new LlmDecision(
                Intent.NEED_MORE_INFO,
                false,
                Action.REPLY,
                "这是一个模拟回复"
        );
    }
}
