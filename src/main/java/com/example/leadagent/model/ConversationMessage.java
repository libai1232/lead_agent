package com.example.leadagent.model;

/**
 * Session 内保存的一条有限对话历史。
 *
 * @param role 消息发送方
 * @param content 实际发送或接收的文本
 */
public record ConversationMessage(
        ConversationRole role,
        String content
) {
}
