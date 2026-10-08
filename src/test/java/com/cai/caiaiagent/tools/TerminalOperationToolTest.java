package com.cai.caiaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 终端操作工具测试（Windows 环境）
 */
class TerminalOperationToolTest {

    private final TerminalOperationTool tool = new TerminalOperationTool();

    @Test
    @DisplayName("执行 echo 命令应输出指定文本")
    void testExecuteTerminalCommand() {
        String result = tool.executeTerminalCommand("echo Hello Tool");
        System.out.println("命令输出：" + result);
        Assertions.assertTrue(result.contains("Hello Tool"), "应该输出 Hello Tool，实际：" + result);
    }
}
