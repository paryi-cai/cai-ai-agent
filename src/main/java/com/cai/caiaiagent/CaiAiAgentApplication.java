package com.cai.caiaiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动类
 *
 * @SpringBootApplication 是组合注解，包含三个核心能力：
 * 1. @SpringBootConfiguration：标识这是配置类
 * 2. @EnableAutoConfiguration：开启自动配置（根据依赖自动装配 Bean，如 Web、AI 模型等）
 * 3. @ComponentScan：扫描当前包及子包下的 @Component/@Service/@RestController 等并注册为 Bean
 */
@SpringBootApplication
public class CaiAiAgentApplication {

    public static void main(String[] args) {
        // 启动 Spring 容器 + 内嵌 Tomcat
        SpringApplication.run(CaiAiAgentApplication.class, args);
    }

}
