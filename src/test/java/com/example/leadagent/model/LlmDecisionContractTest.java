package com.example.leadagent.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证 LLM 结构化输出契约不会扩展到内部状态或工具信息。 */
class LlmDecisionContractTest {

    /** LlmDecision 必须且只能保留四个允许的业务字段。 */
    @Test
    void containsOnlyAllowedBusinessFields() {
        Set<String> componentNames = Arrays.stream(LlmDecision.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());

        assertThat(componentNames).containsExactlyInAnyOrder(
                "intent",
                "dissatisfied",
                "suggestedAction",
                "replyDraft"
        );
    }
}
