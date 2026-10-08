package com.cai.caiaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具测试（纯本地、确定性验证）
 */
class TimeToolTest {

    private final TimeTool tool = new TimeTool();

    @Test
    @DisplayName("获取当前时间：格式正确且包含当前年份")
    void testGetCurrentDateTime() {
        String result = tool.getCurrentDateTime();
        System.out.println("当前时间：" + result);
        Assertions.assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"), "时间格式应为 yyyy-MM-dd HH:mm:ss");
        Assertions.assertTrue(result.startsWith(String.valueOf(LocalDate.now().getYear())), "应包含当前年份");
    }

    @Test
    @DisplayName("倒数：未来 10 天应返回 还有 10 天")
    void testDaysUntilFuture() {
        String target = LocalDate.now().plusDays(10).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String result = tool.daysUntil(target);
        System.out.println("未来日期(" + target + ")结果：" + result);
        Assertions.assertTrue(result.contains("还有 10 天"), "应返回还有 10 天，实际：" + result);
    }

    @Test
    @DisplayName("倒数：昨天应返回 已经过去 1 天")
    void testDaysUntilPast() {
        String target = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String result = tool.daysUntil(target);
        System.out.println("过去日期(" + target + ")结果：" + result);
        Assertions.assertTrue(result.contains("已经过去 1 天"), "应返回已经过去 1 天，实际：" + result);
    }
}
