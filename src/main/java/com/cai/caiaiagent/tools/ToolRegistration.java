package com.cai.caiaiagent.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具注册类：集中创建和管理所有工具，供 AI 一次性绑定使用
 *
 * 暗含的设计模式：
 * 1. 工厂模式：allTools() 作为工厂方法，集中创建并包装所有工具
 * 2. 注册模式：作为中央注册点，统一管理全部可用工具
 * 3. 适配器模式：ToolCallbacks.from 把各种工具类适配成统一的 ToolCallback 数组
 *
 * 好处：增删工具只需改这一处
 * （1.0 升级提示：ToolCallbacks 的包名从 org.springframework.ai.tool 变为 org.springframework.ai.support）
 */
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key:}")
    private String searchApiKey;

    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        UserProfileTool userProfileTool = new UserProfileTool();
        TimeTool timeTool = new TimeTool();
        return ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                userProfileTool,
                timeTool
        );
    }
}
