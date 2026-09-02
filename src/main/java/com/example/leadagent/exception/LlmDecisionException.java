package com.example.leadagent.exception;

/**
 * LLM 调用失败或模型输出无法转换为结构化决策时抛出的异常。
 *
 * <p>该异常避免把未经验证的模型原始输出直接返回给客户。</p>
 */
public class LlmDecisionException extends RuntimeException {

    public LlmDecisionException(String message) {
        super(message);
    }

    public LlmDecisionException(String message, Throwable cause) {
        super(message, cause);
    }
}
