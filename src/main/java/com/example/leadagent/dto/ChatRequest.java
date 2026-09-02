package com.example.leadagent.dto;

/**
 * 聊天接口的请求数据。
 *
 * @param customerId 客户唯一标识，用于定位同一客户的 Session
 * @param message 客户本次发送的消息
 */
public record ChatRequest(String customerId, String message) {
}
