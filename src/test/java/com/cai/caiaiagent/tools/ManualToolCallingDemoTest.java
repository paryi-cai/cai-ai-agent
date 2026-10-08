package com.cai.caiaiagent.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * 用户控制工具执行测试
 * 观察日志中的"【手动循环】第 N 轮"输出，即可看到我们自己控制的工具调用流程
 */
@SpringBootTest
class ManualToolCallingDemoTest {

    @Resource
    private ManualToolCallingDemo manualToolCallingDemo;

    @Test
    void testManualToolExecution() {
        String answer = manualToolCallingDemo.chatWithManualToolExecution(
                "请调用工具查询我自己（当前登录用户）的档案信息",
                Map.of("userName", "鱼皮", "loveStatus", "恋爱中"));
        System.out.println("最终回答：" + answer);
        Assertions.assertNotNull(answer);
        Assertions.assertTrue(answer.contains("鱼皮"), "回答应包含上下文中的用户信息，实际：" + answer);
    }
}
