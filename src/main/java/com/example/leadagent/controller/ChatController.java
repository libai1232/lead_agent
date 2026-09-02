package com.example.leadagent.controller;

import com.example.leadagent.dto.ChatRequest;
import com.example.leadagent.dto.ChatResponse;
import com.example.leadagent.service.AgentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 接收客户聊天请求的 HTTP 入口。
 *
 * <p>Controller 只负责协议转换，所有 Agent 处理均委托给 {@link AgentService}。</p>
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 接收一条客户消息并返回 Agent 处理结果。
     *
     * @param request 客户标识和消息正文
     * @return 回复内容及是否静默
     */
    @PostMapping(produces = "application/json;charset=UTF-8")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return agentService.handleMessage(request);
    }
}
