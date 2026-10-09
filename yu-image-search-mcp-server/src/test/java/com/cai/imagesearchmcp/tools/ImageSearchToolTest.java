package com.cai.imagesearchmcp.tools;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 图片搜索工具测试
 * 未配置 SearchAPI Key 时自动跳过
 * （用 sse,local profile 启动环境：sse 避免 stdio 模式阻塞测试，local 加载本地密钥）
 */
@SpringBootTest
@ActiveProfiles({"sse", "local"})
class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Value("${image-search.api-key:}")
    private String apiKey;

    @Test
    void testSearchImage() {
        Assumptions.assumeTrue(StrUtil.isNotBlank(apiKey),
                "未配置 SearchAPI Key（IMAGE_SEARCH_API_KEY），跳过图片搜索测试");
        String result = imageSearchTool.searchImage("cute couple");
        System.out.println("搜索结果：" + result);
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.startsWith("Error"), "搜索不应报错：" + result);
    }
}
