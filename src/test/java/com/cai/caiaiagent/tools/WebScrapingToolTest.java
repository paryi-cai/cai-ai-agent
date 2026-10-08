package com.cai.caiaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 网页抓取工具测试（需要网络）
 */
class WebScrapingToolTest {

    private final WebScrapingTool tool = new WebScrapingTool();

    @Test
    @DisplayName("抓取 codefather.cn 首页应返回 HTML 内容")
    void testScrapeWebPage() {
        // 首选编程导航官网；若网络不通则用百度兜底（验证工具本身能力即可）
        String result = tool.scrapeWebPage("https://www.codefather.cn");
        if (result.startsWith("Error")) {
            result = tool.scrapeWebPage("https://www.baidu.com");
        }
        System.out.println("抓取内容长度：" + result.length());
        System.out.println("内容预览：" + result.substring(0, Math.min(200, result.length())).replace("\n", " "));
        Assertions.assertFalse(result.startsWith("Error"), "网页抓取不应报错：" + result);
        Assertions.assertTrue(result.length() > 500, "应该抓取到实际的网页内容");
    }
}
