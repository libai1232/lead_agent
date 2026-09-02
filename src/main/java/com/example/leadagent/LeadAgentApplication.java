package com.example.leadagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lead Agent 服务的 Spring Boot 启动入口。
 */
@SpringBootApplication
public class LeadAgentApplication {

    /**
     * 启动应用及其内嵌 Web 服务器。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(LeadAgentApplication.class, args);
    }
}
