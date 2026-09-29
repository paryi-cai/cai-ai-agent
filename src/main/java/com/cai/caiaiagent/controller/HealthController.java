package com.cai.caiaiagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口：用于验证项目是否正常启动、接口文档是否可用
 *
 * @RestController：= @Controller + @ResponseBody，方法返回值直接作为响应内容（而非页面）
 * @RequestMapping("/health")：类级别的路径映射，本类所有接口都以 /health 开头
 * 完整访问路径 = context-path(/api) + /health = /api/health
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * GET /api/health
     *
     * @return 固定返回 "ok"，看到它就说明 Web 服务正常
     */
    @GetMapping
    public String healthCheck() {
        return "ok";
    }
}
