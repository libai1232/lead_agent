package com.example.leadagent.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("fake")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.ai.model.chat=none"
)
/**
 * 使用 Fake LLM 验证聊天接口的完整 HTTP 调用链。
 */
class ChatControllerTest {

    @LocalServerPort
    private int port;

    /** 验证 ACTIVE Session 返回模拟回复且不处于静默状态。 */
    @Test
    void returnsFixedMessage() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"customerId":"customer-1","message":"你好"}
                        """))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type"))
                .contains("application/json;charset=UTF-8");
        assertThat(response.body())
                .contains("\"message\":\"这是一个模拟回复\"")
                .contains("\"silent\":false")
                .contains("\"intent\":\"NEED_MORE_INFO\"")
                .contains("\"dissatisfied\":false")
                .contains("\"action\":\"REPLY\"")
                .contains("\"sessionStatus\":\"ACTIVE\"")
                .contains("\"abnormalCount\":0")
                .contains("\"lastSentAt\":")
                // Fake 初筛服务验证资料能够沿完整 HTTP 链路返回给 Demo 前端。
                .contains("\"interestedProduct\":\"模拟产品\"")
                .contains("\"customerNeed\":\"了解产品能力\"")
                .contains("\"handoffSummary\":null")
                .contains("\"conversationHistorySize\":2");
    }
}
