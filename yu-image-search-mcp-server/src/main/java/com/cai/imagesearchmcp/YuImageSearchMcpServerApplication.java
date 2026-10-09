package com.cai.imagesearchmcp;

import com.cai.imagesearchmcp.tools.ImageSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * 图片搜索 MCP 服务端启动类
 *
 * 原理：通过 ToolCallbackProvider Bean 把工具"暴露"出去，
 * MCP 服务端框架会自动把工具定义转换成 MCP 协议的 Tool 能力，供客户端发现和调用
 */
@SpringBootApplication
public class YuImageSearchMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuImageSearchMcpServerApplication.class, args);
    }

    /**
     * 注册图片搜索工具为 MCP 服务能力
     */
    @Bean
    public ToolCallbackProvider imageSearchTools(ImageSearchTool imageSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(imageSearchTool)
                .build();
    }
}
