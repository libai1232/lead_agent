package com.example.leadagent.controller;

import com.example.leadagent.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟人工管理 Session 的独立 HTTP 入口。
 *
 * <p>该 Demo 接口与客户聊天入口分离，不接收或执行客户消息。</p>
 */
@RestController
@RequestMapping("/api/session")
public class SessionManagementController {

    private final SessionService sessionService;

    public SessionManagementController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * 将已转人工的客户 Session 恢复为 ACTIVE，并清空异常计数。
     *
     * @param customerId 要恢复的客户标识
     */
    @PostMapping("/{customerId}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivate(@PathVariable String customerId) {
        // Controller 仅做路由转发，状态判断和原子变更由 SessionService/CustomerSession 完成。
        sessionService.reactivate(customerId);
    }
}
