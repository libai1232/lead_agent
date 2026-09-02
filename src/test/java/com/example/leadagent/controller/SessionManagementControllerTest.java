package com.example.leadagent.controller;

import com.example.leadagent.model.CustomerSession;
import com.example.leadagent.model.SessionStatus;
import com.example.leadagent.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** 使用真实 HTTP 请求验证 Demo 人工重新激活接口。 */
@ActiveProfiles("fake")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.ai.model.chat=none"
)
class SessionManagementControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private SessionService sessionService;

    /** 验证管理接口恢复 Session 并清空异常计数。 */
    @Test
    void reactivatesSessionThroughManagementEndpoint() throws Exception {
        CustomerSession session = sessionService.getOrCreate("reactivate-api-customer");
        session.setAbnormalCount(2);
        session.escalateToHuman();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port
                        + "/api/session/reactivate-api-customer/reactivate"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getAbnormalCount()).isZero();
    }
}
