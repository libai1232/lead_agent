package com.example.leadagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供最小健康检查接口，用于确认服务已经正常启动。
 */
@RestController
public class HealthController {

    /**
     * 返回服务健康状态。
     *
     * @return 固定字符串 {@code ok}
     */
    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
