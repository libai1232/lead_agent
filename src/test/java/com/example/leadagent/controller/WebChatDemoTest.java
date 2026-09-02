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

/** 使用真实 HTTP 请求验证网页聊天 Demo 的静态资源可访问。 */
@ActiveProfiles("fake")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.ai.model.chat=none"
)
class WebChatDemoTest {

    @LocalServerPort
    private int port;

    /** 首页必须包含客户标识、聊天记录、消息输入框和发送按钮。 */
    @Test
    void servesChatPage() throws Exception {
        HttpResponse<String> response = get("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("id=\"customer-id\"")
                .contains("id=\"chat-history\"")
                .contains("id=\"message\"")
                .contains("id=\"send-button\"")
                // 初筛字段和人工摘要必须位于聊天区域之外的独立展示面板。
                .contains("class=\"lead-panel\"")
                .contains("id=\"lead-product\"")
                .contains("id=\"lead-need\"")
                .contains("id=\"handoff-summary\"");
    }

    /** 页面脚本必须调用聊天 API 并处理 silent 响应。 */
    @Test
    void servesChatScriptWithSilentHandling() throws Exception {
        HttpResponse<String> response = get("/app.js");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("fetch(\"/api/chat\"")
                .contains("result.silent === true")
                .contains("result.intent")
                .contains("result.action")
                .contains("result.sessionStatus")
                .contains("result.abnormalCount")
                .contains("result.leadProfile")
                .contains("result.handoffSummary")
                .contains("renderLeadInformation(result)")
                .contains("本轮 Agent 未自动回复");
    }

    /** 发送一个简单 GET 请求到当前随机端口。 */
    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();

        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
