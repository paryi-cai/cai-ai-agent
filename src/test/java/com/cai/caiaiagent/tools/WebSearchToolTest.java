package com.cai.caiaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 联网搜索工具测试
 * 前置条件：配置 search-api.api-key（SearchAPI 为付费服务，https://www.searchapi.io/）
 * 未配置时自动跳过（Assumptions）
 */
@SpringBootTest
class WebSearchToolTest {

    @Value("${search-api.api-key:}")
    private String searchApiKey;

    @Test
    @DisplayName("搜索关键词应返回网页结果")
    void testSearchWeb() {
        Assumptions.assumeTrue(searchApiKey != null && !searchApiKey.isBlank(),
                "未配置 search-api.api-key，跳过联网搜索测试");
        WebSearchTool tool = new WebSearchTool(searchApiKey);
        String result = tool.searchWeb("程序员鱼皮编程导航 codefather.cn");
        System.out.println("搜索返回长度：" + result.length());
        System.out.println("搜索结果预览：" + result.substring(0, Math.min(200, result.length())));
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.startsWith("Error"), "搜索不应报错：" + result);
    }
}
