package com.example.leadagent.model;

import com.example.leadagent.service.ActionExecutor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证动作集合和执行器入口保持为封闭的 Java 枚举契约。 */
class ActionContractTest {

    /** Action 枚举必须且只能包含四种允许动作。 */
    @Test
    void containsExactlyFourAllowedActions() {
        assertThat(Action.values()).containsExactly(
                Action.REPLY,
                Action.SCHEDULE_FOLLOWUP,
                Action.ESCALATE_TO_HUMAN,
                Action.MARK_NOT_INTERESTED
        );
    }

    /** ActionExecutor 的执行入口只接受 Action，不存在字符串动作入口。 */
    @Test
    void executorAcceptsOnlyTypedActionEnum() {
        Method[] executeMethods = Arrays.stream(ActionExecutor.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("execute"))
                .toArray(Method[]::new);

        assertThat(executeMethods).hasSize(1);
        assertThat(executeMethods[0].getParameterTypes()).containsExactly(
                Action.class,
                CustomerSession.class,
                String.class
        );
    }
}
