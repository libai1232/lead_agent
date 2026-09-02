package com.example.leadagent.service.guard;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** 测试环境使用的固定安全审核实现，不会调用真实模型。 */
@Service
@Profile("fake")
public class FakeOutputGuardService implements OutputGuardService {

    /** 测试链路固定允许回复，以便隔离真实 LLM 调用。 */
    @Override
    public boolean isSafe(String replyDraft) {
        return true;
    }
}
