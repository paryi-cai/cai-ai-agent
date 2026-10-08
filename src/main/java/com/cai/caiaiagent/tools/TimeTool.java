package com.cai.caiaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 时间工具（教程作业：自主实现的第 7 个工具）★
 *
 * 为什么 AI 需要它：大模型不知道"现在"是几点（训练数据有截止日期），
 * 而生成约会计划、计算纪念日倒计时等场景都依赖当前时间 —— 必须由工具提供。
 *
 * 设计说明：
 * 1. 固定使用北京时间（Asia/Shanghai），保证结果稳定可预期
 * 2. 参数名由编译器的 -parameters 保留（Spring Boot 默认开启），@ToolParam 只需写描述
 * 3. 返回值统一为 String，让 AI 理解更直接（教程建议）
 */
public class TimeTool {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    @Tool(description = "Get the current date and time (Beijing timezone, format yyyy-MM-dd HH:mm:ss)")
    public String getCurrentDateTime() {
        return LocalDateTime.now(BEIJING_ZONE)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool(description = "Calculate how many days are left until a target date (useful for anniversary/date countdown)")
    public String daysUntil(
            @ToolParam(description = "Target date in yyyy-MM-dd format, e.g. 2026-10-01") String targetDate) {
        try {
            LocalDate target = LocalDate.parse(targetDate);
            long days = ChronoUnit.DAYS.between(LocalDate.now(BEIJING_ZONE), target);
            if (days > 0) {
                return "距离 " + targetDate + " 还有 " + days + " 天";
            } else if (days == 0) {
                return targetDate + " 就是今天";
            } else {
                return targetDate + " 已经过去 " + (-days) + " 天";
            }
        } catch (Exception e) {
            return "Error parsing date (expected format yyyy-MM-dd): " + e.getMessage();
        }
    }
}
